package net.artemisitor.server

import net.artemisitor.server.api.manager.DownloadManager
import net.artemisitor.server.api.manager.URLManager
import okhttp3.OkHttpClient
import okhttp3.Request

class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val url = URLManager.getForgeUrl("918840","5550420")
            val destinationDir = "./download/"
            DownloadManager.getFinalDownloadUrl(url)?.let { DownloadManager.downloadFile(it, destinationDir) }
        }
    }
}