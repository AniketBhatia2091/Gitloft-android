package com.example.gitloftandroid.util

import org.json.JSONObject

object JwtUtils {

    /**
     * Decodes the payload section of a JWT (header.payload.signature) and returns the JSON string,
     * or null if the token is malformed.
     */
    fun decodePayload(jwt: String?): String? {
        if (jwt.isNullOrBlank()) return null
        val parts = jwt.split(".")
        if (parts.size < 2) return null

        return try {
            val payloadBase64 = parts[1]
            val padded = when (payloadBase64.length % 4) {
                2 -> "$payloadBase64=="
                3 -> "$payloadBase64="
                else -> payloadBase64
            }
            val bytes = try {
                java.util.Base64.getUrlDecoder().decode(padded)
            } catch (e: Throwable) {
                android.util.Base64.decode(
                    padded,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                )
            }
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private val EXP_REGEX = "\"exp\"\\s*:\\s*(\\d+)".toRegex()
    private val SUB_REGEX = "\"sub\"\\s*:\\s*\"([^\"]+)\"".toRegex()

    /**
     * Extracts the "exp" claim (expiration time in seconds since epoch) from the JWT payload.
     * Returns null if missing or token is invalid.
     */
    fun getExpirationEpochSeconds(jwt: String?): Long? {
        val payload = decodePayload(jwt) ?: return null
        val regexMatch = EXP_REGEX.find(payload)?.groupValues?.get(1)?.toLongOrNull()
        if (regexMatch != null) return regexMatch

        return try {
            val json = JSONObject(payload)
            if (json.has("exp")) json.getLong("exp") else null
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Extracts the "exp" claim converted to milliseconds since epoch.
     * Returns null if missing or token is invalid.
     */
    fun getExpirationEpochMillis(jwt: String?): Long? {
        val seconds = getExpirationEpochSeconds(jwt) ?: return null
        return seconds * 1000L
    }

    /**
     * Extracts the subject ("sub", usually user UUID) from the JWT payload.
     */
    fun getSubject(jwt: String?): String? {
        val payload = decodePayload(jwt) ?: return null
        val regexMatch = SUB_REGEX.find(payload)?.groupValues?.get(1)
        if (!regexMatch.isNullOrBlank()) return regexMatch

        return try {
            val json = JSONObject(payload)
            if (json.has("sub")) json.getString("sub") else null
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Checks if the token is expired or will expire within [bufferSeconds].
     */
    fun isExpired(jwt: String?, bufferSeconds: Long = 60): Boolean {
        val expMillis = getExpirationEpochMillis(jwt) ?: return true
        return System.currentTimeMillis() >= (expMillis - (bufferSeconds * 1000L))
    }
}
