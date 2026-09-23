package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun SalimBreadcrumbBar(
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingPath by remember { mutableStateOf(false) }
    var pathInputText by remember(currentPath) { mutableStateOf(currentPath) }
    val scrollState = rememberScrollState()

    // Automatically scroll to end when path changes
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        if (isEditingPath) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = pathInputText,
                        onValueChange = { pathInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("breadcrumb_path_input"),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (File(pathInputText).exists()) {
                                onNavigateToPath(pathInputText)
                                isEditingPath = false
                            }
                        })
                    )
                }

                IconButton(
                    onClick = {
                        if (File(pathInputText).exists()) {
                            onNavigateToPath(pathInputText)
                            isEditingPath = false
                        }
                    },
                    modifier = Modifier.testTag("breadcrumb_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Go to path",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        pathInputText = currentPath
                        isEditingPath = false
                    },
                    modifier = Modifier.testTag("breadcrumb_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel direct path input",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val segments = buildPathSegments(currentPath)
                    segments.forEachIndexed { index, segment ->
                        val isLast = index == segments.size - 1

                        Surface(
                            onClick = { onNavigateToPath(segment.path) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLast) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent,
                            modifier = Modifier.testTag("breadcrumb_item_$index")
                        ) {
                            Text(
                                text = segment.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(9.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isEditingPath = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("breadcrumb_edit_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit path directly",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private data class PathSegment(val displayName: String, val path: String)

private fun buildPathSegments(path: String): List<PathSegment> {
    if (path.isEmpty() || path == "/") {
        return listOf(PathSegment("Root", "/"))
    }

    val segments = mutableListOf<PathSegment>()
    val parts = path.split("/").filter { it.isNotEmpty() }
    var accumulated = ""

    for (part in parts) {
        accumulated += "/$part"
        val display = when {
            accumulated == "/storage/emulated/0" || accumulated == "/sdcard" -> "Internal"
            else -> part
        }
        segments.add(PathSegment(display, accumulated))
    }

    return segments.ifEmpty { listOf(PathSegment("Storage", path)) }
}
