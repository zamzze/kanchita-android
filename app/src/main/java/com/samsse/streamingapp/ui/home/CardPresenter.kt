package com.samsse.streamingapp.ui.home

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.media3.common.BuildConfig
import com.bumptech.glide.Glide
import com.samsse.streamingapp.R
import com.samsse.streamingapp.utils.Constants

class CardPresenter : Presenter() {

    companion object {
        private const val CARD_WIDTH  = 200
        private const val CARD_HEIGHT = 300
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable             = true
            isFocusableInTouchMode  = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            setBackgroundColor(
                ContextCompat.getColor(context, R.color.surface_variant)
            )
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val card     = item as? ContentItem ?: return
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText   = card.title
        cardView.contentText = card.year?.toString() ?: ""

        val imageUrl = card.posterUrl?.let {
            val url = if (it.startsWith("http")) it else Constants.TMDB_IMAGE_BASE + it
            // En debug usar http para evitar SSL del emulador
            if (BuildConfig.DEBUG) url.replace("https://", "http://") else url
        }

        cardView.mainImageView?.let { imageView ->
            Glide.with(cardView.context)
                .load(imageUrl)
                .centerCrop()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .thumbnail(0.1f)
                .placeholder(R.drawable.placeholder_poster)
                .error(R.drawable.placeholder_poster)
                .into(imageView)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage  = null
    }
}