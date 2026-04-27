package com.vestis.wardrobe.ui.screens.outfit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vestis.wardrobe.data.model.Category
import com.vestis.wardrobe.util.Hemisphere
import com.vestis.wardrobe.util.SeasonUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitScreen(hemisphere: Hemisphere, vm: OutfitViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val season = SeasonUtil.getSeason(state.date, hemisphere)
    var showDatePicker by remember { mutableStateOf(false) }

    if (state.saved) {
        SavedConfirmation(season = season.label, onDone = { vm.resetSaved() })
        return
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Record Outfit", fontWeight = FontWeight.Medium) })
    }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Date selector
            Spacer(Modifier.height(8.dp))
            Text("Date", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(state.date.format(DATE_FMT), style = MaterialTheme.typography.bodyLarge)
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${season.label} · ${if (hemisphere == Hemisphere.SOUTH) "S" else "N"}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            // Category filter
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.all) { cat ->
                    FilterChip(
                        selected = state.filterCategory == cat,
                        onClick = { vm.setCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // Selection hint
            Spacer(Modifier.height(8.dp))
            val selCount = state.selectedIds.size
            Text(
                if (selCount > 0) "$selCount item${if (selCount != 1) "s" else ""} selected"
                else "Tap items to add to outfit",
                style = MaterialTheme.typography.bodySmall,
                color = if (selCount > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )

            // Items grid (fixed height avoids nested scroll issues)
            Spacer(Modifier.height(8.dp))
            val gridRows = (state.filtered.size + 2) / 3
            val gridHeight = (gridRows * 90 + (gridRows - 1) * 8).coerceAtLeast(90).dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(state.filtered, key = { it.id }) { item ->
                    val selected = item.id in state.selectedIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.toggleItem(item.id) },
                        border = if (selected)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(item.emoji, fontSize = 24.sp)
                            Text(
                                item.name.let { if (it.length > 12) it.take(11) + "…" else it },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Notes
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes (occasion, event…)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.saveOutfit(hemisphere) },
                enabled = state.selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.selectedIds.isNotEmpty())
                        "Save Outfit — ${state.selectedIds.size} item${if (state.selectedIds.size != 1) "s" else ""}"
                    else "Select at least 1 item"
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        vm.setDate(LocalDate.ofEpochDay(ms / 86_400_000L))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun SavedConfirmation(season: String, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Outfit recorded!", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("Season: $season", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onDone) { Text("Record another") }
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2_500)
        onDone()
    }
}
