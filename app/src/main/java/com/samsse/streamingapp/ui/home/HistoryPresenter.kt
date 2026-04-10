package com.samsse.streamingapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.samsse.streamingapp.R
import com.samsse.streamingapp.data.model.HistoryItem

class HistoryPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_card, parent, false)
        view.isFocusable         = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val history = item as? HistoryItem ?: return
        val view    = viewHolder.view

        view.findViewById<TextView>(R.id.tv_title).text = history.title

        // Calcular progreso — si no hay duration usamos estimado de 2h
        val duration = history.durationSeconds ?: 7200
        val progress = ((history.progressSeconds.toFloat() / duration) * 100).toInt()
            .coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.progress_bar).progress = progress

        Glide.with(view.context)
            .load(history.thumbnail)
            .centerCrop()
            .placeholder(R.drawable.placeholder_poster)
            .into(view.findViewById(R.id.iv_thumbnail))

        view.setOnFocusChangeListener { _, hasFocus ->
            view.scaleX = if (hasFocus) 1.05f else 1f
            view.scaleY = if (hasFocus) 1.05f else 1f
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}