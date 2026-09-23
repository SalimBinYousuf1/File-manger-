package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.theme.TabularStyle

data class TreemapNode(
    val name: String,
    val path: String,
    val size: Long,
    val color: Color
)

@Composable
fun StorageTreemapView(
    nodes: List<TreemapNode>,
    onNodeClick: (TreemapNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) return

    val totalSize = remember(nodes) { nodes.sumOf { it.size }.coerceAtLeast(1L) }
    val sortedNodes = remember(nodes) { nodes.sortedByDescending { it.size }.take(8) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .animateContentSize()
    ) {
        Text(
            text = "Storage Space Treemap",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val totalW = maxWidth
            val totalH = maxHeight

            // Split into two major columns: 1st node or group on left, rest on right
            val topNode = sortedNodes.firstOrNull()
            val remainingNodes = sortedNodes.drop(1)

            if (topNode != null && remainingNodes.isNotEmpty()) {
                val leftRatio = (topNode.size.toFloat() / totalSize.toFloat()).coerceIn(0.35f, 0.65f)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left primary block
                    TreemapBlock(
                        node = topNode,
                        totalSize = totalSize,
                        onClick = { onNodeClick(topNode) },
                        modifier = Modifier
                            .weight(leftRatio)
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                    )

                    // Right secondary blocks
                    Column(
                        modifier = Modifier
                            .weight(1f - leftRatio)
                            .fillMaxHeight()
                            .padding(start = 4.dp)
                    ) {
                        val subFirst = remainingNodes.firstOrNull()
                        val subRest = remainingNodes.drop(1)

                        if (subFirst != null) {
                            TreemapBlock(
                                node = subFirst,
                                totalSize = totalSize,
                                onClick = { onNodeClick(subFirst) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = if (subRest.isNotEmpty()) 4.dp else 0.dp)
                            )
                        }

                        if (subRest.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                subRest.take(2).forEachIndexed { idx, sub ->
                                    TreemapBlock(
                                        node = sub,
                                        totalSize = totalSize,
                                        onClick = { onNodeClick(sub) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(end = if (idx == 0 && subRest.size > 1) 4.dp else 0.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (topNode != null) {
                TreemapBlock(
                    node = topNode,
                    totalSize = totalSize,
                    onClick = { onNodeClick(topNode) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TreemapBlock(
    node: TreemapNode,
    totalSize: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = ((node.size.toDouble() / totalSize.toDouble()) * 100.0).toInt()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(node.color.copy(alpha = 0.2f))
            .border(1.dp, node.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = FileItem.formatBytes(node.size),
                style = TabularStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "$percent%",
                style = TabularStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            )
        }
    }
}
