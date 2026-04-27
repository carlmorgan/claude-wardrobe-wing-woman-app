package com.vestis.wardrobe.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.model.OutfitWithItems
import com.vestis.wardrobe.data.repository.WardrobeRepository
import com.vestis.wardrobe.util.Season
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val outfitsWithItems: List<OutfitWithItems> = emptyList(),
    val seasonFilter: String = "All"
) {
    val filtered get() = if (seasonFilter == "All") outfitsWithItems
        else outfitsWithItems.filter { it.outfit.season == seasonFilter }

    val grouped: Map<String, List<OutfitWithItems>> get() =
        filtered.groupBy { it.outfit.season }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: WardrobeRepository) : ViewModel() {
    private val _filter = MutableStateFlow("All")
    val uiState: StateFlow<HistoryUiState> = combine(
        repo.outfitsWithItems, _filter
    ) { items, filter -> HistoryUiState(items, filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(s: String) { _filter.value = s }
    fun delete(owi: OutfitWithItems) = viewModelScope.launch { repo.deleteOutfit(owi.outfit) }
}
