package org.mozilla.rocket.download.data

import android.content.Context
import android.net.TrafficStats
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Func
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.focus.network.SocketTags
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FetchDownloadManagerDataSource(private val appContext: Context) : DownloadManagerDataSource {

    private val downloadManager: Fetch by lazy {
        val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(appContext)
            .setDownloadConcurrentLimit(3)
            .setNotificationManager(object : DefaultFetchNotificationManager(appContext) {
                override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                    return downloadManager
                }
            })
            .build()
        Fetch.Impl.getInstance(fetchConfiguration).also {
            it.addListener(fetchListener)
        }
    }

    override suspend fun enqueue(download: org.mozilla.rocket.tabs.web.Download, refererUrl: String?): DownloadsRepository.DownloadState = withContext(Dispatchers.IO) {
        suspendCoroutine<DownloadsRepository.DownloadState> { continuation ->
            val cookie = CookieManager.getInstance().getCookie(download.url)
            val fileName = "test100Mb.db" /*download.name
                ?: URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType)*/

            // so far each download always return null even for an image.
            // But we might move downloaded file to another directory.
            // So, for now we always save file to DIRECTORY_DOWNLOADS
//            val dir = Environment.DIRECTORY_DOWNLOADS

            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
                continuation.resume(DownloadsRepository.DownloadState.StorageUnavailable)
            }

            // block non-http/https download links
            if (!URLUtil.isNetworkUrl(download.url)) {
                continuation.resume(DownloadsRepository.DownloadState.FileNotSupported)
            }

            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/fetch/DownloadList/" + fileName
            val request = Request("http://speedtest.ftp.otenet.gr/files/test100Mb.db", downloadDir).also {
                it.priority = Priority.HIGH
                it.networkType = NetworkType.ALL
                it.addHeader("User-Agent", download.userAgent)
                it.addHeader("Cookie", cookie)
            }
            downloadManager.enqueue(request,
                Func<Request> { result ->
                    continuation.resume(DownloadsRepository.DownloadState.Success(result.id.toLong(), download.isStartFromContextMenu))
                },
                Func<Error> {
                    continuation.resume(DownloadsRepository.DownloadState.GeneralError)
                }
            )
        }
    }

    override fun addCompletedDownload(
        title: String,
        description: String,
        isMediaScannerScannable: Boolean,
        mimeType: String,
        path: String,
        length: Long,
        showNotification: Boolean
    ) = -1L

    override suspend fun getDownloadUrlHeaderInfo(url: String): DownloadsRepository.HeaderInfo = withContext(Dispatchers.IO) {
        TrafficStats.setThreadStatsTag(SocketTags.DOWNLOADS)
        var connection: HttpURLConnection? = null
        var isSupportRange = false
        var isValidSSL = true
        var contentLength = 0L
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            val headerField = connection.getHeaderField("Accept-Ranges")
            isSupportRange = (headerField != null && headerField == "bytes")
            val strContentLength = connection.getHeaderField("Content-Length")
            contentLength = strContentLength?.toLong() ?: 0L
            connection.responseCode
            connection.disconnect()
        } catch (e: SSLHandshakeException) {
            isValidSSL = false
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return@withContext DownloadsRepository.HeaderInfo(isSupportRange, isValidSSL, contentLength)
    }

    override suspend fun getDownload(downloadId: Long): DownloadInfo? = withContext(Dispatchers.IO) {
        return@withContext null
//        val query = DownloadManager.Query()
//        query.setFilterById(downloadId)
//        downloadManager.query(query)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val downloadInfo = DownloadInfo()
//                downloadInfo.downloadId = downloadId
//                downloadInfo.description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION))
//                downloadInfo.setStatusInt(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
//                downloadInfo.reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
//                downloadInfo.setSize(cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toDouble())
//                downloadInfo.sizeTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toDouble()
//                downloadInfo.sizeSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).toDouble()
//                downloadInfo.setDate(cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)))
//                downloadInfo.mediaUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI))
//                downloadInfo.fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
//                if (downloadInfo.fileUri != null) {
//                    val extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(downloadInfo.fileUri, "UTF-8"))
//                    downloadInfo.mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT))
//                    downloadInfo.fileExtension = extension
//                    downloadInfo.fileName = File(Uri.parse(downloadInfo.fileUri).path).name
//                }
//                return@withContext downloadInfo
//            }
//        }
//        return@withContext null
    }

    override suspend fun getDownloadingItems(runningIds: LongArray): List<DownloadInfo> = withContext(Dispatchers.IO) {
//        val query = DownloadManager.Query()
//        query.setFilterById(*runningIds)
//        query.setFilterByStatus(DownloadManager.STATUS_RUNNING)
//        downloadManager.query(query)?.use { cursor ->
//            val list = ArrayList<DownloadInfo>()
//            while (cursor.moveToNext()) {
//                val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
//                val totalSize = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
//                val currentSize = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
//                val info = DownloadInfo()
//                info.downloadId = id
//                info.sizeTotal = totalSize
//                info.sizeSoFar = currentSize
//                list.add(info)
//            }
//            return@withContext list
//        }
        return@withContext emptyList<DownloadInfo>()
    }

    override suspend fun delete(downloadId: Long) = withContext(Dispatchers.IO) {
        downloadManager.remove(downloadId.toInt())
        return@withContext 0
    }

    private val fetchListener: FetchListener = object : AbstractFetchListener() {
        override fun onAdded(download: Download) {
            Log.d("FetchDownload", "onAdded -> $download")
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            Log.d("FetchDownload", "onQueued -> $download, waitingOnNetwork: $waitingOnNetwork")
        }

        override fun onCompleted(download: Download) {
            Log.d("FetchDownload", "onCompleted -> $download")
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            Log.d("FetchDownload", "onError -> $download, error: $error")
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            Log.d("FetchDownload", "onProgress -> $download, etaInMilliSeconds: $etaInMilliSeconds, downloadedBytesPerSecond: $downloadedBytesPerSecond")
        }

        override fun onPaused(download: Download) {
            Log.d("FetchDownload", "onPaused -> $download")
        }

        override fun onResumed(download: Download) {
            Log.d("FetchDownload", "onResumed -> $download")
        }

        override fun onCancelled(download: Download) {
            Log.d("FetchDownload", "onCancelled -> $download")
        }

        override fun onRemoved(download: Download) {
            Log.d("FetchDownload", "onRemoved -> $download")
        }

        override fun onDeleted(download: Download) {
            Log.d("FetchDownload", "onDeleted -> $download")
        }
    }

}