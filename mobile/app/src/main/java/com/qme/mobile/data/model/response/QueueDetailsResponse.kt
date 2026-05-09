package com.qme.mobile.data.model.response

data class QueueDetailsResponse(
    val entryId: String,
    val organization: OrganizationResponse,
    val queueNumber: Int,
    val positionInLine: Int,
    val computedStatus: String,   // BEING_SERVED | NEXT_IN_LINE | WAITING
    val currentlyServingNumber: Int?,
    val estimatedWaitTime: Int?,
    val joinedAt: String?
)
