package com.samsse.streamingapp.data.remote

import com.google.gson.annotations.SerializedName
import com.samsse.streamingapp.data.model.*

// ─── Request bodies ──────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)
data class RegisterRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class ProgressRequest(
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("content_id")
    val contentId: String,
    @SerializedName("progress_seconds")
    val progressSeconds: Int
)

// ─── Response bodies ─────────────────────────────────────────────────────────

data class LoginResponse(
    val success: Boolean,
    val data: LoginData?
)

data class LoginData(
    val user: UserData?,
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class UserData(
    val id: String,
    val email: String,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("plan_type")
    val planType: String
)

data class PagedResponse<T>(
    val items: List<T>,
    val pagination: PaginationData?
)
data class PaginationData(
    val page: Int,
    val limit: Int,
    val total: Int,
    @SerializedName("total_pages")
    val totalPages: Int
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?
)

data class ApiListResponse<T>(
    val success: Boolean,
    val data: PagedResponse<T>?
)
data class ApiSearchResponse(
    val success: Boolean,
    val data: List<SearchResultDto>
)

data class SearchResultDto(
    @SerializedName("tmdb_id")
    val tmdbId: Int,
    val title: String,
    @SerializedName("release_year")
    val releaseYear: Int?,
    @SerializedName("poster_url")
    val posterUrl: String?,
    @SerializedName("in_catalog")
    val inCatalog: Boolean,
    @SerializedName("local_id")
    val localId: String?
)

data class ApiContentResponse(
    val id: String,
    @SerializedName("tmdb_id")
    val tmdbId: Int,
    val title: String,
    val type: String,
    @SerializedName("total_seasons")
    val totalSeasons: Int?
)
data class ApiDetailResponse<T>(
    val success: Boolean,
    val data: T?
)

data class ApiDirectResponse<T>(
    val success: Boolean,
    val data: T?
)