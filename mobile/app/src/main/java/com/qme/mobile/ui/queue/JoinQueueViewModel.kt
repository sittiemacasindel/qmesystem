package com.qme.mobile.ui.queue

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.JoinQueueResponse
import com.qme.mobile.data.model.response.OrganizationResponse
import com.qme.mobile.data.repository.QueueRepository

class JoinQueueViewModel(private val repo: QueueRepository) : ViewModel() {

    fun lookupCode(code: String, onResult: (OrganizationResponse?, String?) -> Unit) {
        if (code.length != 6) {
            onResult(null, "Please enter a valid 6-digit queue code.")
            return
        }
        repo.lookupByCode(code, onResult)
    }

    fun confirmJoin(queueCode: String, onResult: (JoinQueueResponse?, String?) -> Unit) {
        repo.joinQueue(queueCode, onResult)
    }
}
