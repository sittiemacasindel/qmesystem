package com.qme.mobile.data.model.request

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "CUSTOMER"
)
