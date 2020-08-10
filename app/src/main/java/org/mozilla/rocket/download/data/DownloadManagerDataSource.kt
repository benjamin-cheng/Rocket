package org.mozilla.rocket.download.data

import org.mozilla.rocket.tabs.web.Download

interface DownloadManagerDataSource {
    suspend fun enqueue(download: Download, refererUrl: String?): DownloadsRepository.DownloadState

    fun addCompletedDownload(
        title: String,
        description: String,
        isMediaScannerScannable: Boolean,
        mimeType: String,
        path: String,
        length: Long,
        showNotification: Boolean
    ): Long

    suspend fun getDownloadUrlHeaderInfo(url: String): DownloadsRepository.HeaderInfo

    suspend fun getDownload(downloadId: Long): DownloadInfo?

    suspend fun getDownloadingItems(runningIds: LongArray): List<DownloadInfo>

    suspend fun delete(downloadId: Long): Int
}