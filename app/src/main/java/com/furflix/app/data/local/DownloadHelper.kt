package com.furflix.app.data.local

import org.json.JSONObject
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

data class DownloadedItem(val uri: Uri, val filename: String)

object DownloadHelper {
    private val client = OkHttpClient()

    private fun getSanitizedFilename(title: String, artist: String, id: String, extension: String): String {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val safeArtist = artist.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)
        return "FA_${safeArtist}_${safeTitle}_$id.$extension"
    }

    suspend fun downloadImage(
        context: Context,
        url: String,
        title: String,
        artist: String,
        submissionId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val extension = url.substringAfterLast('.', "jpg").substringBefore('?')
            val filename = getSanitizedFilename(title, artist, submissionId, extension)
            
            // Check if it already exists in public Pictures/FurFlix directory
            val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "FurFlix"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(filename, "$relativeLocation/")
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    cursor.close()
                    return@withContext Result.failure(Exception("ALREADY_EXISTS"))
                }
                cursor?.close()
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FurFlix")
                val file = File(dir, filename)
                if (file.exists()) {
                    return@withContext Result.failure(Exception("ALREADY_EXISTS"))
                }
            }

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download image: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FurFlix")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                
                FileOutputStream(file).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            
            val prefs = context.getSharedPreferences("downloads_meta", Context.MODE_PRIVATE)
            val metadata = JSONObject().apply {
                put("author", artist)
                put("title", title)
                put("postId", submissionId)
            }.toString()
            prefs.edit().putString(submissionId, metadata).apply()

            Result.success("Downloaded successfully")
        } catch (e: Exception) {
            Log.e("DownloadHelper", "Error downloading image", e)
            Result.failure(e)
        }
    }

    suspend fun getDownloadedImages(context: Context): List<DownloadedItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<DownloadedItem>()
        try {
            val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "FurFlix"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("%$relativeLocation%")
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: ""
                        val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        items.add(DownloadedItem(contentUri, name))
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FurFlix")
                if (dir.exists()) {
                    dir.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { file ->
                        items.add(DownloadedItem(Uri.fromFile(file), file.name))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadHelper", "Error getting downloaded images", e)
        }
        items
    }
    
    fun deleteImage(context: Context, uri: Uri): Boolean {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                val file = uri.path?.let { File(it) }
                file?.delete() ?: false
            }
        } catch (e: Exception) {
            Log.e("DownloadHelper", "Failed to delete image", e)
            false
        }
    }
}
