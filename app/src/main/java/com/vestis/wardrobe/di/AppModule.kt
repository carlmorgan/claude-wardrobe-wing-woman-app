package com.vestis.wardrobe.di

import android.content.Context
import androidx.room.Room
import com.vestis.wardrobe.data.db.OutfitDao
import com.vestis.wardrobe.data.db.VestisDatabase
import com.vestis.wardrobe.data.db.WardrobeItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): VestisDatabase =
        Room.databaseBuilder(ctx, VestisDatabase::class.java, "vestis.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideItemDao(db: VestisDatabase): WardrobeItemDao = db.wardrobeItemDao()

    @Provides
    fun provideOutfitDao(db: VestisDatabase): OutfitDao = db.outfitDao()
}
