package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // Songs
    @Query("SELECT * FROM songs ORDER BY timestamp DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 ORDER BY title ASC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE genre = :genre")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR ministry LIKE '%' || :query || '%' OR lyrics LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    @Query("UPDATE songs SET playsCount = playsCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs SET isDownloaded = :isDownloaded, localFilePath = :localPath WHERE id = :songId")
    suspend fun updateDownloadState(songId: String, isDownloaded: Boolean, localPath: String)

    // Favorites
    @Query("SELECT songId FROM favorites WHERE userId = :userId")
    fun getFavoriteSongIds(userId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId AND userId = :userId)")
    fun isFavorite(songId: String, userId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId AND userId = :userId")
    suspend fun removeFavorite(songId: String, userId: String)

    // Playlists
    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserPlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removePlaylistItem(playlistId: String, songId: String)

    @Query("SELECT songId FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getPlaylistSongIds(playlistId: String): Flow<List<String>>

    // Suggestions
    @Query("SELECT * FROM suggestions ORDER BY createdAt DESC")
    fun getAllSuggestions(): Flow<List<SuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: SuggestionEntity)

    @Query("UPDATE suggestions SET status = :status WHERE id = :id")
    suspend fun updateSuggestionStatus(id: String, status: String)

    @Query("DELETE FROM suggestions WHERE id = :id")
    suspend fun deleteSuggestion(id: String)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    // Users
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET role = :role WHERE uid = :uid")
    suspend fun updateUserRole(uid: String, role: String)
}
