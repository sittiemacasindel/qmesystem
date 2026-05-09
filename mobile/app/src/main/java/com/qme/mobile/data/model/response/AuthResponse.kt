package com.qme.mobile.data.model.response

data class AuthResponse(
    val token: String,
    val tokenType: String,
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val organization: String?
)
