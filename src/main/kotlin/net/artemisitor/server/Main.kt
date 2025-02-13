package net.artemisitor.server

import net.artemisitor.server.api.manager.DownloadManager
import net.artemisitor.server.api.manager.URLManager

class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val url = URLManager.getGithubUrl("/update/launcher/executable/PotatoCraft.zip").also {
                println(it)
            }
            val destinationDir = "./download/"
            DownloadManager.downloadFile(url, destinationDir)
            //DownloadManager.getFinalDownloadUrl(url)?.let { DownloadManager.downloadFile(it, destinationDir) }
        }
    }
}