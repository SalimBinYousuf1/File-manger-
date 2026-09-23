package com.example.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun zipFiles(
        files: List<File>,
        destZipFile: File,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            val allFiles = mutableListOf<Pair<File, String>>()
            for (f in files) {
                if (f.isDirectory) {
                    collectFiles(f, f.name, allFiles)
                } else {
                    allFiles.add(f to f.name)
                }
            }

            var totalBytes = 0L
            for ((f, _) in allFiles) {
                totalBytes += f.length()
            }
            if (totalBytes == 0L) totalBytes = 1L

            var processedBytes = 0L

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZipFile))).use { zos ->
                val buffer = ByteArray(16384)
                for ((f, entryPath) in allFiles) {
                    val entry = ZipEntry(entryPath)
                    zos.putNextEntry(entry)
                    FileInputStream(f).use { fis ->
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            zos.write(buffer, 0, read)
                            processedBytes += read
                            onProgress(
                                (processedBytes.toFloat() / totalBytes).coerceIn(0f, 1f),
                                f.name
                            )
                        }
                    }
                    zos.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun collectFiles(dir: File, basePrefix: String, outList: MutableList<Pair<File, String>>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            val relative = "$basePrefix/${child.name}"
            if (child.isDirectory) {
                collectFiles(child, relative, outList)
            } else {
                outList.add(child to relative)
            }
        }
    }

    fun unzipFile(
        zipFile: File,
        targetDir: File,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Int {
        var extractedCount = 0
        val totalBytes = zipFile.length().coerceAtLeast(1L)
        var readBytes = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(16384)
            while (entry != null) {
                val outputFile = File(targetDir, entry.name)
                // Canonical path check to avoid Zip Slip vulnerability
                if (!outputFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip Slip path traversal attempted: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } != -1) {
                            fos.write(buffer, 0, len)
                            readBytes += len
                            onProgress(
                                (readBytes.toFloat() / totalBytes).coerceIn(0f, 1f),
                                entry.name
                            )
                        }
                    }
                    extractedCount++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extractedCount
    }
}
