package com.samsse.streamingapp.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsse.streamingapp.data.model.Episode
import com.samsse.streamingapp.data.model.MovieDetail
import com.samsse.streamingapp.data.model.SeriesDetail
import com.samsse.streamingapp.data.model.Stream
import com.samsse.streamingapp.data.repository.MovieRepository
import com.samsse.streamingapp.data.repository.SeriesRepository
import com.samsse.streamingapp.data.repository.StreamRepository
import kotlinx.coroutines.launch
import com.samsse.streamingapp.data.remote.ApiService

class DetailViewModel(
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository,
    private val streamRepository: StreamRepository,
    private val api: ApiService
) : ViewModel() {

    // ─── Detail state ────────────────────────────────────────────────────
    sealed class DetailState {
        object Loading : DetailState()
        data class MovieLoaded(val movie: MovieDetail) : DetailState()
        data class SeriesLoaded(val series: SeriesDetail) : DetailState()
        data class Error(val message: String) : DetailState()
    }

    private val _detailState = MutableLiveData<DetailState>(DetailState.Loading)
    val detailState: LiveData<DetailState> = _detailState

    // ─── Streams state ───────────────────────────────────────────────────
    sealed class StreamsState {
        object Idle : StreamsState()
        object Loading : StreamsState()
        data class Success(
            val streams: List<Stream>,
            val showAds: Boolean,
            val subtitleUrl: String?
        ) : StreamsState()
        data class Error(val message: String) : StreamsState()
    }

    private val _streamsState = MutableLiveData<StreamsState>(StreamsState.Idle)
    private var resolvedContentId: String = ""
    val streamsState: LiveData<StreamsState> = _streamsState

    // ─── Episodes state ──────────────────────────────────────────────────
    private val _episodes = MutableLiveData<List<Episode>>(emptyList())
    val episodes: LiveData<List<Episode>> = _episodes

    private val _selectedSeason = MutableLiveData(1)
    val selectedSeason: LiveData<Int> = _selectedSeason

    private var currentSeriesId: String? = null

    // ─── Load detail ─────────────────────────────────────────────────────
    fun loadDetail(contentId: String, contentType: String) {
        viewModelScope.launch {
            _detailState.value = DetailState.Loading

            val isTmdbId = contentId.all { it.isDigit() }

            if (contentType == "movie") {
                val movieId = if (isTmdbId) {
                    fetchByTmdbId(contentId.toInt(), "movie") ?: run {
                        _detailState.value = DetailState.Error("No se pudo cargar el contenido")
                        return@launch
                    }
                } else contentId
                resolvedContentId = movieId  // ← guardar UUID resuelto

                val result = movieRepository.getMovieDetail(movieId)
                _detailState.value = if (result.isSuccess) {
                    DetailState.MovieLoaded(result.getOrNull()!!)
                } else {
                    DetailState.Error(result.exceptionOrNull()?.message ?: "Error")
                }

            } else {
                val seriesId = if (isTmdbId) {
                    // Mostrar loading mientras el backend procesa
                    _detailState.value = DetailState.Loading

                    val localId = fetchByTmdbId(contentId.toInt(), "series") ?: run {
                        _detailState.value = DetailState.Error("No se pudo cargar el contenido")
                        return@launch
                    }
                    localId
                } else contentId
                resolvedContentId = seriesId  // ← guardar UUID resuelto

                currentSeriesId = seriesId

                // Reintentar hasta 3 veces si seasons viene vacío
                var attempts = 0
                var seriesDetail: SeriesDetail? = null

                while (attempts < 3) {
                    val result = seriesRepository.getSeriesDetail(seriesId)
                    if (result.isSuccess) {
                        val detail = result.getOrNull()!!
                        if (detail.seasons.isNotEmpty() || attempts == 2) {
                            seriesDetail = detail
                            break
                        }
                    }
                    attempts++
                    kotlinx.coroutines.delay(1500)
                }

                if (seriesDetail != null) {
                    _detailState.value = DetailState.SeriesLoaded(seriesDetail)
                    loadEpisodes(seriesId, 1)
                } else {
                    _detailState.value = DetailState.Error("Error al cargar la serie")
                }
            }
        }
    }

    private suspend fun fetchByTmdbId(tmdbId: Int, type: String): String? {
        return try {
            // Llamada al content endpoint — agrega a BD si no existe
            val response = api.getContent(tmdbId, type)
            if (!response.isSuccessful) return null

            val localId = response.body()?.data?.id ?: return null

            // Si es serie, esperar un momento para que el backend
            // termine de procesar temporadas y episodios
            if (type == "series") {
                kotlinx.coroutines.delay(2000)
            }

            localId
        } catch (e: Exception) {
            android.util.Log.e("DetailViewModel", "fetchByTmdbId error: ${e.message}")
            null
        }
    }
    // ─── Load streams ────────────────────────────────────────────────────
    fun loadMovieStreams(movieId: String) {
        viewModelScope.launch {
            _streamsState.value = StreamsState.Loading
            val result = streamRepository.getMovieStreams(movieId)
            _streamsState.value = if (result.isSuccess) {
                val response = result.getOrNull()!!
                StreamsState.Success(response.streams, response.showAds, subtitleUrl = response.subtitleUrl)
            } else {
                StreamsState.Error(result.exceptionOrNull()?.message ?: "Error")
            }
        }
    }

    fun loadEpisodeStreams(episodeId: String) {
        viewModelScope.launch {
            _streamsState.value = StreamsState.Loading
            val result = streamRepository.getEpisodeStreams(episodeId)
            _streamsState.value = if (result.isSuccess) {
                val response = result.getOrNull()!!
                StreamsState.Success(response.streams, response.showAds, subtitleUrl = response.subtitleUrl)
            } else {
                StreamsState.Error(result.exceptionOrNull()?.message ?: "Error")
            }
        }
    }

    // ─── Episodes ────────────────────────────────────────────────────────
    fun selectSeason(season: Int) {
        _selectedSeason.value = season
        currentSeriesId?.let { loadEpisodes(it, season) }
    }

    private fun loadEpisodes(seriesId: String, season: Int) {
        viewModelScope.launch {
            val result = seriesRepository.getEpisodes(seriesId, season)
            if (result.isSuccess) {
                _episodes.value = result.getOrNull() ?: emptyList()
            }
        }
    }
    fun getResolvedContentId(): String = resolvedContentId
}