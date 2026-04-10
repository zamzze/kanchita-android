package com.samsse.streamingapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsse.streamingapp.data.repository.HistoryRepository
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private var contentId:        String = ""
    private var contentType:      String = ""
    private var estimatedProgress: Int   = 0

    fun setContent(contentId: String, contentType: String) {
        this.contentId   = contentId
        this.contentType = contentType
    }

    fun updateEstimatedProgress(seconds: Int) {
        estimatedProgress = seconds
    }

    fun saveProgressAndExit() {
        if (estimatedProgress > 0 && contentId.isNotEmpty()) {
            viewModelScope.launch {
                historyRepository.saveProgress(contentType, contentId, estimatedProgress)
            }
        }
    }
}