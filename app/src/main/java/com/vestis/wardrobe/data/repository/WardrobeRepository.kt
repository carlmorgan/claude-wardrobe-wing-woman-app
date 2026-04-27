package com.vestis.wardrobe.data.repository

import com.vestis.wardrobe.data.db.OutfitDao
import com.vestis.wardrobe.data.db.WardrobeItemDao
import com.vestis.wardrobe.data.model.Outfit
import com.vestis.wardrobe.data.model.OutfitWithItems
import com.vestis.wardrobe.data.model.WardrobeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class WardrobeRepository @Inject constructor(
    private val itemDao: WardrobeItemDao,
    private val outfitDao: OutfitDao
) {

    // ─── Items ──────────────────────────────────────────────────

    val items: Flow<List<WardrobeItem>> = itemDao.observeAll()

    suspend fun getAllItems(): List<WardrobeItem> = itemDao.getAll()

    suspend fun saveItem(item: WardrobeItem) = itemDao.insert(item)

    suspend fun deleteItem(item: WardrobeItem) = itemDao.delete(item)

    // ─── Outfits ─────────────────────────────────────────────────

    val outfits: Flow<List<Outfit>> = outfitDao.observeAll()

    /** Emits OutfitWithItems joined in-memory, re-emitting on any change */
    val outfitsWithItems: Flow<List<OutfitWithItems>> =
        combine(itemDao.observeAll(), outfitDao.observeAll()) { allItems, allOutfits ->
            val itemMap = allItems.associateBy { it.id }
            allOutfits.map { outfit ->
                val ids = json.decodeFromString<List<String>>(outfit.itemIds)
                OutfitWithItems(outfit, ids.mapNotNull { itemMap[it] })
            }
        }

    suspend fun saveOutfit(outfit: Outfit) = outfitDao.insert(outfit)

    suspend fun deleteOutfit(outfit: Outfit) = outfitDao.delete(outfit)

    suspend fun getAllOutfits(): List<Outfit> = outfitDao.getAll()

    // ─── Backup import helpers ────────────────────────────────────

    suspend fun replaceAll(items: List<WardrobeItem>, outfits: List<Outfit>) {
        itemDao.deleteAll()
        outfitDao.deleteAll()
        itemDao.insertAll(items)
        outfitDao.insertAll(outfits)
    }

    // ─── Usage counts (item id → times worn) ─────────────────────

    fun usageCounts(outfitList: List<Outfit>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        outfitList.forEach { o ->
            val ids = json.decodeFromString<List<String>>(o.itemIds)
            ids.forEach { map[it] = (map[it] ?: 0) + 1 }
        }
        return map
    }
}
