package com.qme.mobile.ui.auth

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.repository.AuthRepository

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Please enter your email and password.")
            return
        }
        repo.login(email.trim(), password, onResult)
    }
}
