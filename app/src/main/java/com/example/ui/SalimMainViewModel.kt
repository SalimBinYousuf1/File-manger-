package com.example.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SalimDatabase
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.local.entity.VaultItemEntity
import com.example.data.model.DuplicateFileGroup
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.InstalledAppItem
import com.example.data.model.OperationProgress
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.StorageCategoryBreakdown
import com.example.data.model.StorageVolumeInfo
import com.example.data.model.ViewMode
import com.example.data.repository.FileSystemRepository
import com.example.data.repository.SalimPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SalimMainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SalimDatabase.getDatabase(application)
    val fileRepo = FileSystemRepository(application, database.salimDao())
    val prefsRepo = SalimPreferencesRepository(application)

    // Current primary directory path
    private val defaultRoot = Environment.getExternalStorageDirectory().absolutePath
    private val _currentPath = MutableStateFlow(defaultRoot)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // Tabbed browsing
    private val _tabs = MutableStateFlow(listOf(defaultRoot))
    val tabs: StateFlow<List<String>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Dual pane split view
    private val _isSplitPaneActive = MutableStateFlow(false)
    val isSplitPaneActive: StateFlow<Boolean> = _isSplitPaneActive.asStateFlow()

    private val _splitPanePath = MutableStateFlow(
        File(defaultRoot, "Download").let { if (it.exists()) it.absolutePath else defaultRoot }
    )
    val splitPanePath: StateFlow<String> = _splitPanePath.asStateFlow()

    // File listing & filtering
    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList.asStateFlow()

    private val _splitFileList = MutableStateFlow<List<FileItem>>(emptyList())
    val splitFileList: StateFlow<List<FileItem>> = _splitFileList.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    // Multi-selection
    private val _selectedFilePaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilePaths: StateFlow<Set<String>> = _selectedFilePaths.asStateFlow()

    // Clipboard (Paths to copy or move, isMove flag)
    private val _clipboard = MutableStateFlow<Pair<List<String>, Boolean>?>(null)
    val clipboard: StateFlow<Pair<List<String>, Boolean>?> = _clipboard.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    // Storage info & Categories
    private val _storageBreakdown = MutableStateFlow(StorageCategoryBreakdown())
    val storageBreakdown: StateFlow<StorageCategoryBreakdown> = _storageBreakdown.asStateFlow()

    private val _volumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val volumes: StateFlow<List<StorageVolumeInfo>> = _volumes.asStateFlow()

    // Operation progress
    private val _operationProgress = MutableStateFlow(OperationProgress())
    val operationProgress: StateFlow<OperationProgress> = _operationProgress.asStateFlow()

    // Settings & Preferences
    val theme = prefsRepo.themeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val viewMode = prefsRepo.viewModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ViewMode.LIST)
    val sortField = prefsRepo.sortFieldFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SortField.NAME)
    val sortDirection = prefsRepo.sortDirectionFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SortDirection.ASCENDING)
    val showHidden = prefsRepo.showHiddenFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val confirmDelete = prefsRepo.confirmDeleteFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val trashRetentionDays = prefsRepo.trashRetentionDaysFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    val enableRoot = prefsRepo.enableRootFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val appLockEnabled = prefsRepo.appLockEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isAppUnlocked = MutableStateFlow(false)

    // Bookmarks, Recents, Trash, Apps, Vault
    val bookmarks = fileRepo.getBookmarks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recentFiles = fileRepo.getRecentFiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val trashItems = fileRepo.getTrashItems().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val vaultItems = fileRepo.getVaultItems().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Cleaner Scan states
    val duplicateGroups = MutableStateFlow<List<DuplicateFileGroup>>(emptyList())
    val largeFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val emptyFolders = MutableStateFlow<List<FileItem>>(emptyList())
    val isCleanerScanning = MutableStateFlow(false)

    // App Manager
    val installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val isLoadingApps = MutableStateFlow(false)

    init {
        refreshStorageInfo()
        loadDirectory(_currentPath.value)

        // Purge expired trash on launch based on retention setting
        viewModelScope.launch {
            val days = prefsRepo.trashRetentionDaysFlow.first()
            fileRepo.purgeExpiredTrash(days)
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            _currentPath.value = path

            // Update current tab path
            val currentTabs = _tabs.value.toMutableList()
            if (_activeTabIndex.value in currentTabs.indices) {
                currentTabs[_activeTabIndex.value] = path
                _tabs.value = currentTabs
            }

            val list = fileRepo.listDirectory(
                path = path,
                showHidden = showHidden.value,
                sortField = sortField.value,
                sortDirection = sortDirection.value,
                filterCategory = _selectedCategory.value,
                searchQuery = _searchQuery.value
            )
            _fileList.value = list
            _isLoadingFiles.value = false
            _selectedFilePaths.value = emptySet()
        }
    }

    fun loadSplitDirectory(path: String) {
        viewModelScope.launch {
            _splitPanePath.value = path
            val list = fileRepo.listDirectory(
                path = path,
                showHidden = showHidden.value,
                sortField = sortField.value,
                sortDirection = sortDirection.value
            )
            _splitFileList.value = list
        }
    }

    fun refreshCurrentDirectory() {
        loadDirectory(_currentPath.value)
        if (_isSplitPaneActive.value) {
            loadSplitDirectory(_splitPanePath.value)
        }
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            _volumes.value = fileRepo.getVolumes()
            _storageBreakdown.value = fileRepo.getStorageCategoryBreakdown()
        }
    }

    // Tab Management
    fun openNewTab(path: String = defaultRoot) {
        val current = _tabs.value.toMutableList()
        current.add(path)
        _tabs.value = current
        _activeTabIndex.value = current.size - 1
        loadDirectory(path)
    }

    fun switchTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
            loadDirectory(_tabs.value[index])
        }
    }

    fun closeTab(index: Int) {
        val current = _tabs.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _tabs.value = current
            val newIndex = (_activeTabIndex.value - 1).coerceAtLeast(0)
            _activeTabIndex.value = newIndex
            loadDirectory(current[newIndex])
        }
    }

    fun toggleSplitPane() {
        _isSplitPaneActive.value = !_isSplitPaneActive.value
        if (_isSplitPaneActive.value) {
            loadSplitDirectory(_splitPanePath.value)
        }
    }

    // Search and Category Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadDirectory(_currentPath.value)
    }

    fun setSelectedCategory(category: FileCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        loadDirectory(_currentPath.value)
    }

    // Selection
    fun toggleSelection(path: String) {
        val current = _selectedFilePaths.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _selectedFilePaths.value = current
    }

    fun selectAll() {
        _selectedFilePaths.value = _fileList.value.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedFilePaths.value = emptySet()
    }

    fun toggleSelectAll() {
        if (_selectedFilePaths.value.size == _fileList.value.size) {
            clearSelection()
        } else {
            selectAll()
        }
    }

    // Clipboard (Copy / Cut)
    fun copySelected() {
        _clipboard.value = _selectedFilePaths.value.toList() to false
        clearSelection()
    }

    fun cutSelected() {
        _clipboard.value = _selectedFilePaths.value.toList() to true
        clearSelection()
    }

    fun pasteClipboard(targetDir: String = _currentPath.value) {
        val clip = _clipboard.value ?: return
        val (paths, isMove) = clip
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(
                title = if (isMove) "Moving files..." else "Copying files...",
                isRunning = true
            )
            for ((index, path) in paths.withIndex()) {
                val f = File(path)
                _operationProgress.value = _operationProgress.value.copy(
                    currentItemName = f.name,
                    percent = (index.toFloat() / paths.size.toFloat())
                )
                if (isMove) {
                    fileRepo.moveFileOrDirectory(path, targetDir)
                } else {
                    fileRepo.copyFileOrDirectory(path, targetDir) { p, name ->
                        _operationProgress.value = _operationProgress.value.copy(
                            currentItemName = name,
                            percent = p
                        )
                    }
                }
            }
            _operationProgress.value = OperationProgress()
            if (isMove) _clipboard.value = null
            refreshCurrentDirectory()
        }
    }

    // File Actions
    fun deleteFiles(paths: List<String>, permanent: Boolean) {
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(title = "Deleting...", isRunning = true)
            for (p in paths) {
                if (permanent) {
                    fileRepo.deletePermanently(p)
                } else {
                    fileRepo.moveToTrash(p)
                }
            }
            _operationProgress.value = OperationProgress()
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            fileRepo.renameFile(path, newName)
            refreshCurrentDirectory()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            fileRepo.createNewFolder(_currentPath.value, name)
            refreshCurrentDirectory()
        }
    }

    fun createFile(name: String, content: String = "") {
        viewModelScope.launch {
            fileRepo.createNewFile(_currentPath.value, name, content)
            refreshCurrentDirectory()
        }
    }

    fun duplicateFile(path: String) {
        viewModelScope.launch {
            fileRepo.duplicateFile(path)
            refreshCurrentDirectory()
        }
    }

    fun compressFiles(paths: List<String>, zipName: String) {
        viewModelScope.launch {
            val destZip = File(_currentPath.value, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
            _operationProgress.value = OperationProgress(title = "Compressing...", isRunning = true)
            fileRepo.compressFiles(paths, destZip.absolutePath) { p, name ->
                _operationProgress.value = _operationProgress.value.copy(currentItemName = name, percent = p)
            }
            _operationProgress.value = OperationProgress()
            refreshCurrentDirectory()
        }
    }

    fun extractZip(zipPath: String) {
        viewModelScope.launch {
            val src = File(zipPath)
            val outDir = File(src.parentFile, src.nameWithoutExtension)
            _operationProgress.value = OperationProgress(title = "Extracting...", isRunning = true)
            fileRepo.extractZipFile(zipPath, outDir.absolutePath) { p, name ->
                _operationProgress.value = _operationProgress.value.copy(currentItemName = name, percent = p)
            }
            _operationProgress.value = OperationProgress()
            refreshCurrentDirectory()
        }
    }

    fun batchRename(prefix: String, suffix: String, find: String, replace: String, numbering: Int?, uppercase: Boolean?) {
        viewModelScope.launch {
            val selected = _selectedFilePaths.value.toList()
            fileRepo.batchRename(selected, prefix, suffix, find, replace, numbering, uppercase)
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun splitFile(path: String, chunkMb: Int) {
        viewModelScope.launch {
            val bytes = chunkMb.toLong() * 1024L * 1024L
            fileRepo.splitFile(path, bytes, _currentPath.value)
            refreshCurrentDirectory()
        }
    }

    fun mergeFiles(parts: List<String>, outputName: String) {
        viewModelScope.launch {
            val outFile = File(_currentPath.value, outputName)
            fileRepo.mergeFiles(parts, outFile.absolutePath)
            refreshCurrentDirectory()
        }
    }

    // Trash management
    fun restoreTrashItem(item: TrashItemEntity) {
        viewModelScope.launch {
            fileRepo.restoreFromTrash(item)
            refreshCurrentDirectory()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            fileRepo.emptyTrash()
            refreshCurrentDirectory()
        }
    }

    // Cleaner Scanners
    fun startCleanerScan(rootPath: String = defaultRoot) {
        viewModelScope.launch {
            isCleanerScanning.value = true
            duplicateGroups.value = fileRepo.scanDuplicateFiles(rootPath)
            largeFiles.value = fileRepo.scanLargeFiles(rootPath)
            emptyFolders.value = fileRepo.scanEmptyFolders(rootPath)
            isCleanerScanning.value = false
        }
    }

    // App Manager
    fun loadInstalledApps() {
        viewModelScope.launch {
            isLoadingApps.value = true
            installedApps.value = fileRepo.getInstalledApps()
            isLoadingApps.value = false
        }
    }

    fun extractApk(packageName: String) {
        viewModelScope.launch {
            val dest = File(defaultRoot, "Download/Salim_APKs").apply { if (!exists()) mkdirs() }
            fileRepo.extractApk(packageName, dest.absolutePath)
        }
    }

    // View Options
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { prefsRepo.setViewMode(mode) }
    fun setSort(field: SortField, direction: SortDirection) = viewModelScope.launch {
        prefsRepo.setSort(field, direction)
        loadDirectory(_currentPath.value)
    }
    fun toggleHiddenFiles() = viewModelScope.launch {
        prefsRepo.setShowHidden(!showHidden.value)
        loadDirectory(_currentPath.value)
    }

    // Bookmarks and Starred
    fun toggleBookmark(path: String) = viewModelScope.launch {
        val f = File(path)
        val isBm = bookmarks.value.any { it.path == path }
        if (isBm) {
            fileRepo.removeBookmark(path)
        } else {
            fileRepo.addBookmark(f.name, path)
        }
    }

    fun toggleStar(path: String, currentStarred: Boolean) = viewModelScope.launch {
        fileRepo.toggleStar(path, currentStarred)
        loadDirectory(_currentPath.value)
    }

    fun setFolderColor(path: String, colorHex: String?) = viewModelScope.launch {
        fileRepo.setFolderColor(path, colorHex)
        loadDirectory(_currentPath.value)
    }
}
