package com.voxcore.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: PresetEntity): Long

    @Delete
    suspend fun delete(p: PresetEntity)
}
