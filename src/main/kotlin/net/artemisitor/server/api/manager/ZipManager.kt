package net.artemisitor.server.api.manager

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipManager {
    fun unzipFile(zipFilePath: String, destDir: String) {
        val zipFile = File(zipFilePath)
        val destDirectory = File(destDir)
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }
        val zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
        var zipEntry: ZipEntry? = zipInputStream.nextEntry
        while (zipEntry != null) {
            val newFile = File(destDirectory, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                FileOutputStream(newFile).use { fileOutputStream ->
                    BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            bufferedOutputStream.write(buffer, 0, len)
                        }
                    }
                }
            }
            zipInputStream.closeEntry()
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }
}