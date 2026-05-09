package com.qme.mobile.ui.profile

import androidx.lifecycle.ViewModel
import com.qme.mobile.data.model.response.UserProfileResponse
import com.qme.mobile.data.repository.UserRepository

class ProfileViewModel(private val repo: UserRepository) : ViewModel() {

    fun loadProfile(onResult: (UserProfileResponse?, String?) -> Unit) {
        repo.getProfile(onResult)
    }

    fun updateProfile(name: String, organization: String?, onResult: (Boolean, String?) -> Unit) {
        if (name.isBlank()) {
            onResult(false, "Name cannot be empty.")
            return
        }
        repo.updateProfile(name.trim(), organization?.trim(), onResult)
    }

    fun changePassword(current: String, newPass: String, confirm: String, onResult: (Boolean, String?) -> Unit) {
        if (current.isBlank() || newPass.isBlank() || confirm.isBlank()) {
            onResult(false, "Please fill in all password fields.")
            return
        }
        if (newPass != confirm) {
            onResult(false, "New passwords do not match.")
            return
        }
        if (newPass.length < 6) {
            onResult(false, "New password must be at least 6 characters.")
            return
        }
        repo.changePassword(current, newPass, onResult)
    }
}
