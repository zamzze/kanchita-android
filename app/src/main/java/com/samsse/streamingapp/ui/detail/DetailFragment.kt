package com.samsse.streamingapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.model.MovieDetail
import com.samsse.streamingapp.data.model.SeriesDetail
import com.samsse.streamingapp.data.model.Stream
import com.samsse.streamingapp.databinding.FragmentDetailBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModel()
    private val args: DetailFragmentArgs by navArgs()

    private lateinit var episodeAdapter: EpisodeAdapter
    private var currentTab = Tab.EPISODES
    private var currentSubtitleUrl: String? = null
    private var serverDialog: android.app.AlertDialog? = null

    enum class Tab { EPISODES, MORE }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                serverDialog?.isShowing == true -> serverDialog?.dismiss()
                binding.layoutBottom.isVisible  -> hideBottomSection()
                else -> {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        setupRecyclerView()
        setupListeners()
        observeState()
        viewModel.loadDetail(args.contentId, args.contentType)
    }

    private fun setupRecyclerView() {
        episodeAdapter = EpisodeAdapter { episode ->
            showLoadingDialog()
            viewModel.loadEpisodeStreams(episode.id)
        }
        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter        = episodeAdapter
        }
    }

    private fun setupListeners() {
        binding.btnPlay.setOnClickListener {
            when (val state = viewModel.detailState.value) {
                is DetailViewModel.DetailState.MovieLoaded -> {
                    showLoadingDialog()
                    viewModel.loadMovieStreams(state.movie.id)
                }
                is DetailViewModel.DetailState.SeriesLoaded -> {
                    val episodes = viewModel.episodes.value
                    if (!episodes.isNullOrEmpty()) {
                        showLoadingDialog()
                        viewModel.loadEpisodeStreams(episodes.first().id)
                    }
                }
                else -> {}
            }
        }

        binding.btnPlay.setOnFocusChangeListener { _, hasFocus ->
            binding.btnPlay.scaleX = if (hasFocus) 1.05f else 1f
            binding.btnPlay.scaleY = if (hasFocus) 1.05f else 1f
        }

        binding.btnMyList.setOnFocusChangeListener { _, hasFocus ->
            binding.btnMyList.scaleX = if (hasFocus) 1.05f else 1f
            binding.btnMyList.scaleY = if (hasFocus) 1.05f else 1f
        }

        binding.btnPlay.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                showBottomSection()
                true
            } else false
        }

        binding.btnMyList.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                showBottomSection()
                true
            } else false
        }

        binding.tabEpisodes.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                hideBottomSection()
                true
            } else false
        }

        binding.tabMore.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                hideBottomSection()
                true
            } else false
        }

        binding.tabEpisodes.setOnClickListener {
            currentTab = Tab.EPISODES
            updateTabs()
            showEpisodesContent()
        }

        binding.tabMore.setOnClickListener {
            currentTab = Tab.MORE
            updateTabs()
            showMoreContent()
        }

        binding.tabEpisodes.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.rvContent.isVisible = true
        }

        binding.tabMore.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !binding.layoutBottom.isVisible) {
                binding.layoutBottom.isVisible = true
            }
        }
    }

    private fun observeState() {
        viewModel.detailState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DetailViewModel.DetailState.Loading -> {
                    binding.tvTitle.text       = "Cargando..."
                    binding.tvDescription.text = "Obteniendo información del contenido..."
                    binding.btnPlay.isEnabled  = false
                }
                is DetailViewModel.DetailState.MovieLoaded  -> bindMovie(state.movie)
                is DetailViewModel.DetailState.SeriesLoaded -> bindSeries(state.series)
                is DetailViewModel.DetailState.Error -> {
                    binding.tvTitle.text       = "Error"
                    binding.tvDescription.text = state.message
                }
            }
        }

        viewModel.streamsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DetailViewModel.StreamsState.Loading -> showLoadingDialog()
                is DetailViewModel.StreamsState.Success -> {
                    currentSubtitleUrl = state.subtitleUrl
                    showServersDialog(state.streams)
                }
                is DetailViewModel.StreamsState.Error -> {
                    serverDialog?.dismiss()
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Contenido no disponible")
                        .setMessage(state.message.ifEmpty {
                            "Este contenido aún no está disponible. ¡Muy pronto habrá más contenido!"
                        })
                        .setPositiveButton("Entendido", null)
                        .show()
                }
                else -> {}
            }
        }

        viewModel.episodes.observe(viewLifecycleOwner) { episodes ->
            episodeAdapter.submitList(episodes)
        }
    }

    // ─── Dialog de servidores ─────────────────────────────────────────────────

    private fun showLoadingDialog() {
        serverDialog?.dismiss()
        serverDialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar servidor")
            .setMessage("Cargando servidores...")
            .setNegativeButton("Cancelar") { _, _ -> }
            .create()
        serverDialog?.show()
    }

    private fun showServersDialog(streams: List<Stream>) {
        serverDialog?.dismiss()

        if (streams.isEmpty()) {
            serverDialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Sin servidores")
                .setMessage("No hay servidores disponibles para este contenido.")
                .setNegativeButton("Cerrar") { _, _ -> }
                .create()
            serverDialog?.show()
            return
        }

        val serverNames = streams.map {
            "${it.serverName} — ${formatLanguage(it.language)}"
        }.toTypedArray()

        serverDialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar servidor")
            .setItems(serverNames) { _, index ->
                navigateToPlayer(streams[index], streams)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        serverDialog?.show()
    }

    // ─── Resto de métodos ─────────────────────────────────────────────────────

    private fun bindMovie(movie: MovieDetail) {
        binding.tvTypeBadge.text   = "PELÍCULA"
        binding.tvTitle.text       = movie.title
        binding.tvYear.text        = movie.releaseYear?.toString() ?: ""
        binding.tvRating.text      = movie.rating ?: ""
        binding.tvDescription.text = movie.description ?: ""
        binding.tvGenres.text      = movie.genres.joinToString(" • ") { it.name }

        movie.durationSeconds?.let { secs ->
            val h = secs / 3600
            val m = (secs % 3600) / 60
            binding.tvDuration.text      = if (h > 0) "${h}h ${m}m" else "${m}m"
            binding.tvDuration.isVisible = true
        }

        loadImages(movie.backdropUrl, movie.posterUrl)

        binding.tabEpisodes.isVisible   = false
        binding.tabMore.isVisible       = true
        binding.scrollSeasons.isVisible = false
        binding.tabMore.isSelected      = true
        binding.layoutBottom.isVisible  = false
        binding.tabMore.setTextColor(resources.getColor(R.color.white, null))
        binding.btnPlay.nextFocusDownId   = binding.tabMore.id
        binding.btnMyList.nextFocusDownId = binding.tabMore.id

        binding.btnPlay.isEnabled = true
        binding.btnPlay.post { binding.btnPlay.requestFocus() }
    }

    private fun bindSeries(series: SeriesDetail) {
        binding.tvTypeBadge.text   = "SERIE"
        binding.tvTitle.text       = series.title
        binding.tvYear.text        = series.releaseYear?.toString() ?: ""
        binding.tvRating.text      = series.rating ?: ""
        binding.tvDescription.text = series.description ?: ""
        binding.tvGenres.text      = series.genres.joinToString(" • ") { it.name }

        val totalSeasons = series.seasons.size
        binding.tvDuration.text      = "$totalSeasons temporada${if (totalSeasons > 1) "s" else ""}"
        binding.tvDuration.isVisible = true

        loadImages(series.backdropUrl, series.posterUrl)

        binding.tabEpisodes.isVisible  = true
        binding.tabMore.isVisible      = true
        binding.layoutBottom.isVisible = false
        binding.btnPlay.nextFocusDownId   = binding.tabEpisodes.id
        binding.btnMyList.nextFocusDownId = binding.tabEpisodes.id

        if (series.seasons.isNotEmpty()) {
            binding.scrollSeasons.isVisible = true
            buildSeasonSelector(series.seasons.map { it.seasonNumber })
        }

        currentTab = Tab.EPISODES
        updateTabs()
        binding.btnPlay.isEnabled = true
        binding.btnPlay.post { binding.btnPlay.requestFocus() }
    }

    private fun buildSeasonSelector(seasons: List<Int>) {
        binding.layoutSeasons.removeAllViews()
        seasons.forEach { seasonNumber ->
            val btn = TextView(requireContext()).apply {
                text        = "Temporada $seasonNumber"
                textSize    = 13f
                setTextColor(resources.getColor(R.color.white_70, null))
                setPadding(20, 0, 20, 0)
                gravity     = android.view.Gravity.CENTER_VERTICAL
                focusable   = View.FOCUSABLE
                isClickable = true
                background  = resources.getDrawable(R.drawable.bg_focusable_item, null)
                setOnClickListener {
                    viewModel.selectSeason(seasonNumber)
                    highlightSeasonButton(this)
                }
                setOnFocusChangeListener { _, hasFocus ->
                    scaleX = if (hasFocus) 1.05f else 1f
                    scaleY = if (hasFocus) 1.05f else 1f
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { marginEnd = 8 }
            binding.layoutSeasons.addView(btn, params)
        }
        (binding.layoutSeasons.getChildAt(0) as? TextView)?.let {
            highlightSeasonButton(it)
        }
    }

    private fun showBottomSection() {
        binding.layoutBottom.isVisible = true
        updateTabs()
        if (binding.tabEpisodes.isVisible) {
            binding.tabEpisodes.requestFocus()
            showEpisodesContent()
        } else {
            binding.tabMore.requestFocus()
        }
    }

    private fun hideBottomSection() {
        binding.layoutBottom.isVisible = false
        binding.btnPlay.requestFocus()
        binding.tabEpisodes.setTextColor(resources.getColor(R.color.white_40, null))
        binding.tabMore.setTextColor(resources.getColor(R.color.white_40, null))
    }

    private fun highlightSeasonButton(selected: TextView) {
        for (i in 0 until binding.layoutSeasons.childCount) {
            val child = binding.layoutSeasons.getChildAt(i) as? TextView ?: continue
            child.setTextColor(
                resources.getColor(
                    if (child == selected) R.color.white else R.color.white_40, null
                )
            )
        }
    }

    private fun loadImages(backdropUrl: String?, posterUrl: String?) {
        Glide.with(this).load(backdropUrl).centerCrop().into(binding.ivBackdrop)
        Glide.with(this)
            .load(posterUrl)
            .centerCrop()
            .placeholder(R.drawable.placeholder_poster)
            .into(binding.ivPoster)
    }

    private fun updateTabs() {
        val isEpisodes = currentTab == Tab.EPISODES
        binding.tabEpisodes.isSelected = isEpisodes
        binding.tabMore.isSelected     = !isEpisodes
        binding.tabEpisodes.setTextColor(
            resources.getColor(if (isEpisodes) R.color.white else R.color.white_40, null)
        )
        binding.tabMore.setTextColor(
            resources.getColor(if (!isEpisodes) R.color.white else R.color.white_40, null)
        )
        if (isEpisodes) showEpisodesContent() else showMoreContent()
    }

    private fun showEpisodesContent() {
        binding.scrollSeasons.isVisible = true
        episodeAdapter.submitList(viewModel.episodes.value ?: emptyList())
    }

    private fun showMoreContent() {
        binding.scrollSeasons.isVisible = false
        episodeAdapter.submitList(emptyList())
    }

    private fun formatLanguage(language: String): String = when (language) {
        "es-lat"  -> "Español Latino"
        "es-cast" -> "Español Castellano"
        "en-sub"  -> "Inglés + Subtítulos ES"
        "en"      -> "Inglés"
        else      -> language
    }

    private fun navigateToPlayer(selectedStream: Stream, allStreams: List<Stream>) {
        val resolvedId = viewModel.getResolvedContentId().ifEmpty { args.contentId }
        val action = DetailFragmentDirections.actionDetailToPlayer(
            contentId   = resolvedId,
            contentType = args.contentType,
            showAds     = false,
            streamsJson = Gson().toJson(allStreams),
            subtitleUrl = currentSubtitleUrl ?: ""
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        serverDialog?.dismiss()
        serverDialog = null
        _binding = null
    }
}