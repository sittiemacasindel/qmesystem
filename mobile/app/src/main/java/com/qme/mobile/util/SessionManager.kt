package com.qme.mobile.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the authenticated user session using SharedPreferences.
 * Acts as the single source of truth for auth state across the app.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME  = "qme_session"
        private const val KEY_TOKEN  = "token"
        private const val KEY_NAME   = "name"
        private const val KEY_EMAIL  = "email"
        private const val KEY_ROLE   = "role"
        private const val KEY_USER_ID = "userId"
        private const val KEY_ORG   = "organization"
    }

    fun saveSession(token: String, userId: String, name: String, email: String, role: String, organization: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .putString(KEY_ORG, organization)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getName(): String? = prefs.getString(KEY_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getOrganization(): String? = prefs.getString(KEY_ORG, null)
    fun isLoggedIn(): Boolean = getToken() != null

    fun updateName(name: String) = prefs.edit().putString(KEY_NAME, name).apply()

    fun clearSession() = prefs.edit().clear().apply()
}
