package com.qme.mobile.data.model.response

data class HistoryEntryResponse(
    val entryId: String,
    val organizationId: String,
    val organizationName: String,
    val queueCode: String,
    val queueNumber: Int,
    val status: String,      // SERVED | CANCELLED | SKIPPED
    val joinedAt: String?,
    val completedAt: String?
)
