package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val lastAccessedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val trashFileName: String,
    val displayName: String,
    val size: Long,
    val deletedTimestamp: Long = System.currentTimeMillis(),
    val mimeType: String,
    val isDirectory: Boolean
)

@Entity(tableName = "folder_metadata")
data class FolderMetadataEntity(
    @PrimaryKey val path: String,
    val customLabel: String? = null,
    val colorHex: String? = null,
    val isStarred: Boolean = false,
    val isLocked: Boolean = false
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val originalPath: String,
    val encryptedFileName: String,
    val size: Long,
    val mimeType: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val ivHex: String
)
