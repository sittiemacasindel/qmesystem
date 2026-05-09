package com.qme.mobile.data.model.request

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
