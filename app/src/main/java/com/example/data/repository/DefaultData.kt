package com.example.data.repository

import com.example.data.local.SongEntity
import com.example.data.local.UserEntity

object DefaultData {

    val ADMIN_USER = UserEntity(
        uid = "admin_01",
        email = "poolfabian12@gmail.com",
        name = "Administrador Principal",
        role = "admin"
    )

    val SAMPLE_SONGS = emptyList<SongEntity>()
}
