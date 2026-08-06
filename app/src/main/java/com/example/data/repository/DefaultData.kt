package com.example.data.repository

import com.example.data.api.YoutubeAudioConverter
import com.example.data.local.SongEntity
import com.example.data.local.UserEntity

object DefaultData {

    val ADMIN_USER = UserEntity(
        uid = "admin_01",
        email = "poolfabian12@gmail.com",
        name = "Administrador Principal",
        role = "admin"
    )

    val SAMPLE_SONGS = listOf(
        SongEntity(
            id = "song_01",
            title = "Alaba a Dios",
            artist = "Danny Berríos",
            ministry = "Ministerio Danny Berríos",
            genre = "Alabanza y Adoración",
            album = "Dios Cuida de Mí",
            year = 2024,
            durationSeconds = 270,
            audioUrl = YoutubeAudioConverter.DEFAULT_WORSHIP_STREAM,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
            lyrics = "Dios no rechaza oración, la oración es alimento\nNunca vi un justo desamparado, ni su descendencia que mendigue pan\nSi estás pasando por las pruebas, alaba al Señor\nAlaba a Dios, alaba a Dios, tu alabanza romperá las cadenas.",
            downloadsCount = 1420,
            playsCount = 8950,
            timestamp = System.currentTimeMillis() - 86400000L * 2
        ),
        SongEntity(
            id = "song_02",
            title = "Cuerdas de Amor",
            artist = "Julio Melgar",
            ministry = "Julio Melgar Oficial",
            genre = "Adoración Íntima",
            album = "Se Trata de Ti",
            year = 2024,
            durationSeconds = 310,
            audioUrl = YoutubeAudioConverter.DEFAULT_WORSHIP_STREAM_2,
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600",
            lyrics = "Aunque pase por el valle de sombra y de muerte\nNo temeré mal alguno porque Tú estás conmigo\nCon cuerdas de amor me ataste a Ti\nTu fidelidad es grande, Tu misericordia infinita.",
            downloadsCount = 2890,
            playsCount = 15400,
            timestamp = System.currentTimeMillis() - 86400000L * 5
        ),
        SongEntity(
            id = "song_03",
            title = "La Bondad de Dios",
            artist = "Miel San Marcos",
            ministry = "Miel San Marcos",
            genre = "Alabanza Contemporánea",
            album = "Evangelio",
            year = 2024,
            durationSeconds = 295,
            audioUrl = YoutubeAudioConverter.DEFAULT_WORSHIP_STREAM,
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600",
            lyrics = "Te amo Dios, Tu gracia nunca falla\nCada día en Tus manos estoy\nDesde que despierto hasta que me acuesto\nCantaré de la bondad de Dios\nEn mi vida has sido bueno, en mi vida has sido fiel.",
            downloadsCount = 3120,
            playsCount = 19800,
            timestamp = System.currentTimeMillis() - 86400000L * 7
        ),
        SongEntity(
            id = "song_04",
            title = "Way Maker (Aquí Estás)",
            artist = "Sinach",
            ministry = "Worship Central",
            genre = "Worship / Adoración",
            album = "Way Maker",
            year = 2023,
            durationSeconds = 320,
            audioUrl = YoutubeAudioConverter.DEFAULT_WORSHIP_STREAM_2,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600",
            lyrics = "Aquí estás, obrando en este lugar\nTe adoraré, Te adoraré\nMilagroso, abres camino, cumples promesas\nLuz en las tinieblas, mi Dios, ese eres Tú.",
            downloadsCount = 4210,
            playsCount = 27500,
            timestamp = System.currentTimeMillis() - 86400000L * 10
        )
    )
}

