package com.samsse.streamingapp.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.model.Stream
import com.samsse.streamingapp.databinding.FragmentPlayerBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModel()
    private val args: PlayerFragmentArgs by navArgs()


    private var exoPlayer: ExoPlayer? = null
    private var streams: List<Stream> = emptyList()
    private var progressTimer: android.os.CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = object : TypeToken<List<Stream>>() {}.type
        streams = Gson().fromJson(args.streamsJson, type) ?: emptyList()

        viewModel.setContent(args.contentId, args.contentType)


        setupExoPlayer()
        setupListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                binding.layoutServerSelector.isVisible -> hideServerSelector()
                else -> saveProgressAndExit()
            }
        }

        val firstStream = streams.minByOrNull { it.priority }
        firstStream?.let { loadStream(it) }
    }

    // ─── ExoPlayer ───────────────────────────────────────────────────────────

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            binding.playerView.player        = player
            binding.playerView.useController = true
            binding.playerView.setControllerVisibilityListener(
                androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                    binding.layoutHeader.visibility = visibility
                }
            )


            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> showBuffering(true)
                        Player.STATE_READY     -> showBuffering(false)
                        Player.STATE_ENDED     -> saveProgressAndExit()
                        else                   -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e("PlayerFragment", "ExoPlayer error: ${error.message}")
                    showBuffering(false)
                    showError("Error al reproducir. Intenta otro servidor.")
                }
            })
        }
    }

    private fun playDirect(streamUrl: String) {
        val subtitleUrl = args.subtitleUrl.takeIf { it.isNotEmpty() }
        android.util.Log.d("PlayerFragment", "Stream URL: $streamUrl")
        android.util.Log.d("PlayerFragment", "Subtitle URL: $subtitleUrl")

        binding.playerView.isVisible       = true

        showBuffering(true)

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                mapOf(
                    "Referer"    to "https://vidfast.pro/",
                    "Origin"     to "https://vidfast.pro",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )

        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))

        val mediaSource = if (subtitleUrl != null) {
            // Crear source del subtítulo .vtt por separado
            val subtitleDataSourceFactory = DefaultHttpDataSource.Factory()
            val subtitleSource = androidx.media3.exoplayer.source.SingleSampleMediaSource
                .Factory(subtitleDataSourceFactory)
                .createMediaSource(
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                        .setMimeType(androidx.media3.common.MimeTypes.TEXT_VTT)
                        .setLanguage("es")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build(),
                    androidx.media3.common.C.TIME_UNSET
                )

            // Combinar HLS + subtítulo
            androidx.media3.exoplayer.source.MergingMediaSource(hlsSource, subtitleSource)
        } else {
            hlsSource
        }

        exoPlayer?.apply {
            addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    for (group in tracks.groups) {
                        if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                            android.util.Log.d("PlayerFragment", "Text tracks: ${group.length}")
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                android.util.Log.d("PlayerFragment",
                                    "Track $i: lang=${format.language} mime=${format.sampleMimeType} selected=${group.isTrackSelected(i)}")
                            }
                        }
                    }
                }
            })

            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true

            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguage("es")
                .setIgnoredTextSelectionFlags(0)
                .setSelectUndeterminedTextLanguage(true)
                .build()
        }

        startProgressTimer()
    }



    // ─── Stream loader ────────────────────────────────────────────────────────

    private fun loadStream(stream: Stream) {
        binding.layoutError.isVisible = false
        binding.tvServerName.text     = stream.serverName
        binding.tvServerLang.text     = formatLanguage(stream.language)

        when (stream.streamType) {
            "direct" -> {
                stream.streamUrl?.let { playDirect(it) }
                    ?: showError("URL de stream no disponible")
            }

            else -> showError("Tipo de stream desconocido")
        }
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private fun showBuffering(show: Boolean) {
        binding.layoutBuffering.isVisible = show
    }

    private fun showError(message: String) {
        binding.layoutError.isVisible     = true
        binding.tvError.text              = message
        binding.layoutBuffering.isVisible = false
    }

    private fun setupListeners() {
        binding.btnChangeServer.setOnClickListener { showServerSelector() }
        binding.btnBack.setOnClickListener { saveProgressAndExit() }
        binding.btnCancelServer.setOnClickListener { hideServerSelector() }
    }

    private fun showServerSelector() {
        binding.layoutServerSelector.isVisible = true

        binding.layoutServerSelector.isFocusable = true
        binding.layoutServerSelector.isFocusableInTouchMode = true
        binding.layoutServers.removeAllViews()
        binding.layoutServerSelector.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        hideServerSelector()
                        true
                    }
                    else -> false
                }
            } else false
        }


        streams.forEach { stream ->
            val itemView = layoutInflater.inflate(R.layout.item_server, binding.layoutServers, false)

            itemView.findViewById<android.widget.TextView>(R.id.tv_server_name).text =
                stream.serverName
            itemView.findViewById<android.widget.TextView>(R.id.tv_server_lang).text =
                formatLanguage(stream.language)
            itemView.findViewById<android.widget.TextView>(R.id.tv_server_quality).text =
                stream.quality

            itemView.alpha = if (stream.serverName == binding.tvServerName.text) 1f else 0.6f

            itemView.setOnClickListener {
                hideServerSelector()
                exoPlayer?.stop()
                    loadStream(stream)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                itemView.scaleX = if (hasFocus) 1.03f else 1f
                itemView.scaleY = if (hasFocus) 1.03f else 1f
            }

            itemView.isFocusable = true
            binding.layoutServers.addView(itemView)
        }

        binding.layoutServers.getChildAt(0)?.requestFocus()
    }

    private fun hideServerSelector() {
        binding.layoutServerSelector.isVisible = false
    }

    // ─── Progress ────────────────────────────────────────────────────────────

    private fun startProgressTimer() {
        progressTimer?.cancel()
        var elapsedSeconds = 0
        progressTimer = object : android.os.CountDownTimer(Long.MAX_VALUE, 10_000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds += 10
                // Para ExoPlayer usar posición real si está disponible
                val realPosition = exoPlayer?.currentPosition?.div(1000)?.toInt()
                viewModel.updateEstimatedProgress(realPosition ?: elapsedSeconds)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun saveProgressAndExit() {
        progressTimer?.cancel()
        exoPlayer?.let { viewModel.updateEstimatedProgress((it.currentPosition / 1000).toInt()) }
        viewModel.saveProgressAndExit()
        findNavController().popBackStack()
    }

    private fun formatLanguage(language: String): String = when (language) {
        "es-lat"  -> "Español Latino"
        "es-cast" -> "Español Castellano"
        "en-sub"  -> "Inglés + Subtítulos ES"
        "en"      -> "Inglés"
        else      -> language
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        progressTimer?.cancel()
        exoPlayer?.pause()

    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()

        startProgressTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressTimer?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}