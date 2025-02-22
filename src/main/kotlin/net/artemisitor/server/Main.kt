package net.artemisitor.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.artemisitor.server.api.DownloadManager
import java.io.File
import java.security.MessageDigest

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("[更新猫] + PotatoCraft!")
            println("[更新猫] 开始计算文件差异!")
            if (args.contains("--hash")) {
                generateHashJson()
            } else {
                repairFilesFromCloud()
            }
            println("[更新猫] 更新完成喵!")
        }

        
        private fun generateHashJson() {
            val datasFolder = File("datas")
            val folderData = calculateAllFoldersHash(datasFolder)
            val json = JsonObject().apply {
                add("data", JsonArray().apply {
                    folderData.forEach { (path, files) ->
                        add(createFolderJson(path, files))
                    }
                })
            }
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(json)
            val jsonFile = File("./datas/update-datas.json")
            jsonFile.writeText(jsonString)

            println("[更新猫] 哈希值已计算并保存到 datas/update-datas.json!")
        }


        private fun repairFilesFromCloud() {
            val cloudUrl = "http://125.208.22.197:24562/update-datas.json"

            // 获取远程 JSON 文件内容
            val jsonContent = DownloadManager.fetchJsonFromUrl(cloudUrl)
            if (jsonContent == null) {
                println("[更新猫] 获取远程 JSON 文件失败，请检查网络连接或文件 URL。")
                return
            }

            // 解析 JSON 文件
            val gson = Gson()
            val json = try {
                gson.fromJson(jsonContent, JsonObject::class.java)
            } catch (e: Exception) {
                println("[更新猫] 解析 update-datas.json 失败: ${e.message}")
                return
            }

            val remoteFiles = mutableMapOf<String, String>()
            json.getAsJsonArray("data").forEach { element ->
                val path = element.asJsonObject.get("path").asString
                val files = element.asJsonObject.getAsJsonArray("files")
                files.forEach { fileElement ->
                    val fileInfo = fileElement.asJsonObject
                    val fileName = fileInfo.get("name").asString
                    val expectedHash = fileInfo.get("hash").asString
                    remoteFiles[fileName] = expectedHash // 存储文件的完整路径和哈希值
                }
            }

            // 遍历远程 JSON 中规定的 path 文件夹，检查文件是否存在于远程 JSON 中
            json.getAsJsonArray("data").forEach { element ->
                val path = element.asJsonObject.get("path").asString
                val folder = File(path)
                if (!folder.exists()) {
                    println("[更新猫] 文件夹不存在: ${folder.absolutePath}")
                    return@forEach
                }

                folder.walk().filter { it.isFile }.forEach { localFile ->
                    val relativePath = localFile.relativeTo(folder).path.replace("\\", "/") // 统一路径格式
                    val fullPath = "$path/$relativePath"
                    if (!remoteFiles.containsKey(fullPath)) {
                        println("[更新猫] 删除本地文件: ${fullPath}（不在远程 JSON 中）")
                        localFile.delete()
                    }
                }
            }

            // 修复或下载远程 JSON 中存在的文件
            json.getAsJsonArray("data").forEach { element ->
                val path = element.asJsonObject.get("path").asString
                val files = element.asJsonObject.getAsJsonArray("files")
                repairFolder(path, files)
            }

            println("[更新猫] 文件修复完成!")
        }

        
        private fun repairFolder(path: String, files: JsonArray) {
            val folder = File(path)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            files.forEach { fileElement ->
                val fileInfo = fileElement.asJsonObject
                val fileName = fileInfo.get("name").asString
                val expectedHash = fileInfo.get("hash").asString

                val file = File(fileName)
                if (file.exists()) {

                    val localHash = calculateFileHash(file)
                    if (localHash != expectedHash) {

                        file.delete()
                        println("\n[更新猫] 删除文件: ${file.absolutePath}")
                    }
                }


                if (!file.exists()) {
                    val cloudFileUrl = "http://125.208.22.197:24562/$fileName"
                    try {
                        DownloadManager.download(cloudFileUrl, file.absolutePath)
                    } catch (e: Exception) {
                        println("\n[更新猫] 下载文件失败: ${file.absolutePath}, 错误: ${e.message}")
                    }
                }
            }
        }

        
        private fun calculateAllFoldersHash(rootFolder: File): Map<String, List<Pair<String, String>>> {
            if (!rootFolder.exists() || !rootFolder.isDirectory) {
                println("[更新猫] 根文件夹不存在或不是目录: ${rootFolder.absolutePath}")
                return emptyMap()
            }

            val folderData = mutableMapOf<String, List<Pair<String, String>>>()
            rootFolder.walk()
                .maxDepth(Int.MAX_VALUE)
                .filter { it.isDirectory }
                .forEach { folder ->
                    val relativePath = folder.relativeTo(rootFolder).path
                    if (relativePath.isNotEmpty()) {
                        val filesWithHash = calculateFolderHash(folder, relativePath)
                        folderData[relativePath] = filesWithHash.second
                    }
                }

            return folderData
        }

        
        private fun calculateFolderHash(folder: File, path: String): Pair<String, List<Pair<String, String>>> {
            if (!folder.exists() || !folder.isDirectory) {
                println("[更新猫] 文件夹不存在或不是目录: ${folder.absolutePath}")
                return path to emptyList()
            }

            val filesWithHash = mutableListOf<Pair<String, String>>()
            folder.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val hash = calculateFileHash(file)
                    val relativePath = "${path}/${file.name}"
                    filesWithHash.add(relativePath to hash)
                } else if (file.isDirectory) {
                    val subFolderData = calculateFolderHash(file, "${path}/${file.name}")
                    filesWithHash.addAll(subFolderData.second)
                }
            }

            return path to filesWithHash
        }


        private fun calculateFileHash(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (true) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            return bytesToHex(hashBytes)
        }

        
        private fun bytesToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }

        
        private fun createFolderJson(path: String, files: List<Pair<String, String>>): JsonObject {
            return JsonObject().apply {
                addProperty("path", path)
                add("files", JsonArray().apply {
                    files.forEach { (relativePath, hash) ->
                        add(JsonObject().apply {
                            addProperty("name", relativePath.replace(" ","%20"))
                            addProperty("hash", hash)
                        })
                    }
                })
            }
        }
    }
}