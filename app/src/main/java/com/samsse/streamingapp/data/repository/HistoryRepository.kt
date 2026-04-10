package com.samsse.streamingapp.data.repository

import com.samsse.streamingapp.data.model.HistoryItem
import com.samsse.streamingapp.data.remote.ApiService
import com.samsse.streamingapp.data.remote.ProgressRequest

class HistoryRepository(private val api: ApiService) {

    suspend fun saveProgress(
        contentType: String,
        contentId: String,
        progressSeconds: Int
    ): Result<Unit> {
        return try {
            val response = api.saveProgress(
                ProgressRequest(contentType, contentId, progressSeconds)
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al guardar progreso"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistory(): Result<List<HistoryItem>> {
        return try {
            val response = api.getHistory()
            if (response.isSuccessful) {
                // data es directamente un array, no tiene .items
                val body = response.body()
                android.util.Log.d("HistoryRepo", "Response: $body")
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Error al cargar historial"))
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryRepo", "Error: ${e.message}")
            Result.failure(e)
        }
    }
}