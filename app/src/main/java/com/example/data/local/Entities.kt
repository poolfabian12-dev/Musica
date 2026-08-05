package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,
    val userId: String = "guest",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "guest",
    val name: String,
    val coverUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "songId"])
data class PlaylistItemEntity(
    val playlistId: String,
    val songId: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "suggestions")
data class SuggestionEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "guest",
    val userEmail: String = "usuario@cristiano.org",
    val title: String,
    val artist: String,
    val ministry: String = "",
    val reason: String = "",
    val status: String = "PENDIENTE", // PENDIENTE, APROBADA, DESCARTADA
    val createdAt: Long = System.currentTimeMillis(),
    val emailSentToAdmin: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val date: Long = System.currentTimeMillis(),
    val type: String = "NUEVA_CANCION",
    val songId: String? = null,
    val isRead: Boolean = false
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val name: String,
    val role: String = "user", // "admin" or "user"
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val notificationsEnabled: Boolean = true
)
