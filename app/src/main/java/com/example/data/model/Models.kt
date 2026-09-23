package com.example.data.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory(val label: String) {
    FOLDER("Folders"),
    IMAGE("Images"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    DOCUMENT("Documents"),
    ARCHIVE("Archives"),
    APK("Apps & APKs"),
    CODE("Code & Scripts"),
    OTHER("Other")
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri? = null,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val itemCount: Int = 0,
    val mimeType: String = "",
    val extension: String = "",
    val isHidden: Boolean = false,
    val isSymlink: Boolean = false,
    val isStarred: Boolean = false,
    val folderColorHex: String? = null,
    val customLabel: String? = null,
    val category: FileCategory = FileCategory.OTHER
) {
    val formattedSize: String
        get() = formatBytes(size)

    val formattedDate: String
        get() {
            if (lastModified <= 0L) return "—"
            val sdf = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val compactDate: String
        get() {
            if (lastModified <= 0L) return "—"
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
                .coerceIn(0, units.size - 1)
            return if (digitGroups == 0) {
                "$bytes B"
            } else {
                String.format(
                    Locale.US,
                    "%.1f %s",
                    bytes.toDouble() / Math.pow(1024.0, digitGroups.toDouble()),
                    units[digitGroups]
                )
            }
        }
    }
}

data class StorageVolumeInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isRemovable: Boolean = false,
    val isPrimary: Boolean = true
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class StorageCategoryBreakdown(
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val appsBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val trashBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
)

enum class SortField(val label: String) {
    NAME("Name"),
    DATE_MODIFIED("Date Modified"),
    SIZE("Size"),
    TYPE("Type"),
    EXTENSION("Extension")
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class ViewMode {
    LIST,
    GRID
}

data class OperationProgress(
    val title: String = "",
    val currentItemName: String = "",
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Float = 0f,
    val isRunning: Boolean = false,
    val error: String? = null
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val installTime: Long,
    val apkPath: String
)

data class DuplicateFileGroup(
    val hash: String,
    val size: Long,
    val files: List<FileItem>
)
