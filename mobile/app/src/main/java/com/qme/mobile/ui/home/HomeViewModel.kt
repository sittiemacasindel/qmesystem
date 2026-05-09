package com.qme.mobile.ui.home

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.data.repository.QueueRepository

class HomeViewModel(private val repo: QueueRepository) : ViewModel() {

    fun loadMyQueue(onResult: (QueueDetailsResponse?, String?) -> Unit) {
        repo.getMyQueue(onResult)
    }
}
