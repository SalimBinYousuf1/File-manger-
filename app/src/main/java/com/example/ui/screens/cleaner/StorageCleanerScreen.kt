package com.example.ui.screens.cleaner

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.ui.SalimMainViewModel
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileTypeIcon
import com.example.ui.theme.TabularStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanerScreen(
    viewModel: SalimMainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isCleanerScanning.collectAsState()
    val duplicates by viewModel.duplicateGroups.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val emptyFolders by viewModel.emptyFolders.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedPathsToDelete by remember { mutableStateOf(setOf<String>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (duplicates.isEmpty() && largeFiles.isEmpty() && emptyFolders.isEmpty()) {
            viewModel.startCleanerScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Cleaner", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCleanerScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-scan")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedPathsToDelete.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPathsToDelete.size} items selected",
                            style = TabularStyle.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Button(
                            onClick = { showConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("cleaner_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Selected")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Duplicates (${duplicates.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Large Files (${largeFiles.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Empty Folders (${emptyFolders.size})") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("App Cache") }
                )
            }

            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing storage bytes...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> DuplicatesList(
                        groups = duplicates,
                        selectedPaths = selectedPathsToDelete,
                        onTogglePath = { path ->
                            selectedPathsToDelete = if (selectedPathsToDelete.contains(path)) {
                                selectedPathsToDelete - path
                            } else {
                                selectedPathsToDelete + path
                            }
                        }
                    )
                    1 -> LargeFilesList(
                        files = largeFiles,
                        selectedPaths = selectedPathsToDelete,
                        onTogglePath = { path ->
                            selectedPathsToDelete = if (selectedPathsToDelete.contains(path)) {
                                selectedPathsToDelete - path
                            } else {
                                selectedPathsToDelete + path
                            }
                        }
                    )
                    2 -> EmptyFoldersList(
                        folders = emptyFolders,
                        selectedPaths = selectedPathsToDelete,
                        onTogglePath = { path ->
                            selectedPathsToDelete = if (selectedPathsToDelete.contains(path)) {
                                selectedPathsToDelete - path
                            } else {
                                selectedPathsToDelete + path
                            }
                        }
                    )
                    3 -> AppCacheSection(context = context)
                }
            }
        }
    }

    if (showConfirmDialog) {
        DeleteConfirmDialog(
            targetName = "${selectedPathsToDelete.size} items",
            count = selectedPathsToDelete.size,
            onDismiss = { showConfirmDialog = false },
            onConfirm = { permanent ->
                viewModel.deleteFiles(selectedPathsToDelete.toList(), permanent)
                selectedPathsToDelete = emptySet()
                showConfirmDialog = false
                viewModel.startCleanerScan()
            }
        )
    }
}

@Composable
private fun DuplicatesList(
    groups: List<com.example.data.model.DuplicateFileGroup>,
    selectedPaths: Set<String>,
    onTogglePath: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No duplicate files found.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(groups) { group ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Identical Files (${group.files.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = FileItem.formatBytes(group.size),
                                style = TabularStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        group.files.forEachIndexed { index, file ->
                            val isChecked = selectedPaths.contains(file.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onTogglePath(file.path) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = file.path,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (index == 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Original",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeFilesList(
    files: List<FileItem>,
    selectedPaths: Set<String>,
    onTogglePath: (String) -> Unit
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No large files (> 20 MB) detected.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(files) { file ->
                val isChecked = selectedPaths.contains(file.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onTogglePath(file.path) }
                    )
                    FileTypeIcon(
                        category = file.category,
                        isDirectory = false,
                        fileName = file.name,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = file.path,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = file.formattedSize,
                        style = TabularStyle.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFoldersList(
    folders: List<FileItem>,
    selectedPaths: Set<String>,
    onTogglePath: (String) -> Unit
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No empty folders found.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(folders) { folder ->
                val isChecked = selectedPaths.contains(folder.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onTogglePath(folder.path) }
                    )
                    FileTypeIcon(
                        category = FileCategory.FOLDER,
                        isDirectory = true,
                        fileName = folder.name,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = folder.path,
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

@Composable
private fun AppCacheSection(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "System Cache Notice",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Modern Android operating systems restrict third-party apps from directly deleting the private cache of other installed applications without user consent.\n\n" +
                    "To free system-wide app cache, use Android's native Storage Settings manager below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(fallback)
                }
            },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Open System Storage Settings")
        }
    }
}
