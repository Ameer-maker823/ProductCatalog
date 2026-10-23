package com.group4.productcatalog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteProduct)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAll(): Flow<List<FavoriteProduct>>

    @Query("SELECT id FROM favorites")
    fun getIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: Int): Flow<Boolean>
}