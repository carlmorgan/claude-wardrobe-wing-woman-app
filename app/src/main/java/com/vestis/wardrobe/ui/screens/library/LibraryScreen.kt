package com.vestis.wardrobe.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vestis.wardrobe.data.model.Category
import com.vestis.wardrobe.data.model.WardrobeItem

private val EMOJIS = listOf(
    "👕","👔","🧥","👖","👗","🩳","🧣","🧤","👟","👠","👡","👢",
    "🥾","👒","🎩","🧢","👜","👛","🎒","💍","💎","⌚","🕶️","🩴","🩱"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<WardrobeItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wardrobe", fontWeight = FontWeight.Medium) },
                actions = {
                    IconButton(onClick = { editingItem = null; showSheet = true }) {
                        Icon(Icons.Default.Add, "Add item")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search wardrobe…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Category.all) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { vm.setCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // Summary row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Items" to state.items.size,
                    "Shown" to state.filtered.size,
                    "Unworn" to state.unwornCount
                ).forEach { (label, count) ->
                    Card(Modifier.weight(1f)) {
                        Column(
                            Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Grid
            if (state.filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filtered, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            wornCount = state.usageCounts[item.id] ?: 0,
                            onClick = { editingItem = item; showSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ItemSheet(
            item = editingItem,
            wornCount = editingItem?.let { state.usageCounts[it.id] ?: 0 } ?: 0,
            onSave = { item -> vm.saveItem(item); showSheet = false },
            onDelete = { item -> vm.deleteItem(item); showSheet = false },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
fun ItemCard(item: WardrobeItem, wornCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.emoji, fontSize = 36.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                item.brand.ifBlank { item.category },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(item.category, style = MaterialTheme.typography.labelSmall) }
                )
                Text(
                    if (wornCount > 0) "${wornCount}×" else "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (wornCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(
    item: WardrobeItem?,
    wornCount: Int,
    onSave: (WardrobeItem) -> Unit,
    onDelete: (WardrobeItem) -> Unit,
    onDismiss: () -> Unit,
    vm: LibraryViewModel = hiltViewModel()
) {
    var emoji by remember { mutableStateOf(item?.emoji ?: "👕") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Tops") }
    var color by remember { mutableStateOf(item?.color ?: "") }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Emoji picker trigger
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        emoji,
                        fontSize = 52.sp,
                        modifier = Modifier.clickable { showEmojiPicker = !showEmojiPicker }
                    )
                    Text("tap to change", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            if (showEmojiPicker) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = 8.dp)
                ) {
                    items(EMOJIS) { e ->
                        Text(
                            e, fontSize = 22.sp,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { emoji = e; showEmojiPicker = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)) {
                items(Category.all.drop(1)) { cat ->
                    FilterChip(selected = category == cat,
                        onClick = { category = cat }, label = { Text(cat) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = color, onValueChange = { color = it },
                    label = { Text("Colour") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = brand, onValueChange = { brand = it },
                    label = { Text("Brand") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            if (item != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Worn $wornCount time${if (wornCount != 1) "s" else ""} · Added ${item.dateAdded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item != null) {
                    OutlinedButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val updated = item?.copy(
                                emoji = emoji, name = name, category = category,
                                color = color, brand = brand, notes = notes
                            ) ?: vm.newItem(emoji, name, category, color, brand, notes)
                            onSave(updated)
                        }
                    },
                    modifier = Modifier.weight(if (item != null) 1f else Float.MAX_VALUE),
                    enabled = name.isNotBlank()
                ) { Text(if (item != null) "Save" else "Add to Wardrobe") }
            }
        }
    }
}
