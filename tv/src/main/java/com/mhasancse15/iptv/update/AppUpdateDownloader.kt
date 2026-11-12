package com.mhasancse15.iptv.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

sealed interface AppDownloadStatus {
    data class Running(val progress: Float?) : AppDownloadStatus
    data object Complete : AppDownloadStatus
    data class Failed(val reason: Int) : AppDownloadStatus
    data object Missing : AppDownloadStatus
}

class AppUpdateDownloader(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)

    fun enqueue(info: AppUpdateInfo): Long {
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("${info.appName} ${info.latestVersion}")
            .setDescription("Downloading Android TV update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                "bdix-tv-${info.latestVersionCode}.apk"
            )
        return manager.enqueue(request)
    }

    fun status(id: Long): AppDownloadStatus {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id))
        return cursor.use { result ->
            if (!result.moveToFirst()) return@use AppDownloadStatus.Missing
            when (result.getInt(result.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> AppDownloadStatus.Complete
                DownloadManager.STATUS_FAILED -> AppDownloadStatus.Failed(
                    result.getInt(result.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                )
                else -> {
                    val total = result.getLong(
                        result.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    val downloaded = result.getLong(
                        result.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    AppDownloadStatus.Running(
                        if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
                    )
                }
            }
        }
    }

    fun downloadedUri(id: Long): Uri? = manager.getUriForDownloadedFile(id)
}
