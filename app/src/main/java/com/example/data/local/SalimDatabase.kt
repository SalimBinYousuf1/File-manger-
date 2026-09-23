package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SalimDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.FolderMetadataEntity
import com.example.data.local.entity.RecentFileEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.local.entity.VaultItemEntity

@Database(
    entities = [
        BookmarkEntity::class,
        RecentFileEntity::class,
        TrashItemEntity::class,
        FolderMetadataEntity::class,
        VaultItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SalimDatabase : RoomDatabase() {
    abstract fun salimDao(): SalimDao

    companion object {
        @Volatile
        private var INSTANCE: SalimDatabase? = null

        fun getDatabase(context: Context): SalimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalimDatabase::class.java,
                    "salim_filesystem.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
