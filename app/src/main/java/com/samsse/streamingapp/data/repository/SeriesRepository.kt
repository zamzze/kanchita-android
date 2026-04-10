package com.samsse.streamingapp.data.repository

import com.samsse.streamingapp.data.model.Episode
import com.samsse.streamingapp.data.model.Series
import com.samsse.streamingapp.data.model.SeriesDetail
import com.samsse.streamingapp.data.remote.ApiService

class SeriesRepository(private val api: ApiService) {

    suspend fun getSeries(page: Int = 1): Result<List<Series>> {
        return try {
            val response = api.getSeries(page = page)
            if (response.isSuccessful) {
                val series = response.body()?.data?.items ?: emptyList()
                android.util.Log.d("SeriesRepo", "Series count: ${series.size}")
                Result.success(series)
            } else {
                Result.failure(Exception("Error al cargar series"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getSeriesDetail(seriesId: String): Result<SeriesDetail> {
        return try {
            val response = api.getSeriesDetail(seriesId)
            if (response.isSuccessful) {
                val detail = response.body()?.data
                if (detail != null) Result.success(detail)
                else Result.failure(Exception("Serie no encontrada"))
            } else {
                Result.failure(Exception("Error al cargar detalle"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun getEpisodes(seriesId: String, season: Int): Result<List<Episode>> {
        return try {
            val response = api.getEpisodes(seriesId, season)
            if (response.isSuccessful) {
                val episodes = response.body()?.data ?: emptyList()
                android.util.Log.d("SeriesRepo", "Episodes count: ${episodes.size}")
                Result.success(episodes)
            } else {
                Result.failure(Exception("Error al cargar episodios"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}