package com.voxcore.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PresetEntity::class], version = 1, exportSchema = false)
abstract class VoxDatabase : RoomDatabase() {
    abstract fun presets(): PresetDao

    companion object {
        @Volatile
        private var instance: VoxDatabase? = null

        fun get(ctx: Context): VoxDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(ctx.applicationContext, VoxDatabase::class.java, "voxcore.db")
                    .build().also { instance = it }
            }
    }
}
