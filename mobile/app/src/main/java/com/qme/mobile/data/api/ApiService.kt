package com.qme.mobile.data.api

import com.qme.mobile.data.model.request.ChangePasswordRequest
import com.qme.mobile.data.model.request.LoginRequest
import com.qme.mobile.data.model.request.RegisterRequest
import com.qme.mobile.data.model.request.UpdateProfileRequest
import com.qme.mobile.data.model.response.ApiResponse
import com.qme.mobile.data.model.response.AuthResponse
import com.qme.mobile.data.model.response.HistoryEntryResponse
import com.qme.mobile.data.model.response.JoinQueueResponse
import com.qme.mobile.data.model.response.OrganizationResponse
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.data.model.response.UserProfileResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // ── Auth ────────────────────────────────────────────
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    // ── Organizations ────────────────────────────────────
    @GET("api/organizations/code/{code}")
    fun getOrganizationByCode(@Path("code") code: String): Call<ApiResponse<OrganizationResponse>>

    // ── Queue (Customer) ─────────────────────────────────
    @POST("api/queues/join/{queueCode}")
    fun joinQueue(@Path("queueCode") queueCode: String): Call<ApiResponse<JoinQueueResponse>>

    @GET("api/queues/my")
    fun getMyQueue(): Call<ApiResponse<QueueDetailsResponse>>

    @DELETE("api/queues/{entryId}/cancel")
    fun cancelQueue(@Path("entryId") entryId: String): Call<ApiResponse<Void>>

    // ── History ──────────────────────────────────────────
    @GET("api/history")
    fun getHistory(): Call<ApiResponse<List<HistoryEntryResponse>>>

    // ── User Profile ─────────────────────────────────────
    @GET("api/users/me")
    fun getProfile(): Call<ApiResponse<UserProfileResponse>>

    @PUT("api/users/me")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserProfileResponse>>

    @PUT("api/users/me/password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ApiResponse<Void>>
}
