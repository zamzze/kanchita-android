package com.samsse.streamingapp.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsse.streamingapp.data.remote.ApiService
import com.samsse.streamingapp.data.remote.SearchResultDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SearchResultWithType(
    val result: SearchResultDto,
    val type: String
)

class SearchViewModel(private val api: ApiService) : ViewModel() {

    sealed class SearchState {
        object Idle    : SearchState()
        object Loading : SearchState()
        data class Success(val results: List<SearchResultWithType>) : SearchState()
        data class Error(val message: String) : SearchState()
    }

    private val _state = MutableLiveData<SearchState>(SearchState.Idle)
    val state: LiveData<SearchState> = _state

    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.length < 2) {
            _state.value = SearchState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _state.value = SearchState.Loading

            try {
                val moviesDeferred = async {
                    api.search(query, "movie").body()?.data?.map {
                        SearchResultWithType(it, "movie")
                    } ?: emptyList()
                }
                val seriesDeferred = async {
                    api.search(query, "series").body()?.data?.map {
                        SearchResultWithType(it, "series")
                    } ?: emptyList()
                }

                val combined = moviesDeferred.await() + seriesDeferred.await()

                _state.value = if (combined.isEmpty()) {
                    SearchState.Error("Sin resultados para \"$query\"")
                } else {
                    SearchState.Success(combined)
                }

            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error: ${e.message}")
                _state.value = SearchState.Error("Error al buscar")
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = SearchState.Idle
    }
}