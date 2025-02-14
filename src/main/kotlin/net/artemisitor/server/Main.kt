package net.artemisitor.server

import com.google.gson.Gson
import net.artemisitor.server.api.json.PackJson
import net.artemisitor.server.api.manager.DownloadManager
import net.artemisitor.server.api.manager.URLManager
import java.io.File
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            println("PotatoCraft Auto Updater")
            println("Starting up...")
            val mods = File("./mods/")
            if (mods.exists() && !mods.isDirectory) {

            }
        }
    }
}
