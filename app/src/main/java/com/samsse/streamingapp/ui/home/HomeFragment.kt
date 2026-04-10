package com.samsse.streamingapp.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.navigation.fragment.findNavController
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.model.HistoryItem
import com.samsse.streamingapp.data.model.Movie
import com.samsse.streamingapp.data.model.Series
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModel()
    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRowsAdapter()
        observeState()
    }

    private fun setupUI() {
        title                            = "Kanchita+"
        headersState                     = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor                       = ContextCompat.getColor(requireContext(), R.color.accent)
        searchAffordanceColor            = ContextCompat.getColor(requireContext(), R.color.white)

        setOnSearchClickedListener {
            findNavController().navigate(R.id.action_home_to_search)
        }

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is ContentItem -> {
                    val action = HomeFragmentDirections.actionHomeToDetail(
                        contentId   = item.id,
                        contentType = item.type
                    )
                    findNavController().navigate(action)
                }
                is HistoryItem -> {
                    val action = HomeFragmentDirections.actionHomeToDetail(
                        contentId   = item.contentId,
                        contentType = item.contentType
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setupRowsAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter     = rowsAdapter
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeViewModel.HomeState.Loading -> {}
                is HomeViewModel.HomeState.Success -> buildRows(state.movies, state.series,state.history)
                is HomeViewModel.HomeState.Error   -> {}
            }
        }
    }

    private fun buildRows(movies: List<Movie>, series: List<Series>, history: List<HistoryItem>) {
        rowsAdapter.clear()
        //val cardPresenter = CardPresenter()
        // Fila 1 — Continuar viendo (si hay historial)
        if (history.isNotEmpty()) {
            val historyAdapter = ArrayObjectAdapter(CardPresenter())
            history.forEach { item ->
                historyAdapter.add(
                    ContentItem(
                        id          = item.contentId,
                        tmdbId      = null,
                        title       = item.title,
                        posterUrl   = item.thumbnail?.replace("/w500/", "/w342/"),
                        backdropUrl = null,
                        type        = item.contentType,
                        year        = null
                    )
                )
            }
            rowsAdapter.add(ListRow(HeaderItem(0, "Continuar viendo"), historyAdapter))
        }

        if (movies.isNotEmpty()) {
            val moviesAdapter = ArrayObjectAdapter(CardPresenter())
            movies.forEach { movie ->
                moviesAdapter.add(
                    ContentItem(
                        id          = movie.id,
                        tmdbId      = movie.tmdbId,
                        title       = movie.title,
                        posterUrl   = movie.posterUrl,
                        backdropUrl = movie.backdropUrl,
                        type        = "movie",
                        year        = movie.releaseYear
                    )
                )
            }
            rowsAdapter.add(ListRow(HeaderItem(1, "Películas"), moviesAdapter))
        }

        if (series.isNotEmpty()) {
            val seriesAdapter = ArrayObjectAdapter(CardPresenter())
            series.forEach { serie ->
                seriesAdapter.add(
                    ContentItem(
                        id          = serie.id,
                        tmdbId      = serie.tmdbId,
                        title       = serie.title,
                        posterUrl   = serie.posterUrl,
                        backdropUrl = serie.backdropUrl,
                        type        = "series",
                        year        = serie.releaseYear
                    )
                )
            }
            rowsAdapter.add(ListRow(HeaderItem(2, "Series"), seriesAdapter))
        }
    }
}