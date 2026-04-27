package com.vestis.wardrobe.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "outfits")
@Serializable
data class Outfit(
    @PrimaryKey val id: String,
    val date: String,           // ISO-8601 yyyy-MM-dd
    val itemIds: String,        // JSON array stored as string e.g. ["id1","id2"]
    val notes: String = "",
    val season: String          // Summer / Autumn / Winter / Spring
)

/** In-memory view joining Outfit with its resolved WardrobeItems */
data class OutfitWithItems(
    val outfit: Outfit,
    val items: List<WardrobeItem>
)
