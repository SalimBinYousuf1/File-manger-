package com.example.ui.screens.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.ViewMode
import com.example.ui.SalimMainViewModel
import com.example.ui.components.BatchRenameDialog
import com.example.ui.components.CreateItemDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileTypeIcon
import com.example.ui.components.PropertiesBottomSheet
import com.example.ui.components.RenameDialog
import com.example.ui.components.SalimBreadcrumbBar
import com.example.ui.theme.TabularStyle
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: SalimMainViewModel,
    onOpenFileInEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPath by viewModel.currentPath.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val isSplitPaneActive by viewModel.isSplitPaneActive.collectAsState()
    val splitPath by viewModel.splitPanePath.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val splitFileList by viewModel.splitFileList.collectAsState()
    val isLoading by viewModel.isLoadingFiles.collectAsState()
    val selectedPaths by viewModel.selectedFilePaths.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val showHidden by viewModel.showHidden.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val operationProgress by viewModel.operationProgress.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf<Boolean?>(null) } // true: folder, false: file, null: closed
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<FileItem>?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var activeActionFile by remember { mutableStateOf<FileItem?>(null) }
    var inspectPropertiesFile by remember { mutableStateOf<FileItem?>(null) }
    var showSearchInput by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    val propertiesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contextSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (clipboard != null) {
                FloatingActionButton(
                    onClick = { viewModel.pasteClipboard() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("paste_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste (${clipboard!!.first.size})", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (selectedPaths.isEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_folder_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Item")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Strip (Vercel-style clean tabs)
            if (tabs.size > 1 || isSplitPaneActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tabPath ->
                        val isActive = index == activeTabIndex
                        val tabName = File(tabPath).name.ifEmpty { "Storage" }
                        Surface(
                            onClick = { viewModel.switchTab(index) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                if (tabs.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.closeTab(index) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openNewTab() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Top Control Bar: Search, View Mode, Sort, Split Screen, New File
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val parent = File(currentPath).parentFile
                            if (parent != null && parent.exists() && parent.canRead()) {
                                viewModel.loadDirectory(parent.absolutePath)
                            }
                        },
                        enabled = File(currentPath).parentFile != null && currentPath != "/"
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go up")
                    }

                    IconButton(onClick = { showSearchInput = !showSearchInput }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearchInput || searchInput.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Split pane toggle
                    IconButton(
                        onClick = { viewModel.toggleSplitPane() },
                        modifier = Modifier.testTag("split_pane_toggle")
                    ) {
                        Icon(
                            Icons.Default.VerticalSplit,
                            contentDescription = "Dual Pane Split",
                            tint = if (isSplitPaneActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // View Mode toggle (List vs Grid)
                    IconButton(
                        onClick = {
                            viewModel.setViewMode(if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                        },
                        modifier = Modifier.testTag("view_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle View Mode"
                        )
                    }

                    // Sort button with dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort options")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortField.values().forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    trailingIcon = {
                                        if (sortField == field) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        val newDir = if (sortField == field) {
                                            if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                                        } else {
                                            SortDirection.ASCENDING
                                        }
                                        viewModel.setSort(field, newDir)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // More Menu (New File, Refresh, Show Hidden)
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Folder") },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showCreateDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Text File") },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showCreateDialog = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showHidden) "Hide Hidden Files" else "Show Hidden Files") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.toggleHiddenFiles()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.refreshCurrentDirectory()
                                }
                            )
                        }
                    }
                }
            }

            // Search Bar Input (Expands smoothly)
            AnimatedVisibility(visible = showSearchInput) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.setSearchQuery(it)
                    },
                    placeholder = { Text("Search files by name...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                viewModel.setSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Breadcrumb bar
            SalimBreadcrumbBar(
                currentPath = currentPath,
                onNavigateToPath = { viewModel.loadDirectory(it) }
            )

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    null to "All",
                    FileCategory.IMAGE to "Images",
                    FileCategory.VIDEO to "Videos",
                    FileCategory.AUDIO to "Audio",
                    FileCategory.DOCUMENT to "Docs",
                    FileCategory.ARCHIVE to "Archives",
                    FileCategory.APK to "APKs",
                    FileCategory.CODE to "Code"
                )
                categories.forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Real Operation Progress Bar (if operation is running)
            if (operationProgress.isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            operationProgress.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            "${(operationProgress.percent * 100).toInt()}%",
                            style = TabularStyle
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { operationProgress.percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            // Multi-Selection Action Bar (if items selected)
            AnimatedVisibility(visible = selectedPaths.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear selection", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedPaths.size} selected",
                                style = TabularStyle.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.toggleSelectAll() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Select all", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.copySelected() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.cutSelected() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Cut", modifier = Modifier.size(18.dp))
                            }
                            if (selectedPaths.size > 1) {
                                IconButton(onClick = { showBatchRenameDialog = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Batch Rename", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    val toZip = selectedPaths.toList()
                                    viewModel.compressFiles(toZip, "Archive")
                                    viewModel.clearSelection()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "Zip", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val selectedItems = fileList.filter { selectedPaths.contains(it.path) }
                                    showDeleteDialog = selectedItems
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area (Dual Pane or Single Pane)
            if (isSplitPaneActive) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Pane 1 (Primary)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        FileListContent(
                            files = fileList,
                            isLoading = isLoading,
                            viewMode = viewMode,
                            selectedPaths = selectedPaths,
                            onFileClick = { file -> handleFileClick(file, context, viewModel, onOpenFileInEditor) },
                            onFileLongClick = { file -> activeActionFile = file },
                            onToggleSelect = { path -> viewModel.toggleSelection(path) },
                            onCreateFolder = { showCreateDialog = true }
                        )
                    }

                    // Pane divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Pane 2 (Secondary Split Pane)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SalimBreadcrumbBar(
                                currentPath = splitPath,
                                onNavigateToPath = { viewModel.loadSplitDirectory(it) }
                            )
                            FileListContent(
                                files = splitFileList,
                                isLoading = false,
                                viewMode = viewMode,
                                selectedPaths = emptySet(),
                                onFileClick = { file ->
                                    if (file.isDirectory) {
                                        viewModel.loadSplitDirectory(file.path)
                                    } else {
                                        handleFileClick(file, context, viewModel, onOpenFileInEditor)
                                    }
                                },
                                onFileLongClick = { file -> activeActionFile = file },
                                onToggleSelect = {},
                                onCreateFolder = {}
                            )
                        }
                    }
                }
            } else {
                // Single Pane Primary View
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    FileListContent(
                        files = fileList,
                        isLoading = isLoading,
                        viewMode = viewMode,
                        selectedPaths = selectedPaths,
                        onFileClick = { file -> handleFileClick(file, context, viewModel, onOpenFileInEditor) },
                        onFileLongClick = { file -> activeActionFile = file },
                        onToggleSelect = { path -> viewModel.toggleSelection(path) },
                        onCreateFolder = { showCreateDialog = true }
                    )
                }
            }
        }
    }

    // Modal Action Sheet (Context menu on long click)
    activeActionFile?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { activeActionFile = null },
            sheetState = contextSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileTypeIcon(
                        category = file.category,
                        isDirectory = file.isDirectory,
                        fileName = file.name,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (file.isDirectory) "${file.itemCount} items" else file.formattedSize,
                            style = TabularStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionSheetItem(
                    icon = Icons.Default.Info,
                    label = "Details & Checksums",
                    onClick = {
                        activeActionFile = null
                        inspectPropertiesFile = file
                    }
                )

                ActionSheetItem(
                    icon = if (file.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (file.isStarred) "Remove from Starred" else "Add to Starred",
                    onClick = {
                        viewModel.toggleStar(file.path, file.isStarred)
                        activeActionFile = null
                    }
                )

                ActionSheetItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = {
                        viewModel.toggleSelection(file.path)
                        viewModel.copySelected()
                        activeActionFile = null
                    }
                )

                ActionSheetItem(
                    icon = Icons.Default.ContentCut,
                    label = "Move",
                    onClick = {
                        viewModel.toggleSelection(file.path)
                        viewModel.cutSelected()
                        activeActionFile = null
                    }
                )

                ActionSheetItem(
                    icon = Icons.Default.Edit,
                    label = "Rename",
                    onClick = {
                        activeActionFile = null
                        showRenameDialog = file
                    }
                )

                ActionSheetItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Duplicate",
                    onClick = {
                        viewModel.duplicateFile(file.path)
                        activeActionFile = null
                    }
                )

                if (file.extension == "zip") {
                    ActionSheetItem(
                        icon = Icons.Default.Archive,
                        label = "Extract Archive",
                        onClick = {
                            viewModel.extractZip(file.path)
                            activeActionFile = null
                        }
                    )
                } else {
                    ActionSheetItem(
                        icon = Icons.Default.Archive,
                        label = "Compress to Zip",
                        onClick = {
                            viewModel.compressFiles(listOf(file.path), file.name)
                            activeActionFile = null
                        }
                    )
                }

                ActionSheetItem(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    isDestructive = true,
                    onClick = {
                        activeActionFile = null
                        showDeleteDialog = listOf(file)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Properties Inspector Sheet
    inspectPropertiesFile?.let { file ->
        PropertiesBottomSheet(
            fileItem = file,
            sheetState = propertiesSheetState,
            onDismiss = { inspectPropertiesFile = null },
            onCalculateHash = { path, alg -> viewModel.fileRepo.getFileChecksum(path, alg) },
            onColorFolder = { path, hex -> viewModel.setFolderColor(path, hex) },
            onSetLabel = { path, label -> scope.launch { viewModel.fileRepo.setFolderLabel(path, label) } }
        )
    }

    // Dialogs
    showCreateDialog?.let { isFolder ->
        CreateItemDialog(
            isFolder = isFolder,
            onDismiss = { showCreateDialog = null },
            onConfirm = { name ->
                if (isFolder) viewModel.createFolder(name) else viewModel.createFile(name)
                showCreateDialog = null
            }
        )
    }

    showRenameDialog?.let { file ->
        RenameDialog(
            currentName = file.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                viewModel.renameFile(file.path, newName)
                showRenameDialog = null
            }
        )
    }

    showDeleteDialog?.let { files ->
        DeleteConfirmDialog(
            targetName = files.firstOrNull()?.name ?: "",
            count = files.size,
            onDismiss = { showDeleteDialog = null },
            onConfirm = { permanent ->
                viewModel.deleteFiles(files.map { it.path }, permanent)
                showDeleteDialog = null
            }
        )
    }

    if (showBatchRenameDialog) {
        val selectedItems = fileList.filter { selectedPaths.contains(it.path) }
        BatchRenameDialog(
            selectedFiles = selectedItems,
            onDismiss = { showBatchRenameDialog = false },
            onConfirm = { prefix, suffix, find, replace, num, uc ->
                viewModel.batchRename(prefix, suffix, find, replace, num, uc)
                showBatchRenameDialog = false
            }
        )
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun FileListContent(
    files: List<FileItem>,
    isLoading: Boolean,
    viewMode: ViewMode,
    selectedPaths: Set<String>,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onToggleSelect: (String) -> Unit,
    onCreateFolder: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
    } else if (files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "This folder is empty.",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onCreateFolder) {
                    Text("Create new folder")
                }
            }
        }
    } else {
        if (viewMode == ViewMode.LIST) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    val isSelected = selectedPaths.contains(item.path)
                    FileRowItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedPaths.isNotEmpty()) {
                                onToggleSelect(item.path)
                            } else {
                                onFileClick(item)
                            }
                        },
                        onLongClick = {
                            if (selectedPaths.isEmpty()) {
                                onFileLongClick(item)
                            } else {
                                onToggleSelect(item.path)
                            }
                        }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    val isSelected = selectedPaths.contains(item.path)
                    FileGridItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedPaths.isNotEmpty()) {
                                onToggleSelect(item.path)
                            } else {
                                onFileClick(item)
                            }
                        },
                        onLongClick = {
                            if (selectedPaths.isEmpty()) {
                                onFileLongClick(item)
                            } else {
                                onToggleSelect(item.path)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRowItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("file_row_${item.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeIcon(
                category = item.category,
                isDirectory = item.isDirectory,
                fileName = item.name,
                customColor = item.folderColorHex?.let { Color(android.graphics.Color.parseColor(it)) },
                size = 38.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.isStarred) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isDirectory) "${item.itemCount} items" else item.formattedSize,
                        style = TabularStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "  •  ${item.compactDate}",
                        style = TabularStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("file_grid_${item.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileTypeIcon(
                category = item.category,
                isDirectory = item.isDirectory,
                fileName = item.name,
                customColor = item.folderColorHex?.let { Color(android.graphics.Color.parseColor(it)) },
                size = 46.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (item.isDirectory) "${item.itemCount} items" else item.formattedSize,
                style = TabularStyle.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun handleFileClick(
    file: FileItem,
    context: Context,
    viewModel: SalimMainViewModel,
    onOpenFileInEditor: (String) -> Unit
) {
    if (file.isDirectory) {
        viewModel.loadDirectory(file.path)
        return
    }

    // Text & code files open inside built-in Plain Text Editor
    if (file.category == FileCategory.CODE || file.mimeType.startsWith("text/") || file.extension in setOf("txt", "md", "json", "xml", "log", "py", "kt", "java", "sh", "gradle")) {
        onOpenFileInEditor(file.path)
        return
    }

    // Media & other documents open via Android Intent
    try {
        val f = File(file.path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType.ifEmpty { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}
