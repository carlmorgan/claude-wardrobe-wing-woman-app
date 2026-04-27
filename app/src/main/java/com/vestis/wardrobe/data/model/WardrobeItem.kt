package com.vestis.wardrobe.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "wardrobe_items")
@Serializable
data class WardrobeItem(
    @PrimaryKey val id: String,
    val emoji: String,
    val name: String,
    val category: String,
    val color: String = "",
    val brand: String = "",
    val notes: String = "",
    val photoPath: String = "",        // local file URI for camera/gallery photo
    val dateAdded: String              // ISO-8601 date yyyy-MM-dd
)

enum class Category(val label: String) {
    TOPS("Tops"),
    BOTTOMS("Bottoms"),
    DRESSES("Dresses"),
    OUTERWEAR("Outerwear"),
    SHOES("Shoes"),
    BAGS("Bags"),
    ACCESSORIES("Accessories");

    companion object {
        val all: List<String> = listOf("All") + entries.map { it.label }
    }
}
