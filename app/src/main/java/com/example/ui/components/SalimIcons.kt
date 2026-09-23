package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.FileCategory

@Composable
fun FileTypeIcon(
    category: FileCategory,
    isDirectory: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    customColor: Color? = null,
    fileName: String = ""
) {
    val description = if (isDirectory) "Folder: $fileName" else "${category.label} file: $fileName"
    val iconVector: ImageVector = when {
        isDirectory -> Icons.Default.Folder
        category == FileCategory.IMAGE -> Icons.Default.Image
        category == FileCategory.VIDEO -> Icons.Default.VideoFile
        category == FileCategory.AUDIO -> Icons.Default.AudioFile
        category == FileCategory.ARCHIVE -> Icons.Default.Archive
        category == FileCategory.APK -> Icons.Default.Android
        category == FileCategory.CODE -> Icons.Default.Code
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val iconColor = customColor ?: MaterialTheme.colorScheme.onSurface
    val bgColor = if (customColor != null) {
        customColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(bgColor, RoundedCornerShape(size * 0.28f))
            .semantics { this.contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
