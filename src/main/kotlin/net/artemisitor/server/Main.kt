package net.artemisitor.server

import com.google.gson.Gson
import net.artemisitor.server.api.manager.DownloadManager
import net.artemisitor.server.api.manager.URLManager
import net.artemisitor.server.api.manager.ZipManager
import java.io.File
import java.nio.file.Paths


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
        private fun deleteFolderAndContents(folder: File) {
            if (folder.isDirectory) {
                val files = folder.listFiles()
                files?.forEach {
                    deleteFolderAndContents(it)
                }
            }
            if (folder.delete()) {
                println("Deleted: ${folder.absolutePath}")
            }
        }

        fun deleteNonEmptyFolder(folderPath: String) {
            val folder = File(folderPath)
            if (folder.exists()) {
                deleteFolderAndContents(folder)
            } else {
                println("Folder does not exist.")
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            var hasFixOption = false

            for (arg in args) {
                if (arg == "--fix") {
                    hasFixOption = true
                    break
                }
            }

            if (hasFixOption) {
                File("./PotatoCraft/PCL/version.json").delete()
            }


            File("./PotatoCraft/modpack.zip").delete()
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
                deleteFolderAndContents(File("PotatoCraft/.minecraft/versions/"))
                File("./PotatoCraft/PCL/version.json").delete()
                File("./PotatoCraft/modpack.zip").delete()
                DownloadManager.download(URLManager.PACK, File("./PotatoCraft/modpack.zip").absolutePath)
                DownloadManager.download(URLManager.getGithubUrl("version.json"),Paths.get(System.getProperty("user.dir"),"./PotatoCraft/PCL/","version.json").toFile().absolutePath)
            }
            Runtime.getRuntime().exec("./PotatoCraft/PCL.exe")
            File("PCL.zip").delete()

        }
    }
}
