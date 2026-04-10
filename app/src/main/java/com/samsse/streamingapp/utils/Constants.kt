package com.samsse.streamingapp.utils

object Constants {
    // Emulador Android Studio → apunta a tu PC
    const val BASE_URL = "http://10.0.2.2:3000/api/"

    // Cuando despliegues en VPS cambiás solo esta línea:
    // const val BASE_URL = "https://tu-dominio.com/api/"

    const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w342"
    const val TMDB_BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"

    // DataStore keys
    const val PREFS_NAME = "streaming_prefs"

    // Progress tracking
    const val PROGRESS_SAVE_INTERVAL_MS = 10_000L // cada 10 segundos
    const val COMPLETION_THRESHOLD = 0.9f // 90% = completado
}