package com.qme.mobile.data.repository

import com.qme.mobile.data.api.ApiService
import com.qme.mobile.data.model.request.LoginRequest
import com.qme.mobile.data.model.request.RegisterRequest
import com.qme.mobile.data.model.response.AuthResponse
import com.qme.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Handles authentication: register, login, logout.
 * On success, persists the session via SessionManager.
 */
class AuthRepository(
    private val api: ApiService,
    private val session: SessionManager
) {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        api.login(LoginRequest(email, password)).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        session.saveSession(
                            token        = body.token,
                            userId       = body.userId,
                            name         = body.name,
                            email        = body.email,
                            role         = body.role,
                            organization = body.organization
                        )
                        onResult(true, null)
                    } else {
                        onResult(false, "Unexpected empty response.")
                    }
                } else {
                    val msg = when (response.code()) {
                        400  -> "Invalid email or password."
                        401  -> "Invalid credentials."
                        500  -> "Server error. Please try again later."
                        else -> "Login failed (${response.code()})."
                    }
                    onResult(false, msg)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                onResult(false, "Network error. Please check your connection.")
            }
        })
    }

    fun register(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        api.register(RegisterRequest(name = name, email = email, password = password))
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            session.saveSession(
                                token        = body.token,
                                userId       = body.userId,
                                name         = body.name,
                                email        = body.email,
                                role         = body.role,
                                organization = body.organization
                            )
                            onResult(true, null)
                        } else {
                            onResult(false, "Unexpected empty response.")
                        }
                    } else {
                        val msg = when (response.code()) {
                            400  -> "Registration failed. Please check your information."
                            409  -> "An account with this email already exists."
                            500  -> "Server error. Please try again later."
                            else -> "Registration failed (${response.code()})."
                        }
                        onResult(false, msg)
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    onResult(false, "Network error. Please check your connection.")
                }
            })
    }

    fun logout() = session.clearSession()
}
