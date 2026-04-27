package com.vestis.wardrobe.ui.screens.outfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.model.Outfit
import com.vestis.wardrobe.data.model.WardrobeItem
import com.vestis.wardrobe.data.repository.WardrobeRepository
import com.vestis.wardrobe.util.Hemisphere
import com.vestis.wardrobe.util.SeasonUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class OutfitUiState(
    val allItems: List<WardrobeItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val filterCategory: String = "All",
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val saved: Boolean = false
) {
    val filtered get() = if (filterCategory == "All") allItems
        else allItems.filter { it.category == filterCategory }
}

@HiltViewModel
class OutfitViewModel @Inject constructor(private val repo: WardrobeRepository) : ViewModel() {

    private val _state = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.items.collect { _state.update { s -> s.copy(allItems = it) } }
        }
    }

    fun toggleItem(id: String) = _state.update { s ->
        val sel = s.selectedIds.toMutableSet()
        if (sel.contains(id)) sel.remove(id) else sel.add(id)
        s.copy(selectedIds = sel)
    }

    fun setCategory(c: String) = _state.update { it.copy(filterCategory = c) }
    fun setDate(d: LocalDate) = _state.update { it.copy(date = d) }
    fun setNotes(n: String) = _state.update { it.copy(notes = n) }

    fun saveOutfit(hemisphere: Hemisphere) = viewModelScope.launch {
        val s = _state.value
        if (s.selectedIds.isEmpty()) return@launch
        val season = SeasonUtil.getSeason(s.date, hemisphere)
        val outfit = Outfit(
            id = UUID.randomUUID().toString(),
            date = s.date.toString(),
            itemIds = Json.encodeToString(s.selectedIds.toList()),
            notes = s.notes.trim(),
            season = season.label
        )
        repo.saveOutfit(outfit)
        _state.update { it.copy(selectedIds = emptySet(), notes = "", saved = true) }
    }

    fun resetSaved() = _state.update { it.copy(saved = false) }
}
