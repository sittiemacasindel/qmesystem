package com.qme.mobile.data.repository

import com.qme.mobile.data.api.ApiService
import com.qme.mobile.data.model.request.ChangePasswordRequest
import com.qme.mobile.data.model.request.UpdateProfileRequest
import com.qme.mobile.data.model.response.ApiResponse
import com.qme.mobile.data.model.response.UserProfileResponse
import com.qme.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Handles user profile retrieval, update, and password change.
 */
class UserRepository(
    private val api: ApiService,
    private val session: SessionManager
) {

    fun getProfile(onResult: (UserProfileResponse?, String?) -> Unit) {
        api.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileResponse>>,
                response: Response<ApiResponse<UserProfileResponse>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    onResult(response.body()!!.data, null)
                } else {
                    onResult(null, "Failed to load profile (${response.code()}).")
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                onResult(null, "Network error. Please check your connection.")
            }
        })
    }

    fun updateProfile(fullName: String, organization: String?, onResult: (Boolean, String?) -> Unit) {
        api.updateProfile(UpdateProfileRequest(fullName, organization))
            .enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserProfileResponse>>,
                    response: Response<ApiResponse<UserProfileResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.data?.let { session.updateName(it.name) }
                        onResult(true, null)
                    } else {
                        onResult(false, "Failed to update profile (${response.code()}).")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                    onResult(false, "Network error. Please check your connection.")
                }
            })
    }

    fun changePassword(current: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        api.changePassword(ChangePasswordRequest(current, newPass))
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    if (response.isSuccessful) {
                        onResult(true, null)
                    } else {
                        val msg = when (response.code()) {
                            400  -> "Current password is incorrect."
                            401  -> "Session expired. Please log in again."
                            500  -> "Server error. Please try again later."
                            else -> "Failed to change password (${response.code()})."
                        }
                        onResult(false, msg)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    onResult(false, "Network error. Please check your connection.")
                }
            })
    }
}
