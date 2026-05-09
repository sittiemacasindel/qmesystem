package com.qme.mobile.ui.auth

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.repository.AuthRepository

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            onResult(false, "Please fill in all fields.")
            return
        }
        if (password != confirmPassword) {
            onResult(false, "Passwords do not match.")
            return
        }
        if (password.length < 6) {
            onResult(false, "Password must be at least 6 characters.")
            return
        }
        repo.register(name.trim(), email.trim(), password, onResult)
    }
}
