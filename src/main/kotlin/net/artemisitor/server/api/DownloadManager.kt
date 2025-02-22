package net.artemisitor.server.api

import okhttp3.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("PLUGIN_IS_NOT_ENABLED")
object DownloadManager {

    fun getFileName(url: String): String {
        val parts = url.split("/")
        return parts.last()
    }

    fun fetchJsonFromUrl(url: String): String? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response: Response = client.newCall(request).execute()
        if (response.isSuccessful) {
            return response.body?.string()
        } else {
            println("Failed: ${response.message}")
            return null
        }
    }

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

    @Serializable
    data class DownloadService(
        val url: String,
        val contentLength: Long,
        val targetPath: String,
        private val blockCount: Int,
        private val bufferSize: Long,
    ) {
        private val file: RandomAccessFile by lazy {
            RandomAccessFile(targetPath, "rwd")
        }
        private val muteX by lazy {
            Mutex()
        }
        private val blockSize by lazy {
            ceil(contentLength.toFloat() / blockCount).roundToLong()
        }
        private val unCompleteTaskList = mutableListOf<DownloadTask>()
        val init
            get() = client != null

        @Transient
        private var client: HttpClient? = null

        @Transient
        private var job: Job? = null
        val progress: Progress by lazy {
            Progress(contentLength)
        }

        val downloadSize: Long
            get() = unCompleteTaskList.sumOf { it.downloadSize }

        val progressValue: Float
            get() = downloadSize.toFloat() / contentLength

        val isDownloading
            get() = job != null

        fun init(client: HttpClient) {
            check(!init) { "service already init" }
            this.client = client
            if (unCompleteTaskList.isEmpty()) {
                unCompleteTaskList.addAll(
                    (0..<contentLength step blockSize + 1).map {
                        val start = it
                        val end = min(it + blockSize, contentLength)
                        DownloadTask(start, end, 0)
                    }
                )
            }
        }

        fun start(scope: CoroutineScope, block: HttpRequestBuilder.() -> Unit = {}) {
            check(init) { "service not init" }
            check(job == null) { "download still in progress" }

            job = scope.launch {
                unCompleteTaskList.filter { it.downloadSize <= blockSize }.map { taskConfig ->
                    launch {
                        val statement = client!!.prepareGet(url) {
                            headers {
                                append("Range", "bytes=${taskConfig.start + taskConfig.downloadSize}-${taskConfig.end}")
                            }
                        }
                        statement.execute {
                            val channel = it.bodyAsChannel()
                            while (true) {
                                val bytes = channel.readRemaining(bufferSize).readBytes()
                                if (bytes.isEmpty()) break

                                muteX.withLock {
                                    withContext(Dispatchers.IO) {
                                        file.seek(taskConfig.start + taskConfig.downloadSize)
                                        file.write(bytes, 0, bytes.size)
                                    }
                                    taskConfig.downloadSize += bytes.size
                                    progress.updateProgress(bytes.size.toLong())
                                }
                            }
                        }
                    }
                }.joinAll()
                job = null
            }
        }

        suspend fun await() {
            job?.join() ?: throw IllegalStateException("任务未开始")
        }

        fun pause() {
            check(init) { "service not init" }
            check(job != null) { "download not started" }
            job!!.cancel()
            job = null
        }

        companion object {
            suspend fun create(
                block: DownloadServiceConfig.() -> Unit,
            ): DownloadService {
                val config = DownloadServiceConfig().apply(block)

                val client = HttpClient(CIO) {
                    install(HttpTimeout)
                }
                val url = config.url
                val response = client.head(url) {
                    headers.append("Accept-Ranges", "acceptable-ranges")
                }.call.response.headers

                check(response["Accept-Ranges"] == "bytes") {
                    "$url not support multi-thread download"
                }
                val contentLength = response["Content-Length"]?.toLongOrNull()
                check(contentLength != null && contentLength > 0) {
                    "Content Length is Null"
                }
                return DownloadService(
                    url = url,
                    contentLength = contentLength,
                    targetPath = config.targetPath.absolutePath,
                    blockCount = config.blockCount,
                    bufferSize = config.bufferedSize
                )
            }
        }
    }

    class DownloadServiceConfig {
        lateinit var url: String
        lateinit var targetPath: File
        var blockCount: Int = 16
        var bufferedSize: Long = 1024
    }

    @Serializable
    data class DownloadTask(
        var start: Long,
        var end: Long,
        var downloadSize: Long = 0,
    )

    class Progress(private val totalBytes: Long) {
        @Volatile
        var totalBytesDownloaded: Long = 0

        fun updateProgress(bytesRead: Long) {
            totalBytesDownloaded += bytesRead
        }

        private fun getProgress(): Int {
            return if (totalBytes > 0) {
                (totalBytesDownloaded.toFloat() / totalBytes * 100).toInt()
            } else {
                0
            }
        }

        fun getProgressBar(): String {
            val progress = getProgress()
            val progressBarLength = 40
            val filledLength = (progress * progressBarLength) / 100
            val emptyLength = progressBarLength - filledLength
            return "[${"■".repeat(filledLength)}${" ".repeat(emptyLength)}] $progress%"
        }
    }


    fun download(url: String, outputFilePath: String) = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30.minutes.inWholeMilliseconds
                connectTimeoutMillis = 30.minutes.inWholeMilliseconds
                socketTimeoutMillis = 30.minutes.inWholeMilliseconds
            }
        }

        val progressFile = File(File(outputFilePath).parentFile,"${getFileName(url)}.json")
        val service = if (progressFile.exists()) {
            try {
                val savedService = Json.decodeFromString<DownloadService>(progressFile.readText())
                savedService.init(client)
                savedService
            } catch (e: Exception) {
                println("Failed to restore progress, starting new download: ${e.message}")
                progressFile.delete()
                DownloadService.create {
                    this.url = url
                    targetPath = File(outputFilePath).apply {
                        parentFile?.mkdirs()
                        if (exists()) delete()
                        createNewFile()
                    }
                }
            }
        } else {
            DownloadService.create {
                this.url = url
                targetPath = File(outputFilePath).apply {
                    parentFile?.mkdirs()
                    if (exists()) delete()
                    createNewFile()
                }
            }
        }
        if (!service.init) {
            service.init(client)
        }
        service.start(this)
        launch {
            while (service.isDownloading) {
                delay(1.seconds)
                print("\r${service.progress.getProgressBar()} ${getFileName(url)}")
            }
        }
        runCatching {
            service.await()
        }.onFailure {
            println("Download paused or encountered an error.")
        }.onSuccess {
            progressFile.delete()
        }
        if (service.isDownloading) {
            println("Download interrupted. Saving progress...")
            progressFile.writeText(Json.encodeToString(service))
        }
    }


}