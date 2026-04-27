package com.vestis.wardrobe.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.model.WardrobeItem
import com.vestis.wardrobe.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class LibraryUiState(
    val items: List<WardrobeItem> = emptyList(),
    val usageCounts: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val selectedCategory: String = "All"
) {
    val filtered: List<WardrobeItem> get() = items.filter { item ->
        (selectedCategory == "All" || item.category == selectedCategory) &&
        (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) ||
         item.brand.contains(searchQuery, ignoreCase = true))
    }
    val unwornCount: Int get() = items.count { (usageCounts[it.id] ?: 0) == 0 }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: WardrobeRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("All")

    val uiState: StateFlow<LibraryUiState> = combine(
        repo.items,
        repo.outfits,
        _query,
        _category
    ) { items, outfits, query, cat ->
        LibraryUiState(
            items = items,
            usageCounts = repo.usageCounts(outfits),
            searchQuery = query,
            selectedCategory = cat
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }

    fun saveItem(item: WardrobeItem) = viewModelScope.launch { repo.saveItem(item) }

    fun deleteItem(item: WardrobeItem) = viewModelScope.launch { repo.deleteItem(item) }

    fun newItem(
        emoji: String, name: String, category: String,
        color: String, brand: String, notes: String, photoPath: String = ""
    ) = WardrobeItem(
        id = UUID.randomUUID().toString(),
        emoji = emoji, name = name, category = category,
        color = color, brand = brand, notes = notes,
        photoPath = photoPath,
        dateAdded = LocalDate.now().toString()
    )
}
