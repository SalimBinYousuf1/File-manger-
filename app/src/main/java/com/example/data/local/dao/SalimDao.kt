package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.FolderMetadataEntity
import com.example.data.local.entity.RecentFileEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.local.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalimDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC, addedTimestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE path = :path")
    suspend fun deleteBookmarkByPath(path: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE path = :path)")
    fun isBookmarked(path: String): Flow<Boolean>

    // Recent files
    @Query("SELECT * FROM recent_files ORDER BY lastAccessedTimestamp DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 50): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordFileAccess(recent: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentFile(path: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearRecents()

    // Trash Items
    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    suspend fun getTrashItemsSync(): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getTrashItemById(id: Long): TrashItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItemEntity): Long

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItemById(id: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearAllTrashItems()

    @Query("SELECT SUM(size) FROM trash_items")
    fun getTrashTotalSizeBytes(): Flow<Long?>

    // Folder Metadata (Custom colors, labels, starred)
    @Query("SELECT * FROM folder_metadata")
    fun getAllFolderMetadata(): Flow<List<FolderMetadataEntity>>

    @Query("SELECT * FROM folder_metadata WHERE path = :path LIMIT 1")
    fun getFolderMetadata(path: String): Flow<FolderMetadataEntity?>

    @Query("SELECT * FROM folder_metadata WHERE path = :path LIMIT 1")
    suspend fun getFolderMetadataSync(path: String): FolderMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFolderMetadata(metadata: FolderMetadataEntity)

    @Query("DELETE FROM folder_metadata WHERE path = :path")
    suspend fun deleteFolderMetadata(path: String)

    @Query("SELECT path FROM folder_metadata WHERE isStarred = 1")
    fun getStarredPaths(): Flow<List<String>>

    // Encrypted Vault
    @Query("SELECT * FROM vault_items ORDER BY addedTimestamp DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity): Long

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Long): VaultItemEntity?

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)
}
