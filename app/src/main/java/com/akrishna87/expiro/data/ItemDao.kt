package com.akrishna87.expiro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE available = 1 ORDER BY expiryEpochDay ASC, name ASC")
    fun observeAvailable(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE available = 0 ORDER BY expiryEpochDay DESC, name ASC")
    fun observeHistory(): Flow<List<Item>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM items WHERE available = 1")
    suspend fun getAvailable(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("UPDATE items SET available = :available WHERE id = :id")
    suspend fun setAvailable(id: Long, available: Boolean)

    @Query("UPDATE items SET notifiedStage = :stage WHERE id = :id")
    suspend fun setNotifiedStage(id: Long, stage: Int)
}
