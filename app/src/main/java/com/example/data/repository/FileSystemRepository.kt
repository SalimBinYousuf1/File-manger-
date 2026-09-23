package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.local.dao.SalimDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.FolderMetadataEntity
import com.example.data.local.entity.RecentFileEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.local.entity.VaultItemEntity
import com.example.data.model.DuplicateFileGroup
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.InstalledAppItem
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.StorageCategoryBreakdown
import com.example.data.model.StorageVolumeInfo
import com.example.util.CryptoUtils
import com.example.util.MimeUtils
import com.example.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID

class FileSystemRepository(
    private val context: Context,
    private val dao: SalimDao
) {
    private val trashDir: File by lazy {
        File(context.filesDir, "trash").apply { if (!exists()) mkdirs() }
    }

    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
    }

    // 1. Storage & Volume Statistics (StatFs + MediaStore)
    suspend fun getVolumes(): List<StorageVolumeInfo> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageVolumeInfo>()
        val primaryDir = Environment.getExternalStorageDirectory()
        if (primaryDir.exists()) {
            try {
                val stat = StatFs(primaryDir.path)
                val blockSize = stat.blockSizeLong
                val totalBytes = stat.blockCountLong * blockSize
                val freeBytes = stat.availableBlocksLong * blockSize
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                volumes.add(
                    StorageVolumeInfo(
                        name = "Internal Storage",
                        path = primaryDir.absolutePath,
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        usedBytes = usedBytes,
                        isRemovable = false,
                        isPrimary = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Secondary / Removable storage (SD Card, USB OTG)
        val extDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in extDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                try {
                    val root = getVolumeRoot(dir)
                    val stat = StatFs(root.path)
                    val blockSize = stat.blockSizeLong
                    val total = stat.blockCountLong * blockSize
                    val free = stat.availableBlocksLong * blockSize
                    volumes.add(
                        StorageVolumeInfo(
                            name = root.name.ifEmpty { "SD Card" },
                            path = root.absolutePath,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = (total - free).coerceAtLeast(0L),
                            isRemovable = true,
                            isPrimary = false
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        volumes
    }

    private fun getVolumeRoot(file: File): File {
        var current = file
        while (current.parentFile != null && current.parentFile?.path != "/storage") {
            current = current.parentFile ?: break
        }
        return current
    }

    suspend fun getStorageCategoryBreakdown(): StorageCategoryBreakdown = withContext(Dispatchers.IO) {
        var total = 0L
        var free = 0L
        try {
            val primaryDir = Environment.getExternalStorageDirectory()
            val stat = StatFs(primaryDir.path)
            total = stat.blockCountLong * stat.blockSizeLong
            free = stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var imagesBytes = 0L
        var videosBytes = 0L
        var audioBytes = 0L
        var docsBytes = 0L
        var archivesBytes = 0L
        var appsBytes = 0L
        var otherBytes = 0L

        try {
            val projection = arrayOf(MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DATA)
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val mimeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) ?: "" else ""
                    val data = if (dataIndex >= 0) cursor.getString(dataIndex) ?: "" else ""
                    val ext = data.substringAfterLast('.', "").lowercase()

                    when {
                        mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") -> imagesBytes += size
                        mime.startsWith("video/") || ext in setOf("mp4", "mkv", "mov", "avi", "webm") -> videosBytes += size
                        mime.startsWith("audio/") || ext in setOf("mp3", "m4a", "wav", "flac", "ogg", "aac") -> audioBytes += size
                        ext in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md") -> docsBytes += size
                        ext in setOf("zip", "rar", "7z", "tar", "gz") -> archivesBytes += size
                        ext == "apk" -> appsBytes += size
                        else -> otherBytes += size
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Trash size from database
        val trashBytes = dao.getTrashTotalSizeBytes().first() ?: 0L
        val used = (total - free).coerceAtLeast(0L)

        StorageCategoryBreakdown(
            imagesBytes = imagesBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            documentsBytes = docsBytes,
            archivesBytes = archivesBytes,
            appsBytes = appsBytes,
            otherBytes = otherBytes,
            trashBytes = trashBytes,
            totalBytes = total,
            usedBytes = used,
            freeBytes = free
        )
    }

    // 2. Directory Listing & Filtering
    suspend fun listDirectory(
        path: String,
        showHidden: Boolean = false,
        sortField: SortField = SortField.NAME,
        sortDirection: SortDirection = SortDirection.ASCENDING,
        filterCategory: FileCategory? = null,
        searchQuery: String = "",
        minSizeBytes: Long? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val files = dir.listFiles() ?: return@withContext emptyList()
        val starredPaths = dao.getStarredPaths().first().toSet()

        val items = mutableListOf<FileItem>()
        for (f in files) {
            val isHidden = f.name.startsWith(".") || f.name.equals(".nomedia", ignoreCase = true)
            if (!showHidden && isHidden) continue

            val category = MimeUtils.getCategory(f)
            if (filterCategory != null && category != filterCategory) continue

            if (searchQuery.isNotEmpty() && !f.name.contains(searchQuery, ignoreCase = true)) continue

            val size = if (f.isDirectory) 0L else f.length()
            if (minSizeBytes != null && !f.isDirectory && size < minSizeBytes) continue

            val itemCount = if (f.isDirectory) (f.listFiles()?.size ?: 0) else 0
            val isStarred = starredPaths.contains(f.absolutePath)
            val metadata = dao.getFolderMetadataSync(f.absolutePath)

            items.add(
                FileItem(
                    id = f.absolutePath,
                    name = f.name,
                    path = f.absolutePath,
                    size = size,
                    lastModified = f.lastModified(),
                    isDirectory = f.isDirectory,
                    itemCount = itemCount,
                    mimeType = MimeUtils.getMimeType(f),
                    extension = MimeUtils.getExtension(f),
                    isHidden = isHidden,
                    isStarred = isStarred,
                    folderColorHex = metadata?.colorHex,
                    customLabel = metadata?.customLabel,
                    category = category
                )
            )
        }

        // Sort items: folders always first, then by field
        items.sortWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else {
                val comp = when (sortField) {
                    SortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                    SortField.DATE_MODIFIED -> a.lastModified.compareTo(b.lastModified)
                    SortField.SIZE -> a.size.compareTo(b.size)
                    SortField.TYPE -> a.category.name.compareTo(b.category.name)
                    SortField.EXTENSION -> a.extension.compareTo(b.extension, ignoreCase = true)
                }
                if (sortDirection == SortDirection.ASCENDING) comp else -comp
            }
        }
        items
    }

    // 3. File Operations
    suspend fun copyFileOrDirectory(
        sourcePath: String,
        destParentPath: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val src = File(sourcePath)
            if (!src.exists()) return@withContext Result.failure(IllegalArgumentException("Source does not exist"))

            val dest = getAvailableDestinationFile(File(destParentPath, src.name))
            if (src.isDirectory) {
                copyDirectoryRecursive(src, dest, onProgress)
            } else {
                copySingleFile(src, dest, onProgress)
            }
            Result.success(
                FileItem(
                    id = dest.absolutePath,
                    name = dest.name,
                    path = dest.absolutePath,
                    size = if (dest.isDirectory) 0L else dest.length(),
                    lastModified = dest.lastModified(),
                    isDirectory = dest.isDirectory,
                    category = MimeUtils.getCategory(dest)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copySingleFile(
        src: File,
        dest: File,
        onProgress: (Float, String) -> Unit
    ) {
        val totalBytes = src.length().coerceAtLeast(1L)
        var copiedBytes = 0L
        val buffer = ByteArray(32768)
        FileInputStream(src).use { fis ->
            FileOutputStream(dest).use { fos ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                    copiedBytes += read
                    onProgress((copiedBytes.toFloat() / totalBytes).coerceIn(0f, 1f), src.name)
                }
            }
        }
    }

    private fun copyDirectoryRecursive(
        srcDir: File,
        destDir: File,
        onProgress: (Float, String) -> Unit
    ) {
        if (!destDir.exists()) destDir.mkdirs()
        val children = srcDir.listFiles() ?: return
        for (child in children) {
            val target = File(destDir, child.name)
            if (child.isDirectory) {
                copyDirectoryRecursive(child, target, onProgress)
            } else {
                copySingleFile(child, target, onProgress)
            }
        }
    }

    suspend fun moveFileOrDirectory(
        sourcePath: String,
        destParentPath: String
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val src = File(sourcePath)
            if (!src.exists()) return@withContext Result.failure(IllegalArgumentException("Source file does not exist"))
            val dest = getAvailableDestinationFile(File(destParentPath, src.name))

            // Attempt direct rename first
            val renamed = src.renameTo(dest)
            if (renamed) {
                Result.success(
                    FileItem(
                        id = dest.absolutePath,
                        name = dest.name,
                        path = dest.absolutePath,
                        size = dest.length(),
                        lastModified = dest.lastModified(),
                        isDirectory = dest.isDirectory,
                        category = MimeUtils.getCategory(dest)
                    )
                )
            } else {
                // Cross-filesystem move: copy then delete
                val copyResult = copyFileOrDirectory(sourcePath, destParentPath)
                if (copyResult.isSuccess) {
                    deleteRecursive(src)
                    copyResult
                } else {
                    Result.failure(Exception("Failed to move file"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(
        path: String,
        newName: String
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val src = File(path)
            if (!src.exists()) return@withContext Result.failure(IllegalArgumentException("File does not exist"))
            val dest = File(src.parentFile, newName)
            if (dest.exists()) return@withContext Result.failure(IllegalArgumentException("A file with this name already exists"))

            if (src.renameTo(dest)) {
                Result.success(
                    FileItem(
                        id = dest.absolutePath,
                        name = dest.name,
                        path = dest.absolutePath,
                        size = dest.length(),
                        lastModified = dest.lastModified(),
                        isDirectory = dest.isDirectory,
                        category = MimeUtils.getCategory(dest)
                    )
                )
            } else {
                Result.failure(Exception("Rename operation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun duplicateFile(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val src = File(path)
            if (!src.exists()) return@withContext Result.failure(IllegalArgumentException("File not found"))

            val parent = src.parentFile ?: return@withContext Result.failure(IllegalArgumentException("Invalid parent"))
            val dot = src.name.lastIndexOf('.')
            val base = if (dot > 0) src.name.substring(0, dot) else src.name
            val ext = if (dot > 0) src.name.substring(dot) else ""

            var counter = 1
            var candidate: File
            do {
                candidate = File(parent, "$base ($counter)$ext")
                counter++
            } while (candidate.exists())

            if (src.isDirectory) {
                copyDirectoryRecursive(src, candidate) { _, _ -> }
            } else {
                copySingleFile(src, candidate) { _, _ -> }
            }

            Result.success(
                FileItem(
                    id = candidate.absolutePath,
                    name = candidate.name,
                    path = candidate.absolutePath,
                    size = candidate.length(),
                    lastModified = candidate.lastModified(),
                    isDirectory = candidate.isDirectory,
                    category = MimeUtils.getCategory(candidate)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewFolder(parentPath: String, folderName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val dir = File(parentPath, folderName)
            if (dir.exists()) return@withContext Result.failure(IllegalArgumentException("Folder already exists"))
            if (dir.mkdirs()) {
                Result.success(
                    FileItem(
                        id = dir.absolutePath,
                        name = dir.name,
                        path = dir.absolutePath,
                        isDirectory = true,
                        lastModified = dir.lastModified(),
                        category = FileCategory.FOLDER
                    )
                )
            } else {
                Result.failure(Exception("Failed to create folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewFile(parentPath: String, fileName: String, content: String = ""): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(parentPath, fileName)
            if (file.exists()) return@withContext Result.failure(IllegalArgumentException("File already exists"))
            file.writeText(content)
            Result.success(
                FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    isDirectory = false,
                    lastModified = file.lastModified(),
                    mimeType = MimeUtils.getMimeType(file),
                    extension = MimeUtils.getExtension(file),
                    category = MimeUtils.getCategory(file)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Trash & Safe Deletion
    suspend fun moveToTrash(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val src = File(path)
            if (!src.exists()) return@withContext Result.failure(IllegalArgumentException("File does not exist"))

            val trashFileName = UUID.randomUUID().toString() + "_" + src.name
            val trashTarget = File(trashDir, trashFileName)

            val success = src.renameTo(trashTarget) || (
                if (src.isDirectory) {
                    copyDirectoryRecursive(src, trashTarget) { _, _ -> }
                    deleteRecursive(src)
                } else {
                    copySingleFile(src, trashTarget) { _, _ -> }
                    src.delete()
                }
            )

            if (success) {
                dao.insertTrashItem(
                    TrashItemEntity(
                        originalPath = src.absolutePath,
                        trashFileName = trashFileName,
                        displayName = src.name,
                        size = if (trashTarget.isDirectory) 0L else trashTarget.length(),
                        mimeType = MimeUtils.getMimeType(trashTarget),
                        isDirectory = trashTarget.isDirectory
                    )
                )
                Result.success(true)
            } else {
                Result.failure(Exception("Could not move item to Trash"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromTrash(trashItem: TrashItemEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trashDir, trashItem.trashFileName)
            if (!trashFile.exists()) {
                dao.deleteTrashItemById(trashItem.id)
                return@withContext Result.failure(IllegalArgumentException("Trashed file not found on disk"))
            }

            val originalFile = File(trashItem.originalPath)
            originalFile.parentFile?.mkdirs()
            val dest = getAvailableDestinationFile(originalFile)

            val restored = trashFile.renameTo(dest) || (
                if (trashFile.isDirectory) {
                    copyDirectoryRecursive(trashFile, dest) { _, _ -> }
                    deleteRecursive(trashFile)
                } else {
                    copySingleFile(trashFile, dest) { _, _ -> }
                    trashFile.delete()
                }
            )

            if (restored) {
                dao.deleteTrashItemById(trashItem.id)
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to restore item from Trash"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val deleted = deleteRecursive(file)
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val items = dao.getTrashItemsSync()
            var count = 0
            for (item in items) {
                val f = File(trashDir, item.trashFileName)
                if (f.exists()) deleteRecursive(f)
                dao.deleteTrashItemById(item.id)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purgeExpiredTrash(retentionDays: Int): Int = withContext(Dispatchers.IO) {
        if (retentionDays <= 0) return@withContext 0
        val items = dao.getTrashItemsSync()
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
        var purged = 0
        for (item in items) {
            if (item.deletedTimestamp < cutoff) {
                val f = File(trashDir, item.trashFileName)
                if (f.exists()) deleteRecursive(f)
                dao.deleteTrashItemById(item.id)
                purged++
            }
        }
        purged
    }

    private fun deleteRecursive(fileOrDir: File): Boolean {
        if (fileOrDir.isDirectory) {
            fileOrDir.listFiles()?.forEach { deleteRecursive(it) }
        }
        return fileOrDir.delete()
    }

    private fun getAvailableDestinationFile(target: File): File {
        if (!target.exists()) return target
        val parent = target.parentFile
        val dot = target.name.lastIndexOf('.')
        val base = if (dot > 0) target.name.substring(0, dot) else target.name
        val ext = if (dot > 0) target.name.substring(dot) else ""

        var counter = 1
        var candidate: File
        do {
            candidate = File(parent, "$base ($counter)$ext")
            counter++
        } while (candidate.exists())
        return candidate
    }

    // 5. ZIP Compression & Extraction
    suspend fun compressFiles(
        paths: List<String>,
        outputZipPath: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val files = paths.map { File(it) }
            val zipFile = File(outputZipPath)
            val ok = ZipUtils.zipFiles(files, zipFile, onProgress)
            if (ok) {
                Result.success(
                    FileItem(
                        id = zipFile.absolutePath,
                        name = zipFile.name,
                        path = zipFile.absolutePath,
                        size = zipFile.length(),
                        lastModified = zipFile.lastModified(),
                        isDirectory = false,
                        category = FileCategory.ARCHIVE
                    )
                )
            } else {
                Result.failure(Exception("Failed to compress files"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractZipFile(
        zipPath: String,
        destDir: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val zip = File(zipPath)
            val dest = File(destDir)
            if (!dest.exists()) dest.mkdirs()
            val extracted = ZipUtils.unzipFile(zip, dest, onProgress)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Checksums (MD5 / SHA-256)
    suspend fun getFileChecksum(path: String, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
        CryptoUtils.calculateHash(File(path), algorithm)
    }

    // 7. File Split and Merge
    suspend fun splitFile(
        path: String,
        chunkSizeBytes: Long,
        outputDir: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val src = File(path)
            if (!src.exists() || src.isDirectory) return@withContext Result.failure(IllegalArgumentException("Invalid file"))
            val out = File(outputDir).apply { if (!exists()) mkdirs() }

            val totalLength = src.length()
            val chunkCount = ((totalLength + chunkSizeBytes - 1) / chunkSizeBytes).toInt()
            val createdPaths = mutableListOf<String>()

            val buffer = ByteArray(65536)
            FileInputStream(src).use { fis ->
                for (i in 0 until chunkCount) {
                    val partFile = File(out, "${src.name}.part${String.format("%03d", i + 1)}")
                    var remaining = chunkSizeBytes
                    FileOutputStream(partFile).use { fos ->
                        while (remaining > 0) {
                            val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            val read = fis.read(buffer, 0, toRead)
                            if (read == -1) break
                            fos.write(buffer, 0, read)
                            remaining -= read
                        }
                    }
                    createdPaths.add(partFile.absolutePath)
                }
            }
            Result.success(createdPaths)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergeFiles(
        partPaths: List<String>,
        outputFilePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sortedParts = partPaths.sorted()
            val out = File(outputFilePath)
            out.parentFile?.mkdirs()

            val buffer = ByteArray(65536)
            FileOutputStream(out).use { fos ->
                for (partPath in sortedParts) {
                    val part = File(partPath)
                    FileInputStream(part).use { fis ->
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            }
            Result.success(out.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. Batch Rename
    suspend fun batchRename(
        paths: List<String>,
        prefix: String,
        suffix: String,
        findText: String,
        replaceText: String,
        numberingStart: Int?,
        uppercase: Boolean?
    ): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<Pair<String, String>>()
            var currentNum = numberingStart ?: 1
            for (path in paths) {
                val f = File(path)
                val dot = f.name.lastIndexOf('.')
                var baseName = if (dot > 0) f.name.substring(0, dot) else f.name
                val ext = if (dot > 0) f.name.substring(dot) else ""

                if (findText.isNotEmpty()) {
                    baseName = baseName.replace(findText, replaceText)
                }
                if (numberingStart != null) {
                    baseName = "$baseName-$currentNum"
                    currentNum++
                }
                var newName = "$prefix$baseName$suffix$ext"
                if (uppercase == true) {
                    newName = newName.uppercase()
                } else if (uppercase == false) {
                    newName = newName.lowercase()
                }

                val target = File(f.parentFile, newName)
                if (f.renameTo(target)) {
                    results.add(path to target.absolutePath)
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. Storage Cleaner Scanners
    suspend fun scanDuplicateFiles(
        rootPath: String,
        onProgress: (String) -> Unit = {}
    ): List<DuplicateFileGroup> = withContext(Dispatchers.IO) {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        val root = File(rootPath)

        fun collectFiles(dir: File) {
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    // Skip hidden or system-heavy directories
                    if (!f.name.startsWith(".")) collectFiles(f)
                } else if (f.length() > 1024L) { // Only files > 1KB
                    val s = f.length()
                    sizeMap.getOrPut(s) { mutableListOf() }.add(f)
                }
            }
        }
        collectFiles(root)

        // Only hash sizes that have >= 2 files
        val candidateSizes = sizeMap.filter { it.value.size > 1 }
        val duplicatesByHash = mutableMapOf<String, MutableList<File>>()

        for ((_, fileList) in candidateSizes) {
            for (f in fileList) {
                onProgress(f.name)
                val hash = CryptoUtils.calculateHash(f, "SHA-256")
                if (hash.isNotEmpty()) {
                    duplicatesByHash.getOrPut(hash) { mutableListOf() }.add(f)
                }
            }
        }

        val groups = duplicatesByHash
            .filter { it.value.size > 1 }
            .map { (hash, files) ->
                DuplicateFileGroup(
                    hash = hash,
                    size = files.first().length(),
                    files = files.map { f ->
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            isDirectory = false,
                            category = MimeUtils.getCategory(f)
                        )
                    }
                )
            }
            .sortedByDescending { it.size * (it.files.size - 1) }

        groups
    }

    suspend fun scanLargeFiles(
        rootPath: String,
        minSizeBytes: Long = 20L * 1024 * 1024 // 20 MB default
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val largeFiles = mutableListOf<FileItem>()
        fun scan(dir: File) {
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    if (!f.name.startsWith(".")) scan(f)
                } else if (f.length() >= minSizeBytes) {
                    largeFiles.add(
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            category = MimeUtils.getCategory(f)
                        )
                    )
                }
            }
        }
        scan(File(rootPath))
        largeFiles.sortByDescending { it.size }
        largeFiles
    }

    suspend fun scanEmptyFolders(rootPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val emptyDirs = mutableListOf<FileItem>()
        fun scan(dir: File) {
            val list = dir.listFiles() ?: return
            if (list.isEmpty()) {
                emptyDirs.add(
                    FileItem(
                        id = dir.absolutePath,
                        name = dir.name,
                        path = dir.absolutePath,
                        isDirectory = true,
                        itemCount = 0,
                        lastModified = dir.lastModified(),
                        category = FileCategory.FOLDER
                    )
                )
            } else {
                for (f in list) {
                    if (f.isDirectory && !f.name.startsWith(".")) {
                        scan(f)
                    }
                }
            }
        }
        scan(File(rootPath))
        emptyDirs
    }

    // 10. Root Access Module
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/daemonsu"
        )
        for (p in paths) {
            if (File(p).exists()) return@withContext true
        }

        // Test running su command
        return@withContext try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    // 11. Installed Apps Manager
    suspend fun getInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<InstalledAppItem>()

        for (app in apps) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val name = pm.getApplicationLabel(app).toString()
            val apkFile = File(app.sourceDir)
            val size = if (apkFile.exists()) apkFile.length() else 0L
            val pkgInfo = try {
                pm.getPackageInfo(app.packageName, 0)
            } catch (e: Exception) {
                null
            }

            result.add(
                InstalledAppItem(
                    packageName = app.packageName,
                    appName = name,
                    versionName = pkgInfo?.versionName ?: "1.0",
                    apkSize = size,
                    isSystemApp = isSystem,
                    installTime = pkgInfo?.firstInstallTime ?: 0L,
                    apkPath = app.sourceDir
                )
            )
        }
        result.sortByDescending { it.apkSize }
        result
    }

    suspend fun extractApk(packageName: String, destDirPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val srcApk = File(appInfo.sourceDir)
            if (!srcApk.exists()) return@withContext Result.failure(IllegalArgumentException("Source APK not found"))

            val destDir = File(destDirPath).apply { if (!exists()) mkdirs() }
            val label = pm.getApplicationLabel(appInfo).toString().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetApk = getAvailableDestinationFile(File(destDir, "$label-$packageName.apk"))

            copySingleFile(srcApk, targetApk) { _, _ -> }
            Result.success(targetApk.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 12. Encrypted Vault (AES-256)
    suspend fun importFileToVault(
        filePath: String,
        passcode: String,
        deleteOriginal: Boolean = true
    ): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        try {
            val src = File(filePath)
            if (!src.exists() || src.isDirectory) return@withContext Result.failure(IllegalArgumentException("Invalid file"))

            val salt = "SalimSaltKey2026".toByteArray(Charsets.UTF_8)
            val key = CryptoUtils.deriveKey(passcode, salt)
            val encFileName = UUID.randomUUID().toString() + ".enc"
            val encFile = File(vaultDir, encFileName)

            val iv = CryptoUtils.encryptFile(src, encFile, key)
            val ivHex = iv.joinToString("") { "%02x".format(it) }

            val item = VaultItemEntity(
                originalName = src.name,
                originalPath = src.absolutePath,
                encryptedFileName = encFileName,
                size = src.length(),
                mimeType = MimeUtils.getMimeType(src),
                ivHex = ivHex
            )
            val id = dao.insertVaultItem(item)

            if (deleteOriginal) {
                src.delete()
            }
            Result.success(item.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportFileFromVault(
        vaultItem: VaultItemEntity,
        passcode: String,
        targetDir: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultDir, vaultItem.encryptedFileName)
            if (!encFile.exists()) return@withContext Result.failure(IllegalArgumentException("Encrypted file missing"))

            val salt = "SalimSaltKey2026".toByteArray(Charsets.UTF_8)
            val key = CryptoUtils.deriveKey(passcode, salt)
            val iv = vaultItem.ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

            val dest = getAvailableDestinationFile(File(targetDir, vaultItem.originalName))
            CryptoUtils.decryptFile(encFile, dest, key, iv)
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(vaultItem: VaultItemEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultDir, vaultItem.encryptedFileName)
            if (encFile.exists()) encFile.delete()
            dao.deleteVaultItemById(vaultItem.id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 13. Bookmarks, Recents, and Metadata queries
    fun getBookmarks(): Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    suspend fun addBookmark(name: String, path: String) = dao.insertBookmark(BookmarkEntity(name = name, path = path))
    suspend fun removeBookmark(path: String) = dao.deleteBookmarkByPath(path)
    fun isBookmarked(path: String): Flow<Boolean> = dao.isBookmarked(path)

    fun getRecentFiles(): Flow<List<RecentFileEntity>> = dao.getRecentFiles()
    suspend fun recordFileAccess(file: File) {
        if (!file.exists() || file.isDirectory) return
        dao.recordFileAccess(
            RecentFileEntity(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                mimeType = MimeUtils.getMimeType(file)
            )
        )
    }

    fun getTrashItems(): Flow<List<TrashItemEntity>> = dao.getAllTrashItems()
    fun getVaultItems(): Flow<List<VaultItemEntity>> = dao.getAllVaultItems()

    suspend fun toggleStar(path: String, currentlyStarred: Boolean) {
        val existing = dao.getFolderMetadataSync(path)
        dao.setFolderMetadata(
            FolderMetadataEntity(
                path = path,
                customLabel = existing?.customLabel,
                colorHex = existing?.colorHex,
                isStarred = !currentlyStarred,
                isLocked = existing?.isLocked ?: false
            )
        )
    }

    suspend fun setFolderColor(path: String, colorHex: String?) {
        val existing = dao.getFolderMetadataSync(path)
        dao.setFolderMetadata(
            FolderMetadataEntity(
                path = path,
                customLabel = existing?.customLabel,
                colorHex = colorHex,
                isStarred = existing?.isStarred ?: false,
                isLocked = existing?.isLocked ?: false
            )
        )
    }

    suspend fun setFolderLabel(path: String, label: String?) {
        val existing = dao.getFolderMetadataSync(path)
        dao.setFolderMetadata(
            FolderMetadataEntity(
                path = path,
                customLabel = label,
                colorHex = existing?.colorHex,
                isStarred = existing?.isStarred ?: false,
                isLocked = existing?.isLocked ?: false
            )
        )
    }
}
