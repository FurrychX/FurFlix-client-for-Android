package com.furflix.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request


@Serializable
data class GitHubRelease(
    val tag_name: String,
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String // Direct URL for public repository downloads
)

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val assetUrl: String
)

class GitHubUpdateManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: GitHubUpdateManager? = null

        fun getInstance(context: Context): GitHubUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: GitHubUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        private const val REPO_OWNER = "FurrychX"
        private const val REPO_NAME = "FurFlix-client-for-Android"
        private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Checks for the latest release on GitHub.
     * Returns UpdateInfo if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("UpdateManager", "Failed to check for updates: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(responseBody)

            val latestVersion = release.tag_name.removePrefix("v")

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName?.removePrefix("v") ?: ""

            // Normalize versions (e.g. "1.3-beta" and "1.3beta" both become "13beta")
            val latestVersionNormalized = latestVersion.replace("[^0-9a-zA-Z]".toRegex(), "")
            val currentVersionNormalized = currentVersion.replace("[^0-9a-zA-Z]".toRegex(), "")

            if (latestVersionNormalized != currentVersionNormalized) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
                if (apkAsset != null) {
                    return@withContext UpdateInfo(
                        version = latestVersion,
                        changelog = release.body,
                        assetUrl = apkAsset.browser_download_url
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e("UpdateManager", "Exception checking for updates", e)
            null
        }
    }

    /**
     * Triggers DownloadManager to download the APK.
     * The BroadcastReceiver to handle the completion is defined in MainActivity or App.
     */
    fun downloadUpdate(updateInfo: UpdateInfo): Long {
        val request = DownloadManager.Request(Uri.parse(updateInfo.assetUrl))
            .setTitle("FurFlix Update ${updateInfo.version}")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "FurFlix_update_${updateInfo.version}.apk"
            )

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }
}
