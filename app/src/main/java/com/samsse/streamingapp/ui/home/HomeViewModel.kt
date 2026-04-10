package com.samsse.streamingapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsse.streamingapp.data.model.Movie
import com.samsse.streamingapp.data.model.Series
import com.samsse.streamingapp.data.repository.MovieRepository
import com.samsse.streamingapp.data.repository.SeriesRepository
import com.samsse.streamingapp.data.model.HistoryItem
import com.samsse.streamingapp.data.repository.HistoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeViewModel(
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository,
    private val historyRepository: HistoryRepository

) : ViewModel() {

    sealed class HomeState {
        object Loading : HomeState()
        data class Success(
            val movies: List<Movie>,
            val series: List<Series>,
            val history: List<HistoryItem> = emptyList()
        ) : HomeState()
        data class Error(val message: String) : HomeState()
    }

    private val _state = MutableLiveData<HomeState>(HomeState.Loading)
    val state: LiveData<HomeState> = _state

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            try {
                val moviesDeferred  = async { movieRepository.getMovies() }
                val seriesDeferred  = async { seriesRepository.getSeries() }
                val historyDeferred = async { historyRepository.getHistory() }

                val movies  = moviesDeferred.await().getOrDefault(emptyList())
                val series  = seriesDeferred.await().getOrDefault(emptyList())
                val history = historyDeferred.await().getOrDefault(emptyList())

                _state.value = HomeState.Success(movies, series, history)
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}