package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.theme.TabularStyle
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CreateItemDialog(
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(if (isFolder) "New Folder" else "untitled.txt") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFolder) "Create Folder" else "Create File",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isFolder) "Folder Name" else "File Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_item_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_item_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_item_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName != currentName) onConfirm(newName.trim())
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeleteConfirmDialog(
    targetName: String,
    count: Int,
    confirmPermanent: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (permanent: Boolean) -> Unit
) {
    var permanent by remember { mutableStateOf(confirmPermanent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (permanent) "Delete Permanently" else "Move to Trash",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column {
                val label = if (count > 1) "$count items" else "\"$targetName\""
                Text(
                    text = if (permanent) {
                        "Are you sure you want to permanently delete $label? This action cannot be undone."
                    } else {
                        "Move $label to Trash? You can restore it anytime before the retention period expires."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { permanent = !permanent }
                ) {
                    Checkbox(
                        checked = permanent,
                        onCheckedChange = { permanent = it },
                        modifier = Modifier.testTag("permanent_delete_checkbox"),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                    )
                    Text(
                        text = "Delete permanently (bypass Trash)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (permanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(permanent) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (permanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("delete_dialog_confirm_button")
            ) {
                Text(if (permanent) "Delete" else "Move to Trash")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("delete_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun BatchRenameDialog(
    selectedFiles: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirm: (prefix: String, suffix: String, find: String, replace: String, numbering: Int?, uppercase: Boolean?) -> Unit
) {
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var useNumbering by remember { mutableStateOf(false) }
    var startNumber by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batch Rename (${selectedFiles.size} items)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefix") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = { suffix = it },
                        label = { Text("Suffix") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        label = { Text("Find") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("Replace") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = useNumbering,
                        onCheckedChange = { useNumbering = it }
                    )
                    Text("Add Sequence Numbering", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Preview
                Text(
                    text = "Preview (First item):",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                val sample = selectedFiles.firstOrNull()
                val samplePreview = remember(sample, prefix, suffix, findText, replaceText, useNumbering, startNumber) {
                    if (sample == null) "" else {
                        var base = sample.name.substringBeforeLast('.', sample.name)
                        val ext = if (sample.name.contains('.')) ".${sample.name.substringAfterLast('.')}" else ""
                        if (findText.isNotEmpty()) base = base.replace(findText, replaceText)
                        if (useNumbering) base = "$base-${startNumber.toIntOrNull() ?: 1}"
                        "$prefix$base$suffix$ext"
                    }
                }

                Text(
                    text = samplePreview,
                    style = TabularStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        prefix,
                        suffix,
                        findText,
                        replaceText,
                        if (useNumbering) startNumber.toIntOrNull() ?: 1 else null,
                        null
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("batch_rename_apply_button")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesBottomSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onCalculateHash: suspend (String, String) -> String,
    onColorFolder: (String, String?) -> Unit,
    onSetLabel: (String, String?) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var hashSha256 by remember { mutableStateOf<String?>(null) }
    var hashMd5 by remember { mutableStateOf<String?>(null) }
    var isCalculatingHash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileTypeIcon(
                    category = fileItem.category,
                    isDirectory = fileItem.isDirectory,
                    fileName = fileItem.name,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (fileItem.isDirectory) "${fileItem.itemCount} items" else fileItem.formattedSize,
                        style = TabularStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PropertyRow(label = "Location", value = fileItem.path, onCopy = { clipboardManager.setText(AnnotatedString(fileItem.path)) })
            PropertyRow(label = "Modified", value = fileItem.formattedDate)
            PropertyRow(label = "Type", value = if (fileItem.isDirectory) "Folder" else fileItem.mimeType.ifEmpty { fileItem.extension })

            if (!fileItem.isDirectory) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checksums",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    if (hashSha256 == null && !isCalculatingHash) {
                        OutlinedButton(
                            onClick = {
                                isCalculatingHash = true
                                scope.launch {
                                    hashSha256 = onCalculateHash(fileItem.path, "SHA-256")
                                    hashMd5 = onCalculateHash(fileItem.path, "MD5")
                                    isCalculatingHash = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Compute Hash")
                        }
                    }
                }

                if (isCalculatingHash) {
                    Text("Computing MD5 & SHA-256...", style = MaterialTheme.typography.bodySmall)
                } else if (hashSha256 != null) {
                    PropertyRow(label = "MD5", value = hashMd5 ?: "", onCopy = { hashMd5?.let { clipboardManager.setText(AnnotatedString(it)) } })
                    PropertyRow(label = "SHA-256", value = hashSha256 ?: "", onCopy = { hashSha256?.let { clipboardManager.setText(AnnotatedString(it)) } })
                }
            } else {
                // Folder Color picker
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Folder Color Badge",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val colors = listOf(
                    null to "Default",
                    "#0071E3" to "Blue",
                    "#34C759" to "Green",
                    "#FF9500" to "Orange",
                    "#FF3B30" to "Red",
                    "#AF52DE" to "Purple"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { (hex, _) ->
                        val isSelected = fileItem.folderColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { onColorFolder(fileItem.path, hex) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(90.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = TabularStyle.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2
            )

            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RootDisclosureDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Root Access Disclosure",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Enabling root access grants Salim the ability to execute commands via the 'su' binary. " +
                            "This allows reading and modifying system-protected directories (/data, /system, /root).\n\n" +
                            "Warning: Modifying system files can cause device instability or data loss. " +
                            "Salim never performs root commands without explicit user action.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Enable Root")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ShredConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Shred File Permanently?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "DoD 5220.22-M Forensic Overwrite:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The contents of '$itemName' will be overwritten 3 times (random cryptographic bytes, 0xFF, and 0x00), flushed to hardware, renamed, and destroyed. " +
                            "This data cannot be recovered by any file recovery software.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_shred_button")
            ) {
                Text("Shred File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TagSelectionDialog(
    currentColorHex: String?,
    onDismiss: () -> Unit,
    onSelectColor: (String?) -> Unit
) {
    val tags = listOf(
        null to "None",
        "#FF3B30" to "Red",
        "#FF9500" to "Orange",
        "#FFCC00" to "Yellow",
        "#34C759" to "Green",
        "#0071E3" to "Blue",
        "#AF52DE" to "Purple",
        "#8E8E93" to "Slate"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Finder Tag",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { (hex, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectColor(hex) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hex != null) Color(android.graphics.Color.parseColor(hex))
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (currentColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (currentColorHex == hex) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
