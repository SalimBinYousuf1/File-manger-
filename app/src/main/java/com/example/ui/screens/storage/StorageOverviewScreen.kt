package com.example.ui.screens.storage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.ui.SalimMainViewModel
import com.example.ui.components.FileTypeIcon
import com.example.ui.components.SalimStorageBar
import com.example.ui.components.StorageTreemapView
import com.example.ui.components.TreemapNode
import com.example.ui.theme.TabularStyle

@Composable
fun StorageOverviewScreen(
    viewModel: SalimMainViewModel,
    onNavigateToCategory: (FileCategory) -> Unit,
    onNavigateToPath: (String) -> Unit,
    onOpenCleaner: () -> Unit,
    onOpenAppManager: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown by viewModel.storageBreakdown.collectAsState()
    val volumes by viewModel.volumes.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val recents by viewModel.recentFiles.collectAsState()
    val trashItems by viewModel.trashItems.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { viewModel.refreshStorageInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh storage")
                }
            }
        }

        // Live Proportional Storage Bar
        item {
            SalimStorageBar(breakdown = breakdown)
        }

        // Space Treemap
        item {
            val treemapNodes = remember(breakdown) {
                listOf(
                    TreemapNode("Images", "images", breakdown.imagesBytes, Color(0xFF0071E3)),
                    TreemapNode("Videos", "videos", breakdown.videosBytes, Color(0xFF34C759)),
                    TreemapNode("Docs", "docs", breakdown.documentsBytes, Color(0xFFFF9500)),
                    TreemapNode("Audio", "audio", breakdown.audioBytes, Color(0xFFFF2D55)),
                    TreemapNode("Archives", "archives", breakdown.archivesBytes, Color(0xFF5856D6)),
                    TreemapNode("Apps", "apps", breakdown.appsBytes, Color(0xFFAF52DE)),
                    TreemapNode("Other", "other", breakdown.otherBytes, Color(0xFF8E8E93))
                ).filter { it.size > 0 }
            }
            if (treemapNodes.isNotEmpty()) {
                StorageTreemapView(
                    nodes = treemapNodes,
                    onNodeClick = { node ->
                        when (node.name) {
                            "Images" -> onNavigateToCategory(FileCategory.IMAGE)
                            "Videos" -> onNavigateToCategory(FileCategory.VIDEO)
                            "Docs" -> onNavigateToCategory(FileCategory.DOCUMENT)
                            "Audio" -> onNavigateToCategory(FileCategory.AUDIO)
                            "Archives" -> onNavigateToCategory(FileCategory.ARCHIVE)
                            "Apps" -> onNavigateToCategory(FileCategory.APK)
                            else -> {}
                        }
                    }
                )
            }
        }

        // Quick Navigation Tiles (Cleaner, Apps, Trash, Vault)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    icon = Icons.Default.CleaningServices,
                    label = "Cleaner",
                    sub = "Duplicates, Big files",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onOpenCleaner,
                    modifier = Modifier.weight(1f).testTag("quick_tile_cleaner")
                )
                QuickTile(
                    icon = Icons.Default.Apps,
                    label = "Apps",
                    sub = "Manage, Extract APK",
                    color = Color(0xFF5856D6),
                    onClick = onOpenAppManager,
                    modifier = Modifier.weight(1f).testTag("quick_tile_apps")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val trashSizeBytes = trashItems.sumOf { it.size }
                QuickTile(
                    icon = Icons.Default.Delete,
                    label = "Trash Bin",
                    sub = "${trashItems.size} items (${FileItem.formatBytes(trashSizeBytes)})",
                    color = MaterialTheme.colorScheme.error,
                    onClick = onOpenTrash,
                    modifier = Modifier.weight(1f).testTag("quick_tile_trash")
                )
                QuickTile(
                    icon = Icons.Default.Lock,
                    label = "Vault",
                    sub = "AES-256 Encrypted",
                    color = Color(0xFFFF9500),
                    onClick = onOpenVault,
                    modifier = Modifier.weight(1f).testTag("quick_tile_vault")
                )
            }
        }

        // Volumes list (Internal, SD cards, USB OTG)
        item {
            Text(
                text = "Volumes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                volumes.forEach { vol ->
                    VolumeCard(
                        volume = vol,
                        onClick = { onNavigateToPath(vol.path) }
                    )
                }
            }
        }

        // Bookmarks (Pinned folders)
        if (bookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "Pinned Folders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bookmarks) { bm ->
                        Surface(
                            onClick = { onNavigateToPath(bm.path) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.width(130.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bm.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bm.path.substringAfterLast('/'),
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Files Access Log
        if (recents.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Accessed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recents.take(5).forEach { rec ->
                        Surface(
                            onClick = { onNavigateToPath(java.io.File(rec.path).parent ?: "/") },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FileTypeIcon(
                                    category = FileCategory.OTHER,
                                    isDirectory = false,
                                    fileName = rec.name,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FileItem.formatBytes(rec.size),
                                        style = TabularStyle.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    sub: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VolumeCard(
    volume: com.example.data.model.StorageVolumeInfo,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("volume_card_${volume.name}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (volume.isRemovable) Icons.Default.SdCard else Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${FileItem.formatBytes(volume.freeBytes)} free of ${FileItem.formatBytes(volume.totalBytes)}",
                    style = TabularStyle.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Text(
                text = "${(volume.usedPercent * 100).toInt()}%",
                style = TabularStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            )
        }
    }
}
