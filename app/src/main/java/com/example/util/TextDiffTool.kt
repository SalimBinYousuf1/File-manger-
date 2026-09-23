package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class DiffType {
    SAME,
    ADDED,
    REMOVED
}

data class DiffLine(
    val type: DiffType,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val text: String
)

object TextDiffTool {

    suspend fun computeDiff(fileA: File, fileB: File): List<DiffLine> = withContext(Dispatchers.IO) {
        val linesA = if (fileA.exists()) fileA.readLines() else emptyList()
        val linesB = if (fileB.exists()) fileB.readLines() else emptyList()

        val lcs = computeLcs(linesA, linesB)
        val result = mutableListOf<DiffLine>()

        var i = 0
        var j = 0
        var oldLineNum = 1
        var newLineNum = 1

        while (i < linesA.size || j < linesB.size) {
            if (i < linesA.size && j < linesB.size && linesA[i] == linesB[j]) {
                result.add(
                    DiffLine(
                        type = DiffType.SAME,
                        oldLineNumber = oldLineNum++,
                        newLineNumber = newLineNum++,
                        text = linesA[i]
                    )
                )
                i++
                j++
            } else if (j < linesB.size && (i >= linesA.size || lcs[i][j + 1] >= lcs[i + 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffType.ADDED,
                        oldLineNumber = null,
                        newLineNumber = newLineNum++,
                        text = linesB[j]
                    )
                )
                j++
            } else if (i < linesA.size) {
                result.add(
                    DiffLine(
                        type = DiffType.REMOVED,
                        oldLineNumber = oldLineNum++,
                        newLineNumber = null,
                        text = linesA[i]
                    )
                )
                i++
            }
        }
        result
    }

    private fun computeLcs(a: List<String>, b: List<String>): Array<IntArray> {
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                if (a[i] == b[j]) {
                    dp[i][j] = dp[i + 1][j + 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }
        return dp
    }
}
