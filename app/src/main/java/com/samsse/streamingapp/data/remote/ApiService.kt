package com.samsse.streamingapp.data.remote

import com.samsse.streamingapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // ─── Movies ──────────────────────────────────────────────────────────
    @GET("movies")
    suspend fun getMovies(
        @Query("page")  page:  Int    = 1,
        @Query("limit") limit: Int    = 20,
        @Query("genre") genre: String? = null
    ): Response<ApiListResponse<Movie>>

    @GET("series")
    suspend fun getSeries(
        @Query("page")  page:  Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiListResponse<Series>>

    @GET("series/{id}/seasons/{season}")
    suspend fun getEpisodes(
        @Path("id")     seriesId: String,
        @Path("season") season:   Int
    ): Response<ApiDetailResponse<List<Episode>>>

    // ─── Streams ─────────────────────────────────────────────────────────
    @GET("streams/movie/{id}")
    suspend fun getMovieStreams(
        @Path("id") movieId: String
    ): Response<ApiDetailResponse<StreamsResponse>>

    @GET("streams/episode/{id}")
    suspend fun getEpisodeStreams(
        @Path("id") episodeId: String
    ): Response<ApiDetailResponse<StreamsResponse>>

    // ─── Content ─────────────────────────────────────────────────────────
    @GET("content/search")
    suspend fun search(
        @Query("q")    query: String,
        @Query("type") type:  String
    ): Response<ApiSearchResponse>

    @GET("content/{tmdbId}")
    suspend fun getContent(
        @Path("tmdbId") tmdbId: Int,
        @Query("type")  type:   String
    ): Response<ApiDetailResponse<ApiContentResponse>>

    @GET("movies/{id}")
    suspend fun getMovieDetail(
        @Path("id") movieId: String
    ): Response<ApiDetailResponse<MovieDetail>>

    @GET("series/{id}")
    suspend fun getSeriesDetail(
        @Path("id") seriesId: String
    ): Response<ApiDetailResponse<SeriesDetail>>


    // ─── History ─────────────────────────────────────────────────────────
    @POST("history")
    suspend fun saveProgress(@Body body: ProgressRequest): Response<Unit>

    @GET("history")
    suspend fun getHistory(): Response<ApiDirectResponse<List<HistoryItem>>>
}