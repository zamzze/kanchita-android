package com.samsse.streamingapp.data.repository

import com.samsse.streamingapp.data.model.Movie
import com.samsse.streamingapp.data.remote.SearchResultDto
import com.samsse.streamingapp.data.remote.ApiService
import com.samsse.streamingapp.data.model.MovieDetail

class MovieRepository(private val api: ApiService) {

    suspend fun getMovies(page: Int = 1, genre: String? = null): Result<List<Movie>> {
        return try {
            val response = api.getMovies(page = page, genre = genre)
            if (response.isSuccessful) {
                val movies = response.body()?.data?.items ?: emptyList()
                android.util.Log.d("MovieRepo", "Movies count: ${movies.size}")
                Result.success(movies)
            } else {
                android.util.Log.e("MovieRepo", "Error: ${response.code()}")
                Result.failure(Exception("Error al cargar películas"))
            }
        } catch (e: Exception) {
            android.util.Log.e("MovieRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun getMovieDetail(movieId: String): Result<MovieDetail> {
        return try {
            val response = api.getMovieDetail(movieId)
            if (response.isSuccessful) {
                val detail = response.body()?.data
                if (detail != null) Result.success(detail)
                else Result.failure(Exception("Película no encontrada"))
            } else {
                Result.failure(Exception("Error al cargar detalle"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String): Result<List<SearchResultDto>> {
        return try {
            val response = api.search(query, "movie")
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Error en búsqueda"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}