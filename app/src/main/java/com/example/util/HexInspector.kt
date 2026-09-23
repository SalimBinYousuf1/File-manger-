package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

data class HexRow(
    val offsetHex: String,
    val hexValues: String,
    val asciiString: String
)

object HexInspector {

    suspend fun readHexPage(
        file: File,
        startOffset: Long,
        length: Int = 1024
    ): List<HexRow> = withContext(Dispatchers.IO) {
        val rows = mutableListOf<HexRow>()
        if (!file.exists() || file.isDirectory) return@withContext rows

        val fileLen = file.length()
        if (startOffset >= fileLen) return@withContext rows

        val bytesToRead = length.coerceAtMost((fileLen - startOffset).toInt()).coerceAtLeast(0)
        val buffer = ByteArray(bytesToRead)

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startOffset)
            raf.readFully(buffer)
        }

        var offset = startOffset
        for (i in buffer.indices step 16) {
            val chunkLen = (buffer.size - i).coerceAtMost(16)
            val chunk = buffer.copyOfRange(i, i + chunkLen)

            val offsetStr = String.format("%08X", offset)

            val hexSb = StringBuilder()
            val asciiSb = StringBuilder()

            for (b in 0 until 16) {
                if (b < chunk.size) {
                    val byteVal = chunk[b].toInt() and 0xFF
                    hexSb.append(String.format("%02X ", byteVal))
                    // Printable ASCII range 32..126
                    if (byteVal in 32..126) {
                        asciiSb.append(byteVal.toChar())
                    } else {
                        asciiSb.append('.')
                    }
                } else {
                    hexSb.append("   ")
                }
            }

            rows.add(
                HexRow(
                    offsetHex = offsetStr,
                    hexValues = hexSb.toString().trimEnd(),
                    asciiString = asciiSb.toString()
                )
            )
            offset += 16
        }

        rows
    }
}
