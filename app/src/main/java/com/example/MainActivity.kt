package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.SalimMainViewModel
import com.example.ui.screens.apps.AppManagerScreen
import com.example.ui.screens.archive.ArchiveExplorerScreen
import com.example.ui.screens.browser.FileBrowserScreen
import com.example.ui.screens.cleaner.StorageCleanerScreen
import com.example.ui.screens.diff.FileDiffScreen
import com.example.ui.screens.editor.TextEditorScreen
import com.example.ui.screens.hex.HexInspectorScreen
import com.example.ui.screens.quicklook.QuickLookViewerScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.storage.StorageOverviewScreen
import com.example.ui.screens.trash.TrashScreen
import com.example.ui.screens.vault.EncryptedVaultScreen
import com.example.ui.theme.SalimTheme

enum class SalimScreen {
    BROWSER,
    STORAGE,
    CLEANER,
    SETTINGS,
    TEXT_EDITOR,
    TRASH,
    APPS_MANAGER,
    VAULT,
    ARCHIVE_EXPLORER,
    HEX_INSPECTOR,
    FILE_DIFF,
    QUICK_LOOK
}

class MainActivity : ComponentActivity() {
    private val viewModel: SalimMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by viewModel.theme.collectAsState()
            val useDark = when (themePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SalimTheme(darkTheme = useDark) {
                SalimApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SalimApp(viewModel: SalimMainViewModel) {
    var currentScreen by remember { mutableStateOf(SalimScreen.BROWSER) }
    var activeEditorFilePath by remember { mutableStateOf<String?>(null) }
    var activeQuickLookPath by remember { mutableStateOf<String?>(null) }
    var activeArchivePath by remember { mutableStateOf<String?>(null) }
    var activeHexPath by remember { mutableStateOf<String?>(null) }
    var activeDiffPaths by remember { mutableStateOf<Pair<String, String>?>(null) }
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }

    val requestManageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasStoragePermission = Environment.isExternalStorageManager()
            if (hasStoragePermission) {
                viewModel.refreshCurrentDirectory()
            }
        }
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.all { it }
        if (granted) {
            viewModel.refreshCurrentDirectory()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom navigation bar only for top-level screens
            val isTopLevel = currentScreen in listOf(
                SalimScreen.BROWSER,
                SalimScreen.STORAGE,
                SalimScreen.CLEANER,
                SalimScreen.SETTINGS
            )

            AnimatedVisibility(visible = isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val navItems = listOf(
                        NavEntry(SalimScreen.BROWSER, "Files", Icons.Filled.Folder, Icons.Outlined.Folder),
                        NavEntry(SalimScreen.STORAGE, "Storage", Icons.Filled.Storage, Icons.Outlined.Storage),
                        NavEntry(SalimScreen.CLEANER, "Cleaner", Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices),
                        NavEntry(SalimScreen.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )

                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentScreen = item.screen
                                if (item.screen == SalimScreen.BROWSER) {
                                    viewModel.refreshCurrentDirectory()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_tab_${item.label.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "All Files Access Required",
                                style = MaterialTheme.typography.titleLarge
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "Salim is a native file manager that requires permission to manage files on your storage volume.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
                            Button(onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:com.aistudio.salim.flmgr")
                                    }
                                    requestManageStorageLauncher.launch(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    requestManageStorageLauncher.launch(fallback)
                                }
                            }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            } else {
                when (currentScreen) {
                    SalimScreen.BROWSER -> {
                        FileBrowserScreen(
                            viewModel = viewModel,
                            onOpenFileInEditor = { path ->
                                activeEditorFilePath = path
                                currentScreen = SalimScreen.TEXT_EDITOR
                            },
                            onOpenQuickLook = { path ->
                                activeQuickLookPath = path
                                currentScreen = SalimScreen.QUICK_LOOK
                            },
                            onOpenArchive = { path ->
                                activeArchivePath = path
                                currentScreen = SalimScreen.ARCHIVE_EXPLORER
                            },
                            onOpenHex = { path ->
                                activeHexPath = path
                                currentScreen = SalimScreen.HEX_INSPECTOR
                            },
                            onOpenDiff = { pathA, pathB ->
                                activeDiffPaths = Pair(pathA, pathB)
                                currentScreen = SalimScreen.FILE_DIFF
                            }
                        )
                    }
                    SalimScreen.STORAGE -> {
                        StorageOverviewScreen(
                            viewModel = viewModel,
                            onNavigateToCategory = { cat ->
                                viewModel.setSelectedCategory(cat)
                                currentScreen = SalimScreen.BROWSER
                            },
                            onNavigateToPath = { path ->
                                viewModel.loadDirectory(path)
                                currentScreen = SalimScreen.BROWSER
                            },
                            onOpenCleaner = { currentScreen = SalimScreen.CLEANER },
                            onOpenAppManager = { currentScreen = SalimScreen.APPS_MANAGER },
                            onOpenTrash = { currentScreen = SalimScreen.TRASH },
                            onOpenVault = { currentScreen = SalimScreen.VAULT },
                            onOpenSettings = { currentScreen = SalimScreen.SETTINGS }
                        )
                    }
                    SalimScreen.CLEANER -> {
                        StorageCleanerScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = SalimScreen.STORAGE }
                        )
                    }
                    SalimScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = SalimScreen.BROWSER }
                        )
                    }
                    SalimScreen.TEXT_EDITOR -> {
                        activeEditorFilePath?.let { path ->
                            TextEditorScreen(
                                filePath = path,
                                onBack = {
                                    activeEditorFilePath = null
                                    currentScreen = SalimScreen.BROWSER
                                }
                            )
                        }
                    }
                    SalimScreen.TRASH -> {
                        TrashScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = SalimScreen.STORAGE }
                        )
                    }
                    SalimScreen.APPS_MANAGER -> {
                        AppManagerScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = SalimScreen.STORAGE }
                        )
                    }
                    SalimScreen.VAULT -> {
                        EncryptedVaultScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = SalimScreen.STORAGE }
                        )
                    }
                    SalimScreen.QUICK_LOOK -> {
                        activeQuickLookPath?.let { path ->
                            QuickLookViewerScreen(
                                filePath = path,
                                onBack = {
                                    activeQuickLookPath = null
                                    currentScreen = SalimScreen.BROWSER
                                }
                            )
                        }
                    }
                    SalimScreen.ARCHIVE_EXPLORER -> {
                        activeArchivePath?.let { path ->
                            ArchiveExplorerScreen(
                                zipPath = path,
                                onBack = {
                                    activeArchivePath = null
                                    currentScreen = SalimScreen.BROWSER
                                }
                            )
                        }
                    }
                    SalimScreen.HEX_INSPECTOR -> {
                        activeHexPath?.let { path ->
                            HexInspectorScreen(
                                filePath = path,
                                onBack = {
                                    activeHexPath = null
                                    currentScreen = SalimScreen.BROWSER
                                }
                            )
                        }
                    }
                    SalimScreen.FILE_DIFF -> {
                        activeDiffPaths?.let { (pathA, pathB) ->
                            FileDiffScreen(
                                fileAPath = pathA,
                                fileBPath = pathB,
                                onBack = {
                                    activeDiffPaths = null
                                    currentScreen = SalimScreen.BROWSER
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavEntry(
    val screen: SalimScreen,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)
