package com.vestis.wardrobe.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.model.WardrobeItem
import com.vestis.wardrobe.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReportsUiState(
    val totalItems: Int = 0,
    val totalOutfits: Int = 0,
    val unwornItems: List<WardrobeItem> = emptyList(),
    val topWorn: List<Pair<WardrobeItem, Int>> = emptyList(),
    val seasonCounts: Map<String, Int> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class ReportsViewModel @Inject constructor(repo: WardrobeRepository) : ViewModel() {
    val uiState: StateFlow<ReportsUiState> = combine(
        repo.items, repo.outfits
    ) { items, outfits ->
        val counts = repo.usageCounts(outfits)
        val unworn = items.filter { (counts[it.id] ?: 0) == 0 }
        val top = items
            .filter { (counts[it.id] ?: 0) > 0 }
            .sortedByDescending { counts[it.id] }
            .take(5)
            .map { it to (counts[it.id] ?: 0) }
        val seasons = outfits.groupBy { it.season }.mapValues { it.value.size }
        val categories = items.groupBy { it.category }.mapValues { it.value.size }
        ReportsUiState(items.size, outfits.size, unworn, top, seasons, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}

private val seasonColors = mapOf(
    "Summer" to Color(0xFFE07B4F), "Autumn" to Color(0xFFC4773B),
    "Winter" to Color(0xFF6B9BBF), "Spring" to Color(0xFF72B572)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel = hiltViewModel()) {
    val s by vm.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Reports", fontWeight = FontWeight.Medium) })
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Items" to s.totalItems,
                        "Outfits" to s.totalOutfits,
                        "Unworn" to s.unwornItems.size,
                        "Active" to (s.totalItems - s.unwornItems.size)
                    ).forEach { (label, count) ->
                        Card(Modifier.weight(1f)) {
                            Column(
                                Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Most worn
            if (s.topWorn.isNotEmpty()) {
                item {
                    SectionTitle("Most Worn")
                    val max = s.topWorn.maxOf { it.second }.toFloat()
                    s.topWorn.forEach { (item, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(item.emoji, fontSize = 20.sp, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${count}×",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { count / max },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Never worn
            if (s.unwornItems.isNotEmpty()) {
                item {
                    SectionTitle("Never Worn (${s.unwornItems.size})")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.unwornItems) { item ->
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

            // Outfits by season
            item {
                SectionTitle("Outfits by Season")
                val maxSeason = s.seasonCounts.values.maxOrNull()?.toFloat() ?: 1f
                listOf("Summer","Autumn","Winter","Spring").forEach { season ->
                    val count = s.seasonCounts[season] ?: 0
                    val color = seasonColors[season] ?: MaterialTheme.colorScheme.primary
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(season, style = MaterialTheme.typography.bodySmall,
                            color = color, modifier = Modifier.width(52.dp))
                        LinearProgressIndicator(
                            progress = { if (count > 0) count / maxSeason else 0f },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "$count", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(20.dp)
                        )
                    }
                }
            }

            // By category
            item {
                SectionTitle("By Category")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tops","Bottoms","Dresses","Outerwear").forEach { cat ->
                            CategoryStatCard(cat, s.categoryCounts[cat] ?: 0)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Shoes","Bags","Accessories").forEach { cat ->
                            CategoryStatCard(cat, s.categoryCounts[cat] ?: 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CategoryStatCard(category: String, count: Int) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f))
            Text("$count", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
