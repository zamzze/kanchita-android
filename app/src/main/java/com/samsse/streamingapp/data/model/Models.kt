package com.samsse.streamingapp.data.model

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: String,
    val tmdbId: Int? = null,
    val title: String,
    val originalTitle: String? = null,
    val description: String? = null,
    @SerializedName("release_year")
    val releaseYear: Int? = null,
    @SerializedName("duration_seconds")
    val durationSeconds: Int? = null,
    @SerializedName("poster_url")
    val posterUrl: String? = null,
    @SerializedName("backdrop_url")
    val backdropUrl: String? = null,
    val rating: String? = null,
    val genres: List<Genre> = emptyList()
)

data class Series(
    val id: String,
    val tmdbId: Int? = null,
    val title: String,
    val description: String? = null,
    @SerializedName("release_year")
    val releaseYear: Int? = null,
    @SerializedName("poster_url")
    val posterUrl: String? = null,
    @SerializedName("backdrop_url")
    val backdropUrl: String? = null,
    val rating: String? = null,
    val status: String? = null,
    val totalSeasons: Int = 1,
    val genres: List<Genre> = emptyList()
)
data class Episode(
    val id: String,
    @SerializedName("episode_number")
    val episodeNumber: Int,
    val title: String?,
    val description: String?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?
)

data class Genre(
    val id: String,
    val name: String
)

data class StreamsResponse(
    @SerializedName("content_id")
    val contentId: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("show_ads")
    val showAds: Boolean,
    @SerializedName("subtitle_url")
    val subtitleUrl: String?,
    val streams: List<Stream> = emptyList()
)

data class Stream(
    @SerializedName("server_name")
    val serverName: String,
    val quality: String,
    val language: String,
    @SerializedName("stream_url")
    val streamUrl: String?,
    @SerializedName("embed_url")
    val embedUrl: String?,
    @SerializedName("stream_type")
    val streamType: String,
    val priority: Int
)

data class WatchProgress(
    val contentType: String,
    val contentId: String,
    val progressSeconds: Int,
    val completed: Boolean
)

data class SearchResult(
    val id: String?,           // UUID local en BD (null si es nuevo)
    val tmdbId: Int,
    val title: String,
    val type: String,
    val posterUrl: String?,
    val releaseYear: Int?
)

data class ContentDetail(
    val id: String,
    val tmdbId: Int,
    val title: String,
    val type: String,
    val totalSeasons: Int?
)

data class HistoryItem(
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("content_id")
    val contentId: String,
    @SerializedName("progress_seconds")
    val progressSeconds: Int,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    val completed: Boolean,
    @SerializedName("last_watched_at")
    val lastWatchedAt: String,
    val title: String,
    val thumbnail: String?
)

data class MovieDetail(
    val id: String,
    @SerializedName("tmdb_id")
    val tmdbId: Int?,
    val title: String,
    @SerializedName("original_title")
    val originalTitle: String?,
    val description: String?,
    @SerializedName("release_year")
    val releaseYear: Int?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("poster_url")
    val posterUrl: String?,
    @SerializedName("backdrop_url")
    val backdropUrl: String?,
    val rating: String?,
    val genres: List<GenreDetail> = emptyList()
)

data class SeriesDetail(
    val id: String,
    @SerializedName("tmdb_id")
    val tmdbId: Int?,
    val title: String,
    val description: String?,
    @SerializedName("release_year")
    val releaseYear: Int?,
    @SerializedName("poster_url")
    val posterUrl: String?,
    @SerializedName("backdrop_url")
    val backdropUrl: String?,
    val rating: String?,
    val status: String?,
    val genres: List<GenreDetail> = emptyList(),
    val seasons: List<SeasonInfo> = emptyList()
)

data class GenreDetail(
    val id: Int,
    val name: String
)

data class SeasonInfo(
    @SerializedName("season_number")
    val seasonNumber: Int,
    @SerializedName("episode_count")
    val episodeCount: String
)