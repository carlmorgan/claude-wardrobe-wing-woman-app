package com.vestis.wardrobe.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vestis.wardrobe.data.repository.PrefsRepository
import com.vestis.wardrobe.util.Hemisphere
import com.vestis.wardrobe.util.LocationUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository
) : ViewModel() {

    val hemisphere = prefs.hemisphere.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        Hemisphere.SOUTH
    )

    /** Called once after location permission is granted */
    fun detectAndSaveHemisphere() {
        viewModelScope.launch {
            val h = LocationUtil.detectHemisphere(context)
            prefs.saveHemisphere(h)
        }
    }

    fun setHemisphere(h: Hemisphere) {
        viewModelScope.launch { prefs.saveHemisphere(h) }
    }
}
