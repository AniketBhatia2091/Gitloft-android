package com.example.gitloftandroid.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class JwtUtilsTest {

    private fun createMockJwt(sub: String, expEpochSeconds: Long): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"$sub\",\"exp\":$expEpochSeconds}".toByteArray())
        val signature = "mockSignature"
        return "$header.$payload.$signature"
    }

    @Test
    fun testDecodeValidPayload() {
        val nowSeconds = System.currentTimeMillis() / 1000
        val jwt = createMockJwt("user-123", nowSeconds + 3600)

        val payload = JwtUtils.decodePayload(jwt)
        assertNotNull(payload)
        assertTrue(payload!!.contains("user-123"))
        assertTrue(payload.contains("\"exp\":${nowSeconds + 3600}"))
    }

    @Test
    fun testGetSubject() {
        val jwt = createMockJwt("c49a7102-1234-5678-9abc-def012345678", 1800000000L)
        val sub = JwtUtils.getSubject(jwt)
        assertEquals("c49a7102-1234-5678-9abc-def012345678", sub)
    }

    @Test
    fun testGetExpirationEpochSeconds() {
        val exp = 1899999999L
        val jwt = createMockJwt("test-user", exp)
        val extractedExp = JwtUtils.getExpirationEpochSeconds(jwt)
        assertEquals(exp, extractedExp)
        assertEquals(exp * 1000L, JwtUtils.getExpirationEpochMillis(jwt))
    }

    @Test
    fun testIsExpired_PastToken() {
        val pastSeconds = (System.currentTimeMillis() / 1000) - 300 // 5 minutes ago
        val jwt = createMockJwt("expired-user", pastSeconds)
        assertTrue("Past token should be reported as expired", JwtUtils.isExpired(jwt))
    }

    @Test
    fun testIsExpired_NearExpiryWithinBuffer() {
        val nearExpirySeconds = (System.currentTimeMillis() / 1000) + 30 // expires in 30s
        val jwt = createMockJwt("near-expiry-user", nearExpirySeconds)
        // With default 60s buffer, 30s remaining should be treated as expired
        assertTrue("Token expiring within buffer should be reported as expired", JwtUtils.isExpired(jwt, bufferSeconds = 60))
        // With 10s buffer, 30s remaining is not yet expired
        assertFalse("Token expiring beyond buffer should not be expired", JwtUtils.isExpired(jwt, bufferSeconds = 10))
    }

    @Test
    fun testIsExpired_FutureToken() {
        val futureSeconds = (System.currentTimeMillis() / 1000) + 7200 // 2 hours in future
        val jwt = createMockJwt("active-user", futureSeconds)
        assertFalse("Future token should not be expired", JwtUtils.isExpired(jwt))
    }

    @Test
    fun testMalformedTokens() {
        assertNull(JwtUtils.decodePayload(null))
        assertNull(JwtUtils.decodePayload(""))
        assertNull(JwtUtils.decodePayload("not-a-jwt"))
        assertNull(JwtUtils.decodePayload("singlepart"))
        assertNull(JwtUtils.getExpirationEpochSeconds("invalid.jwt.token"))
        assertTrue("Malformed token should be considered expired", JwtUtils.isExpired("invalid.jwt.token"))
    }

    @Test
    fun testSupabaseStyleJwt() {
        val jsonPayload = """
        {
          "iss": "https://skqiulskkduazrbnsnze.supabase.co/auth/v1",
          "sub": "9d901026-6467-4632-a506-69687e8e50be",
          "aud": "authenticated",
          "exp": 1780000000,
          "iat": 1740000000,
          "email": "user@example.com",
          "phone": "",
          "app_metadata": {
            "provider": "email",
            "providers": ["email"]
          },
          "user_metadata": {
            "user_name": "aniket"
          },
          "role": "authenticated",
          "aal": "aal1",
          "amr": [{"method": "password", "timestamp": 1740000000}],
          "session_id": "921867c2-8418-4981-b586-777e48bcecf4"
        }
        """.trimIndent()
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(jsonPayload.toByteArray())
        val jwt = "$header.$payload.signature"

        assertEquals("9d901026-6467-4632-a506-69687e8e50be", JwtUtils.getSubject(jwt))
        assertEquals(1780000000L, JwtUtils.getExpirationEpochSeconds(jwt))
        assertEquals(1780000000000L, JwtUtils.getExpirationEpochMillis(jwt))
    }
}
