package com.samsse.streamingapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.model.Episode

class EpisodeAdapter(
    private val onEpisodeClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    private var episodes: List<Episode> = emptyList()

    fun submitList(list: List<Episode>) {
        episodes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(episodes[position])
    }

    override fun getItemCount() = episodes.size

    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivThumbnail   = itemView.findViewById<ImageView>(R.id.iv_thumbnail)
        private val tvNumber      = itemView.findViewById<TextView>(R.id.tv_episode_number)
        private val tvTitle       = itemView.findViewById<TextView>(R.id.tv_episode_title)
        private val tvDescription = itemView.findViewById<TextView>(R.id.tv_episode_description)
        private val tvDuration    = itemView.findViewById<TextView>(R.id.tv_episode_duration)

        fun bind(episode: Episode) {
            tvNumber.text      = "E${episode.episodeNumber}"
            tvTitle.text       = episode.title ?: "Episodio ${episode.episodeNumber}"
            tvDescription.text = episode.description ?: ""

            episode.durationSeconds?.let { secs ->
                tvDuration.text = "${secs / 60}m"
            }

            Glide.with(itemView.context)
                .load(episode.thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_poster)
                .into(ivThumbnail)

            itemView.setOnClickListener { onEpisodeClick(episode) }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                itemView.scaleX = if (hasFocus) 1.02f else 1f
                itemView.scaleY = if (hasFocus) 1.02f else 1f
            }
        }
    }
}