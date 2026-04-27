package com.vestis.wardrobe.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vestis.wardrobe.data.model.OutfitWithItems
import com.vestis.wardrobe.util.Hemisphere
import com.vestis.wardrobe.util.Season
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy")
private val seasonColors = mapOf(
    "Summer" to Color(0xFFE07B4F),
    "Autumn" to Color(0xFFC4773B),
    "Winter" to Color(0xFF6B9BBF),
    "Spring" to Color(0xFF72B572)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(hemisphere: Hemisphere, vm: HistoryViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Outfit History", fontWeight = FontWeight.Medium) })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            // Season filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Season.all) { s ->
                    FilterChip(
                        selected = state.seasonFilter == s,
                        onClick = { vm.setFilter(s) },
                        label = { Text(s) }
                    )
                }
            }

            if (state.filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No outfits recorded yet", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.grouped.forEach { (season, outfits) ->
                        if (state.seasonFilter == "All") {
                            item(key = "header_$season") {
                                SeasonHeader(season, outfits.size)
                            }
                        }
                        items(outfits, key = { it.outfit.id }) { owi ->
                            OutfitCard(owi, onDelete = { vm.delete(owi) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonHeader(season: String, count: Int) {
    val color = seasonColors[season] ?: MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(season, color = color, fontWeight = FontWeight.SemiBold)
        Text("· $count", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun OutfitCard(owi: OutfitWithItems, onDelete: () -> Unit) {
    val seasonColor = seasonColors[owi.outfit.season] ?: MaterialTheme.colorScheme.primary
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        LocalDate.parse(owi.outfit.date).format(DATE_FMT),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (owi.outfit.notes.isNotBlank()) {
                        Text(owi.outfit.notes, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(owi.outfit.season,
                                style = MaterialTheme.typography.labelSmall,
                                color = seasonColor)
                        }
                    )
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Items
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(owi.items) { item ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.emoji, fontSize = 14.sp)
                                Text(item.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete outfit?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
