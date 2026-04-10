package com.samsse.streamingapp.ui.search

import android.os.Bundle
import android.view.View
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.navigation.fragment.findNavController
import com.samsse.streamingapp.data.remote.SearchResultDto
import com.samsse.streamingapp.ui.home.CardPresenter
import com.samsse.streamingapp.ui.home.ContentItem
import org.koin.androidx.viewmodel.ext.android.viewModel


class SearchFragment : SearchSupportFragment(),
    SearchSupportFragment.SearchResultProvider {

    private val viewModel: SearchViewModel by viewModel()
    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
        setupClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
    }

    private fun setupClickListener() {
        setOnItemViewClickedListener { _, item, _, _ ->
            if (item is ContentItem) {
                val action = SearchFragmentDirections.actionSearchToDetail(
                    contentId   = if (item.id.isEmpty()) item.tmdbId.toString() else item.id,
                    contentType = item.type
                )
                findNavController().navigate(action)
            }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        viewModel.search(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.search(query)
        return true
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchViewModel.SearchState.Idle    -> rowsAdapter.clear()
                is SearchViewModel.SearchState.Loading -> rowsAdapter.clear()
                is SearchViewModel.SearchState.Success -> buildResults(state.results)
                is SearchViewModel.SearchState.Error   -> rowsAdapter.clear()
                is SearchViewModel.SearchState.Success -> buildResults(state.results)
            }
        }
    }
    private fun buildResults(results: List<SearchResultWithType>) {
        rowsAdapter.clear()

        val movies = results.filter { it.type == "movie" }
        val series = results.filter { it.type == "series" }

        val cardPresenter = CardPresenter()

        if (movies.isNotEmpty()) {
            val adapter = ArrayObjectAdapter(cardPresenter)
            movies.forEach { adapter.add(it.toContentItem()) }
            rowsAdapter.add(ListRow(HeaderItem(0, "Películas"), adapter))
        }

        if (series.isNotEmpty()) {
            val adapter = ArrayObjectAdapter(cardPresenter)
            series.forEach { adapter.add(it.toContentItem()) }
            rowsAdapter.add(ListRow(HeaderItem(1, "Series"), adapter))
        }
    }

    private fun SearchResultWithType.toContentItem() = ContentItem(
        id          = this.result.localId ?: "",
        tmdbId      = this.result.tmdbId,
        title       = this.result.title,
        posterUrl   = this.result.posterUrl,
        backdropUrl = null,
        type        = this.type,
        year        = this.result.releaseYear
    )
}