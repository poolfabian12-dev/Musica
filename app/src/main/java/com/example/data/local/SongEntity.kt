package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val ministry: String,
    val genre: String,
    val album: String = "",
    val year: Int = 2024,
    val durationSeconds: Int = 210,
    val audioUrl: String,
    val coverUrl: String = "",
    val lyrics: String = "",
    val downloadsCount: Int = 0,
    val playsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true,
    val isDownloaded: Boolean = false,
    val localFilePath: String = ""
)
