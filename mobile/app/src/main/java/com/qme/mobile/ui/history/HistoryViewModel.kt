package com.qme.mobile.ui.history

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.HistoryEntryResponse
import com.qme.mobile.data.repository.QueueRepository

class HistoryViewModel(private val repo: QueueRepository) : ViewModel() {

    fun loadHistory(onResult: (List<HistoryEntryResponse>?, String?) -> Unit) {
        repo.getHistory(onResult)
    }
}
