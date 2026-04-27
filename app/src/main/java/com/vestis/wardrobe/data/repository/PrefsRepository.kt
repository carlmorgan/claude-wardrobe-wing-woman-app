package com.vestis.wardrobe.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vestis.wardrobe.util.Hemisphere
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "vestis_prefs")

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_HEMISPHERE = stringPreferencesKey("hemisphere")

    val hemisphere: Flow<Hemisphere> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_HEMISPHERE]) {
            "NORTH" -> Hemisphere.NORTH
            else -> Hemisphere.SOUTH
        }
    }

    suspend fun saveHemisphere(h: Hemisphere) {
        context.dataStore.edit { it[KEY_HEMISPHERE] = h.name }
    }
}
