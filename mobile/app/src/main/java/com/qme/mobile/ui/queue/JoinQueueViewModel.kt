package com.qme.mobile.ui.queue

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.HistoryEntryResponse
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
        repo.joinQueue(queueCode) { result, err ->
            // Remap the "already in queue" error to the required message
            val mappedError = if (err?.contains("already") == true || err?.contains("not accepting") == true) {
                "You are only allowed to join up to 1 queue."
            } else {
                err
            }
            onResult(result, mappedError)
        }
    }

    fun loadRecentQueues(onResult: (List<HistoryEntryResponse>, Boolean) -> Unit) {
        repo.getHistory { list, _ ->
            val all = list ?: emptyList()
            onResult(all.take(3), all.size > 3)
        }
    }
}