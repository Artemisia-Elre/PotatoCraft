package net.artemisitor.server

import com.google.gson.Gson
import net.artemisitor.server.api.json.PackJson
import net.artemisitor.server.api.manager.DownloadManager
import net.artemisitor.server.api.manager.URLManager
import java.io.File
import kotlinx.coroutines.*
import net.artemisitor.server.api.manager.ZipManager
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Semaphore

class Main {
    companion object {
        data class Version(val version: String)
        fun getVersion() : String?{
            try {
                val file = Paths.get(System.getProperty("user.dir"),"./PotatoCraft/PCL/","version.json").toFile()
                if (file.exists()) {
                    val gson = Gson()
                    val version = gson.fromJson(file.reader(), Version::class.java)
                    return version.version
                } else {
                    return null
                }
            } catch (_: Exception) { }
            return null
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println("PotatoCraft Auto Updater")
            println("Starting up...")
            val version = getVersion()
            val internet = Gson().fromJson(DownloadManager.fetchJsonFromUrl(URLManager.getGithubUrl("version.json"))?.reader(), Version::class.java)
            if (!File("PCL.zip").exists() && !File("PotatoCraft").exists()){
                println("Starting Download PCL")
                DownloadManager.download(URLManager.PCL, File("PCL.zip").absolutePath)
                ZipManager.unzipFile(File("PCL.zip").absolutePath,File("PotatoCraft").also {
                    if (!it.exists() && !it.isDirectory){
                        it.mkdirs()
                    }
                }.absolutePath)
            }
            if (version == null || internet.version != version) {
                DownloadManager.download(URLManager.PACK, File("./PotatoCraft/modpack.zip").absolutePath)
                DownloadManager.download(URLManager.getGithubUrl("version.json"),Paths.get(System.getProperty("user.dir"),"./PotatoCraft/PCL/","version.json").toFile().absolutePath)
            }
            Runtime.getRuntime().exec("./PotatoCraft/PCL.exe")
            File("PCL.zip").delete()
        }
    }
}
