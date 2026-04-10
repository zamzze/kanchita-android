package com.samsse.streamingapp.ui.home

data class ContentItem(
    val id: String,
    val tmdbId: Int?,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val type: String,
    val year: Int?
)