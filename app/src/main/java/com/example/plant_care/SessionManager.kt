package com.example.plant_care

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val USER_EMAIL = "email"
        private const val USER_NAME = "username"
        private const val JWT_TOKEN = "jwt_token"
        private const val IS_LOGGED_IN = "isLoggedIn"
    }

    fun saveSession(email: String, username: String, token: String) {
        prefs.edit().apply {
            putString(USER_EMAIL, email)
            putString(USER_NAME, username)
            putString(JWT_TOKEN, token)
            putBoolean(IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getEmail(): String? = prefs.getString(USER_EMAIL, null)
    fun getUsername(): String? = prefs.getString(USER_NAME, null)
    fun getToken(): String? = prefs.getString(JWT_TOKEN, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }
}