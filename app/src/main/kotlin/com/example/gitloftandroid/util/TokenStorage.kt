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
        private const val SUPABASE_REFRESH_TOKEN_KEY = "supabase_refresh_token"
        private const val SUPABASE_EXPIRES_AT_KEY = "supabase_expires_at"
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
        val editor = prefs.edit().putString(SUPABASE_JWT_KEY, token)
        val expMillis = JwtUtils.getExpirationEpochMillis(token)
        if (expMillis != null && expMillis > 0) {
            editor.putLong(SUPABASE_EXPIRES_AT_KEY, expMillis)
        }
        editor.apply()
    }

    fun getSupabaseJwt(): String? {
        return prefs.getString(SUPABASE_JWT_KEY, null)
    }

    fun saveSupabaseRefreshToken(refreshToken: String) {
        prefs.edit().putString(SUPABASE_REFRESH_TOKEN_KEY, refreshToken).apply()
    }

    fun getSupabaseRefreshToken(): String? {
        return prefs.getString(SUPABASE_REFRESH_TOKEN_KEY, null)
    }

    fun clearSupabaseRefreshToken() {
        prefs.edit().remove(SUPABASE_REFRESH_TOKEN_KEY).apply()
    }

    fun saveSupabaseSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long? = null,
        expiresAtEpochSeconds: Long? = null
    ) {
        val editor = prefs.edit()
        editor.putString(SUPABASE_JWT_KEY, accessToken)

        if (!refreshToken.isNullOrBlank()) {
            editor.putString(SUPABASE_REFRESH_TOKEN_KEY, refreshToken)
        }

        val expiresAtMillis = when {
            expiresAtEpochSeconds != null && expiresAtEpochSeconds > 0 -> expiresAtEpochSeconds * 1000L
            expiresInSeconds != null && expiresInSeconds > 0 -> System.currentTimeMillis() + (expiresInSeconds * 1000L)
            else -> JwtUtils.getExpirationEpochMillis(accessToken)
                ?: (System.currentTimeMillis() + 3600_000L)
        }
        editor.putLong(SUPABASE_EXPIRES_AT_KEY, expiresAtMillis)
        editor.apply()
    }

    fun getSupabaseExpiresAt(): Long {
        return prefs.getLong(SUPABASE_EXPIRES_AT_KEY, 0L)
    }

    fun isSupabaseTokenExpired(bufferSeconds: Long = 60): Boolean {
        val jwt = getSupabaseJwt()
        if (jwt.isNullOrBlank() || jwt == "demo_mode") return false

        val expiresAt = getSupabaseExpiresAt()
        if (expiresAt > 0) {
            val now = System.currentTimeMillis()
            val threshold = expiresAt - (bufferSeconds * 1000L)
            return now >= threshold
        }

        // Fallback: check exp claim directly from JWT
        return JwtUtils.isExpired(jwt, bufferSeconds)
    }

    fun clearSupabaseSession() {
        prefs.edit()
            .remove(SUPABASE_JWT_KEY)
            .remove(SUPABASE_REFRESH_TOKEN_KEY)
            .remove(SUPABASE_EXPIRES_AT_KEY)
            .apply()
    }

    fun clearSupabaseJwt() {
        clearSupabaseSession()
    }
}
