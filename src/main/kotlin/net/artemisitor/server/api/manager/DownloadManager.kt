package net.artemisitor.server.api.manager

import okhttp3.*
import java.io.File
import java.io.RandomAccessFile

object DownloadManager {
    fun getFinalDownloadUrl(url: String): String? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val redirectedUrl = response.request.url.toString()
                return redirectedUrl
            } else {
                println("Failed to fetch URL: ${response.code}")
                return null
            }
        }
    }
    fun downloadFile(url: String, destinationDir: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val contentLength = response.body?.contentLength() ?: -1L
            val fileName = url.substring(url.lastIndexOf("/") + 1)

            val destinationFile = File(destinationDir, fileName)
            if (!destinationFile.parentFile.exists()) {
                destinationFile.parentFile.mkdirs()  // 创建文件夹
            }

            val progress = Progress(contentLength)

            // 下载文件
            downloadFullFile(url, destinationFile, progress)

        } else {
            println("Failed to fetch file information: ${response.code}")
        }
    }

    // 下载整个文件并更新进度
    private fun downloadFullFile(
        url: String,
        destinationFile: File,
        progress: Progress
    ) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val tempFile = File(destinationFile.parent, destinationFile.name)
                RandomAccessFile(tempFile, "rw").use { raf ->
                    raf.seek(0)
                    response.body?.byteStream()?.use { inputStream ->
                        val buffer = ByteArray(8 * 1024)  // 每次读取8KB
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            raf.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            progress.updateProgress(bytesRead.toLong())
                            showProgress(progress.totalBytesDownloaded, progress.totalBytes, destinationFile.name)
                        }
                    }
                }
            } else {
                println("Failed to download file: ${response.code}")
            }
        }
    }

    // 显示进度条
    private fun showProgress(totalDownloaded: Long, totalSize: Long, fileName: String) {
        val progress = (totalDownloaded.toDouble() / totalSize * 100).toInt()
        val progressBar = "■".repeat(progress / 2)
        val progressSpaces = " ".repeat(50 - progressBar.length)
        print("\r $progress% [ $progressBar$progressSpaces ] $fileName")
    }

    class Progress(val totalBytes: Long) {
        @Volatile
        var totalBytesDownloaded: Long = 0
        fun updateProgress(bytesRead: Long) {
            totalBytesDownloaded += bytesRead
        }
    }
}




