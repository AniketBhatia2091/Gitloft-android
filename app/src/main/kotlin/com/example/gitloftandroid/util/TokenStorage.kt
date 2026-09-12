package com.example.gitloftandroid.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "gitloft-secure-prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.w("TokenStorage", "EncryptedSharedPreferences failed, falling back to standard prefs", e)
            context.getSharedPreferences("gitloft-secure-prefs-fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val GITHUB_TOKEN_KEY = "github_token"
        private const val SUPABASE_JWT_KEY = "supabase_jwt"
        private const val PROFILE_USERNAME_KEY = "profile_username"
    }

    fun saveProfileUsername(username: String) {
        prefs.edit().putString(PROFILE_USERNAME_KEY, username).apply()
    }

    fun getProfileUsername(): String? {
        return prefs.getString(PROFILE_USERNAME_KEY, null)
    }

    fun clearProfileUsername() {
        prefs.edit().remove(PROFILE_USERNAME_KEY).apply()
    }

    fun saveGitHubToken(token: String) {
        prefs.edit().putString(GITHUB_TOKEN_KEY, token).apply()
    }

    fun getGitHubToken(): String? {
        return prefs.getString(GITHUB_TOKEN_KEY, null)
    }

    fun clearGitHubToken() {
        prefs.edit().remove(GITHUB_TOKEN_KEY).apply()
    }

    fun saveSupabaseJwt(token: String) {
        prefs.edit().putString(SUPABASE_JWT_KEY, token).apply()
    }

    fun getSupabaseJwt(): String? {
        return prefs.getString(SUPABASE_JWT_KEY, null)
    }

    fun clearSupabaseJwt() {
        prefs.edit().remove(SUPABASE_JWT_KEY).apply()
    }
}
