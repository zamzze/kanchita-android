package com.samsse.streamingapp.data.repository

import com.samsse.streamingapp.data.model.StreamsResponse
import com.samsse.streamingapp.data.remote.ApiService

class StreamRepository(private val api: ApiService) {

    suspend fun getMovieStreams(movieId: String): Result<StreamsResponse> {
        return try {
            val response = api.getMovieStreams(movieId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("Sin streams disponibles"))
            } else {
                Result.failure(Exception("Error al cargar streams"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodeStreams(episodeId: String): Result<StreamsResponse> {
        return try {
            val response = api.getEpisodeStreams(episodeId)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) Result.success(data)
                else Result.failure(Exception("Sin streams disponibles"))
            } else {
                Result.failure(Exception("Error al cargar streams"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}