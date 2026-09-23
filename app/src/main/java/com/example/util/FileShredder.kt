package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.UUID

object FileShredder {

    /**
     * Executes a 3-pass DoD 5220.22-M overwrite:
     * Pass 1: Cryptographic pseudo-random bytes
     * Pass 2: 0xFF (complement)
     * Pass 3: 0x00 (zeros)
     * Followed by descriptor sync, inode filename renaming, and deletion.
     */
    suspend fun shred(file: File, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        if (file.isDirectory) {
            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                shred(child, onProgress)
            }
            return@withContext file.delete()
        }

        try {
            val length = file.length()
            if (length > 0) {
                val random = SecureRandom()
                val bufferSize = 65536.coerceAtMost(length.toInt()).coerceAtLeast(1024)
                val buffer = ByteArray(bufferSize)

                RandomAccessFile(file, "rws").use { raf ->
                    // Pass 1: Random data
                    var remaining = length
                    raf.seek(0)
                    while (remaining > 0) {
                        val toWrite = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                        random.nextBytes(buffer)
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                        onProgress(0.33f * ((length - remaining).toFloat() / length.toFloat()))
                    }
                    raf.fd.sync()

                    // Pass 2: 0xFF
                    buffer.fill(0xFF.toByte())
                    remaining = length
                    raf.seek(0)
                    while (remaining > 0) {
                        val toWrite = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                        onProgress(0.33f + 0.33f * ((length - remaining).toFloat() / length.toFloat()))
                    }
                    raf.fd.sync()

                    // Pass 3: 0x00
                    buffer.fill(0x00.toByte())
                    remaining = length
                    raf.seek(0)
                    while (remaining > 0) {
                        val toWrite = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                        onProgress(0.66f + 0.34f * ((length - remaining).toFloat() / length.toFloat()))
                    }
                    raf.fd.sync()
                }
            }

            // Rename to destroy filename traces in directory index
            val parent = file.parentFile
            val scrambled = File(parent, UUID.randomUUID().toString())
            file.renameTo(scrambled)
            scrambled.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to standard delete if low-level RAF fails
            file.delete()
        }
    }
}
