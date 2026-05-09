package com.qme.mobile.ui.queue

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.data.repository.QueueRepository

class QueueDetailViewModel(private val repo: QueueRepository) : ViewModel() {

    fun loadDetails(onResult: (QueueDetailsResponse?, String?) -> Unit) {
        repo.getMyQueue(onResult)
    }

    fun cancelQueue(entryId: String, onResult: (Boolean, String?) -> Unit) {
        repo.cancelQueue(entryId, onResult)
    }
}
