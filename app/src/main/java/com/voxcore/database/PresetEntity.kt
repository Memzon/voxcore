package com.voxcore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** `json`, PresetCodec'in ürettiği tam ön ayar belgesidir (dışa aktarılan dosyayla aynı). */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val json: String,
    val createdAt: Long,
)
