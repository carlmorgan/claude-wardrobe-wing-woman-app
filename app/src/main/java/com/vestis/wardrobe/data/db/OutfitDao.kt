package com.vestis.wardrobe.data.db

import androidx.room.*
import com.vestis.wardrobe.data.model.Outfit
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {

    @Query("SELECT * FROM outfits ORDER BY date DESC")
    fun observeAll(): Flow<List<Outfit>>

    @Query("SELECT * FROM outfits ORDER BY date DESC")
    suspend fun getAll(): List<Outfit>

    @Query("SELECT * FROM outfits WHERE season = :season ORDER BY date DESC")
    suspend fun getBySeason(season: String): List<Outfit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outfit: Outfit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(outfits: List<Outfit>)

    @Delete
    suspend fun delete(outfit: Outfit)

    @Query("DELETE FROM outfits")
    suspend fun deleteAll()
}
