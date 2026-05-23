package com.qme.mobile.data.api

import com.qme.mobile.util.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Use http://10.0.2.2:8080/ for Android Emulator to reach host machine localhost.
    // Change to your LAN IP (e.g. "http://192.168.x.x:8080/") for physical device.
    private const val BASE_URL = "http://192.168.1.2:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Attaches Bearer token from SessionManager to every request
    private fun authInterceptor(sessionManager: SessionManager) = Interceptor { chain ->
        val token = sessionManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    fun create(sessionManager: SessionManager): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
