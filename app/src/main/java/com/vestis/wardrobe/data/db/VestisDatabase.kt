package com.vestis.wardrobe.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vestis.wardrobe.data.model.Outfit
import com.vestis.wardrobe.data.model.WardrobeItem

@Database(
    entities = [WardrobeItem::class, Outfit::class],
    version = 1,
    exportSchema = false
)
abstract class VestisDatabase : RoomDatabase() {
    abstract fun wardrobeItemDao(): WardrobeItemDao
    abstract fun outfitDao(): OutfitDao
}
