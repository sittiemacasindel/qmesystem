package com.qme.mobile.data.model.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)
