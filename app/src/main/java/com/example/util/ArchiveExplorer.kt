package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class ArchiveEntryItem(
    val name: String,
    val path: String,
    val uncompressedSize: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val lastModified: Long
) {
    val compressionRatioPercent: Int
        get() = if (uncompressedSize > 0) {
            ((1.0 - (compressedSize.toDouble() / uncompressedSize.toDouble())) * 100.0).toInt().coerceIn(0, 99)
        } else 0
}

object ArchiveExplorer {

    suspend fun listEntries(zipFile: File): List<ArchiveEntryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ArchiveEntryItem>()
        if (!zipFile.exists() || zipFile.isDirectory) return@withContext list

        try {
            ZipFile(zipFile).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry: ZipEntry = entries.nextElement()
                    val path = entry.name.removeSuffix("/")
                    val displayName = path.substringAfterLast('/')

                    list.add(
                        ArchiveEntryItem(
                            name = displayName.ifEmpty { path },
                            path = entry.name,
                            uncompressedSize = if (entry.size < 0) 0L else entry.size,
                            compressedSize = if (entry.compressedSize < 0) 0L else entry.compressedSize,
                            isDirectory = entry.isDirectory,
                            lastModified = if (entry.time > 0) entry.time else zipFile.lastModified()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedWith(compareBy({ !it.isDirectory }, { it.path }))
    }

    suspend fun extractSingleEntry(
        zipFile: File,
        entryPath: String,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipFile(zipFile).use { zf ->
                val entry = zf.getEntry(entryPath)
                    ?: return@withContext Result.failure(IllegalArgumentException("Entry not found"))

                val fileName = entry.name.removeSuffix("/").substringAfterLast('/')
                val destFile = File(targetDir, fileName)

                if (entry.isDirectory) {
                    destFile.mkdirs()
                    return@withContext Result.success(destFile)
                }

                destFile.parentFile?.mkdirs()
                zf.getInputStream(entry).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(destFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
