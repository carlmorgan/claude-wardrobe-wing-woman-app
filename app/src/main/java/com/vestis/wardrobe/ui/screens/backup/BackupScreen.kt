package com.vestis.wardrobe.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.backup.BackupManager
import com.vestis.wardrobe.data.repository.WardrobeRepository
import com.vestis.wardrobe.util.Hemisphere
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Working : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

data class BackupUiState(
    val itemCount: Int = 0,
    val outfitCount: Int = 0,
    val status: BackupStatus = BackupStatus.Idle
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: WardrobeRepository
) : ViewModel() {

    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val uiState: StateFlow<BackupUiState> = combine(repo.items, repo.outfits, _status) { items, outfits, status ->
        BackupUiState(items.size, outfits.size, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    fun export(uri: Uri, hemisphere: Hemisphere) = viewModelScope.launch {
        _status.value = BackupStatus.Working
        runCatching {
            val items = repo.getAllItems()
            val outfits = repo.getAllOutfits()
            withContext(Dispatchers.IO) {
                BackupManager.export(context, items, outfits, hemisphere.name, uri)
            }
        }.onSuccess {
            _status.value = BackupStatus.Success("Backup exported successfully")
        }.onFailure {
            _status.value = BackupStatus.Error("Export failed: ${it.message}")
        }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        _status.value = BackupStatus.Working
        runCatching {
            val photoDir = File(context.filesDir, "photos")
            val result = withContext(Dispatchers.IO) {
                BackupManager.import(context, uri, photoDir)
            }
            repo.replaceAll(result.items, result.outfits)
            result
        }.onSuccess { result ->
            _status.value = BackupStatus.Success(
                "Imported ${result.items.size} items and ${result.outfits.size} outfits"
            )
        }.onFailure {
            _status.value = BackupStatus.Error("Import failed: ${it.message}")
        }
    }

    fun clearStatus() { _status.value = BackupStatus.Idle }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(hemisphere: Hemisphere, vm: BackupViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }

    // SAF launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { vm.export(it, hemisphere) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Backup & Restore", fontWeight = FontWeight.Medium) })
    }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Status banner
            when (val status = state.status) {
                is BackupStatus.Success -> StatusBanner(status.message, false) { vm.clearStatus() }
                is BackupStatus.Error   -> StatusBanner(status.message, true)  { vm.clearStatus() }
                is BackupStatus.Working -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp))
                else -> Unit
            }

            // Summary card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Data Summary", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                    listOf(
                        "Wardrobe items" to state.itemCount.toString(),
                        "Outfits recorded" to state.outfitCount.toString(),
                        "Hemisphere" to if (hemisphere == Hemisphere.SOUTH) "Southern" else "Northern"
                    ).forEach { (label, value) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                            Text(value, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                        }
                        if (label != "Hemisphere") HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Export
            Button(
                onClick = {
                    exportLauncher.launch("vestis-backup-${java.time.LocalDate.now()}.zip")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status !is BackupStatus.Working
            ) {
                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export Backup (ZIP)")
            }

            Spacer(Modifier.height(8.dp))

            // Import
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status !is BackupStatus.Working
            ) {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import Backup")
            }

            Spacer(Modifier.height(20.dp))

            // Archive format info
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Open Archive Format", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        "Backups use standard ZIP with JSON files (schema vestis:wardrobe:v1). " +
                        "Fully portable and readable by any third-party tool.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    listOf(
                        "manifest.json" to "Schema ID, version, export date",
                        "items.json"    to "Full wardrobe catalogue",
                        "outfits.json"  to "Outfit history + item references",
                        "photos/"       to "Item photos (PNG/JPEG)"
                    ).forEach { (file, desc) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(file,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.width(110.dp))
                            Text(desc, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "VESTIS Wardrobe Manager · v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Confirm import (destructive)
    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("Replace all data?") },
            text = { Text("Importing a backup will replace your current wardrobe and outfit history. Export a backup first if you want to keep your data.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.import(uri); showImportConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Import & Replace") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}
