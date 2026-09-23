package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SalimMainViewModel
import com.example.ui.components.RootDisclosureDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SalimMainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by viewModel.theme.collectAsState()
    val showHidden by viewModel.showHidden.collectAsState()
    val confirmDelete by viewModel.confirmDelete.collectAsState()
    val retentionDays by viewModel.trashRetentionDays.collectAsState()
    val enableRoot by viewModel.enableRoot.collectAsState()

    var showRootDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader("Appearance")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ThemeOption(label = "System Default", selected = theme == "SYSTEM", onSelect = {
                            scope.launch { viewModel.prefsRepo.setTheme("SYSTEM") }
                        })
                        ThemeOption(label = "Light (Off-white)", selected = theme == "LIGHT", onSelect = {
                            scope.launch { viewModel.prefsRepo.setTheme("LIGHT") }
                        })
                        ThemeOption(label = "Dark (True OLED Black)", selected = theme == "DARK", onSelect = {
                            scope.launch { viewModel.prefsRepo.setTheme("DARK") }
                        })
                    }
                }
            }

            item {
                val density by viewModel.rowDensity.collectAsState()
                SectionHeader("Layout Density")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ThemeOption(label = "Compact (Dense list)", selected = density == "COMPACT", onSelect = {
                            scope.launch { viewModel.prefsRepo.setRowDensity("COMPACT") }
                        })
                        ThemeOption(label = "Standard (Balanced)", selected = density == "STANDARD", onSelect = {
                            scope.launch { viewModel.prefsRepo.setRowDensity("STANDARD") }
                        })
                        ThemeOption(label = "Spacious (Large touch targets)", selected = density == "SPACIOUS", onSelect = {
                            scope.launch { viewModel.prefsRepo.setRowDensity("SPACIOUS") }
                        })
                    }
                }
            }

            item {
                SectionHeader("Files & Browsing")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ToggleRow(
                            label = "Show Hidden Files",
                            description = "Show dotfiles and hidden folders",
                            checked = showHidden,
                            onCheckedChange = { scope.launch { viewModel.prefsRepo.setShowHidden(it) } }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ToggleRow(
                            label = "Confirm Deletions",
                            description = "Ask for confirmation before moving to trash",
                            checked = confirmDelete,
                            onCheckedChange = { scope.launch { viewModel.prefsRepo.setConfirmDelete(it) } }
                        )
                    }
                }
            }

            item {
                SectionHeader("Trash Retention")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val options = listOf(7 to "7 days", 14 to "14 days", 30 to "30 days", 0 to "Never (Manual)")
                        options.forEach { (days, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { scope.launch { viewModel.prefsRepo.setTrashRetentionDays(days) } }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = retentionDays == days,
                                    onClick = { scope.launch { viewModel.prefsRepo.setTrashRetentionDays(days) } }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Advanced & Security")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ToggleRow(
                            label = "Root Access",
                            description = "Enable su binary shell execution for system folders",
                            checked = enableRoot,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showRootDialog = true
                                } else {
                                    scope.launch { viewModel.prefsRepo.setEnableRoot(false) }
                                }
                            }
                        )
                    }
                }
            }

            item {
                SectionHeader("About Salim")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Salim File Manager", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.0 • Pure Native Android", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Architected with Apple motion physics and Vercel visual restraint. " +
                                    "Zero mock data, zero tracking, 100% on-device local execution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showRootDialog) {
        RootDisclosureDialog(
            onDismiss = { showRootDialog = false },
            onAccept = {
                showRootDialog = false
                scope.launch {
                    val rootExists = viewModel.fileRepo.checkRootAccess()
                    if (rootExists) {
                        viewModel.prefsRepo.setEnableRoot(true)
                        Toast.makeText(context, "Root access enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No root / su binary detected on device", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
