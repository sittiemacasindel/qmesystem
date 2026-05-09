package com.qme.mobile.data.model.response

data class JoinQueueResponse(
    val entryId: String,
    val queueNumber: Int,
    val positionInLine: Int,
    val status: String,
    val estimatedWaitTimeMin: Int?,
    val estimatedWaitTimeMax: Int?
)
