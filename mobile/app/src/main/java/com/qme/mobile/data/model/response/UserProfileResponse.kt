package com.qme.mobile.data.model.response

data class UserProfileResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val organization: String?
)
