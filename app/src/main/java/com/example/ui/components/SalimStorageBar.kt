package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.data.model.StorageCategoryBreakdown
import com.example.ui.theme.TabularStyle

@Composable
fun SalimStorageBar(
    breakdown: StorageCategoryBreakdown,
    modifier: Modifier = Modifier
) {
    val total = breakdown.totalBytes.coerceAtLeast(1L)
    val usedPercent = (breakdown.usedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${FileItem.formatBytes(breakdown.usedBytes)} of ${FileItem.formatBytes(breakdown.totalBytes)} used",
                        style = TabularStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    )
                }

                Text(
                    text = "${(usedPercent * 100).toInt()}%",
                    style = TabularStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proportional Multi-Segment Progress Bar
            val imgWeight = (breakdown.imagesBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val vidWeight = (breakdown.videosBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val audWeight = (breakdown.audioBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val docWeight = (breakdown.documentsBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val appWeight = (breakdown.appsBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val othWeight = (breakdown.otherBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val freeWeight = (breakdown.freeBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imgWeight > 0.005f) {
                    Box(modifier = Modifier.weight(imgWeight).fillMaxHeight().background(Color(0xFF34C759)))
                }
                if (vidWeight > 0.005f) {
                    Box(modifier = Modifier.weight(vidWeight).fillMaxHeight().background(Color(0xFFFF9500)))
                }
                if (audWeight > 0.005f) {
                    Box(modifier = Modifier.weight(audWeight).fillMaxHeight().background(Color(0xFFAF52DE)))
                }
                if (docWeight > 0.005f) {
                    Box(modifier = Modifier.weight(docWeight).fillMaxHeight().background(Color(0xFF0071E3)))
                }
                if (appWeight > 0.005f) {
                    Box(modifier = Modifier.weight(appWeight).fillMaxHeight().background(Color(0xFF5856D6)))
                }
                if (othWeight > 0.005f) {
                    Box(modifier = Modifier.weight(othWeight).fillMaxHeight().background(Color(0xFF8E8E93)))
                }
                if (freeWeight > 0.005f) {
                    Box(modifier = Modifier.weight(freeWeight).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryLegendItem(label = "Images", bytes = breakdown.imagesBytes, color = Color(0xFF34C759))
                CategoryLegendItem(label = "Videos", bytes = breakdown.videosBytes, color = Color(0xFFFF9500))
                CategoryLegendItem(label = "Docs", bytes = breakdown.documentsBytes, color = Color(0xFF0071E3))
                CategoryLegendItem(label = "Free", bytes = breakdown.freeBytes, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun CategoryLegendItem(
    label: String,
    bytes: Long,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = FileItem.formatBytes(bytes),
                style = TabularStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
