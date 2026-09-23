package com.example.ui.screens.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TabularStyle
import com.example.util.DiffLine
import com.example.util.DiffType
import com.example.util.TextDiffTool
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDiffScreen(
    fileAPath: String,
    fileBPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileA = remember(fileAPath) { File(fileAPath) }
    val fileB = remember(fileBPath) { File(fileBPath) }
    var diffLines by remember { mutableStateOf<List<DiffLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(fileAPath, fileBPath) {
        diffLines = TextDiffTool.computeDiff(fileA, fileB)
        isLoading = false
    }

    val additions = remember(diffLines) { diffLines.count { it.type == DiffType.ADDED } }
    val deletions = remember(diffLines) { diffLines.count { it.type == DiffType.REMOVED } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${fileA.name} ↔ ${fileB.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                        Text(
                            text = "+$additions additions, -$deletions deletions",
                            style = TabularStyle.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else {
            val hScroll = rememberScrollState()
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .horizontalScroll(hScroll)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(diffLines) { line ->
                        val bg = when (line.type) {
                            DiffType.ADDED -> Color(0xFF34C759).copy(alpha = 0.12f)
                            DiffType.REMOVED -> Color(0xFFFF3B30).copy(alpha = 0.12f)
                            DiffType.SAME -> Color.Transparent
                        }

                        val indicator = when (line.type) {
                            DiffType.ADDED -> "+"
                            DiffType.REMOVED -> "-"
                            DiffType.SAME -> " "
                        }

                        val textColor = when (line.type) {
                            DiffType.ADDED -> Color(0xFF28A745)
                            DiffType.REMOVED -> Color(0xFFD73A49)
                            DiffType.SAME -> MaterialTheme.colorScheme.onSurface
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Line number
                            Text(
                                text = String.format("%3s", line.oldLineNumber?.toString() ?: ""),
                                style = TabularStyle.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%3s", line.newLineNumber?.toString() ?: ""),
                                style = TabularStyle.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = indicator,
                                style = TabularStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = line.text,
                                style = TabularStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
