package com.furflix.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

import android.annotation.SuppressLint

@SuppressLint("StaticFieldLeak")
class TagsDatabase(private val context: Context) {
    
    private val assetName = "tags_fur_v4.db"
    private val localDbName = "tags_fur_v5.db"
    private var db: SQLiteDatabase? = null

    fun prefetch() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getDatabase()
            } catch (e: Exception) {
                android.util.Log.e("TagsDatabase", "Error prefetching database", e)
            }
        }
    }

    suspend fun searchTags(query: String, limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        val database = getDatabase()
        val tags = mutableListOf<String>()
        val cursor = try {
            val safeQuery = query.replace("\"", "")
            database.rawQuery(
                """
                SELECT tags.name 
                FROM tags_fts 
                JOIN tags ON tags_fts.rowid = tags.id 
                WHERE tags_fts.name MATCH ? 
                ORDER BY 
                    CASE WHEN tags.name LIKE ? THEN 0 ELSE 1 END,
                    tags.post_count DESC 
                LIMIT ?
                """.trimIndent(), 
                arrayOf("\"$safeQuery*\"", "$safeQuery%", limit.toString())
            )
        } catch (e: Exception) {
            android.util.Log.e("TagsDatabase", "Error searching tags for '$query'", e)
            return@withContext emptyList()
        }
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    tags.add(it.getString(0))
                } while (it.moveToNext())
            }
        }
        tags
    }

    private fun getDatabase(): SQLiteDatabase {
        db?.let { if (it.isOpen) return it }
        val dbFile = context.getDatabasePath(localDbName)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            val tempFile = File(dbFile.absolutePath + ".tmp")
            context.assets.open(assetName).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.renameTo(dbFile)
        }
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return db!!
    }

    companion object {
        @Volatile
        private var INSTANCE: TagsDatabase? = null

        fun getInstance(context: Context): TagsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TagsDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
