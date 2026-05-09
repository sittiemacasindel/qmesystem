package com.qme.mobile.data.model.response

data class OrganizationResponse(
    val id: String,
    val name: String,
    val queueCode: String,
    val status: String,
    val openingHours: String?,
    val closingHours: String?,
    val waitTimeMin: Int?,
    val waitTimeMax: Int?,
    val location: String?,
    val contactNumber: String?,
    val totalWaitingCustomers: Int?,
    val totalServedToday: Int?
)
