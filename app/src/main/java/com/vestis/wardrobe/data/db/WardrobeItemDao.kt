package com.vestis.wardrobe.data.db

import androidx.room.*
import com.vestis.wardrobe.data.model.WardrobeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeItemDao {

    @Query("SELECT * FROM wardrobe_items ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM wardrobe_items ORDER BY dateAdded DESC")
    suspend fun getAll(): List<WardrobeItem>

    @Query("SELECT * FROM wardrobe_items WHERE id = :id")
    suspend fun getById(id: String): WardrobeItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WardrobeItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WardrobeItem>)

    @Update
    suspend fun update(item: WardrobeItem)

    @Delete
    suspend fun delete(item: WardrobeItem)

    @Query("DELETE FROM wardrobe_items")
    suspend fun deleteAll()
}
