package com.example.gitloftandroid.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun testValidGitHubUsernames() {
        assertTrue(InputValidator.isValidGitHubUsername("aniketbhatia02091"))
        assertTrue(InputValidator.isValidGitHubUsername("octocat"))
        assertTrue(InputValidator.isValidGitHubUsername("torvalds"))
        assertTrue(InputValidator.isValidGitHubUsername("user-name"))
        assertTrue(InputValidator.isValidGitHubUsername("a"))
    }

    @Test
    fun testInvalidGitHubUsernames() {
        assertFalse(InputValidator.isValidGitHubUsername(null))
        assertFalse(InputValidator.isValidGitHubUsername(""))
        assertFalse(InputValidator.isValidGitHubUsername("-invalid"))
        assertFalse(InputValidator.isValidGitHubUsername("invalid-"))
        assertFalse(InputValidator.isValidGitHubUsername("inv--alid"))
        assertFalse(InputValidator.isValidGitHubUsername("user@name"))
        assertFalse(InputValidator.isValidGitHubUsername("user name"))
        assertFalse(InputValidator.isValidGitHubUsername("a".repeat(40))) // max 39
    }

    @Test
    fun testValidGitHubTokens() {
        val validClassicPat = "ghp_" + "A".repeat(36)
        val validFineGrainedPat = "github_pat_" + "B".repeat(82)

        assertTrue(InputValidator.isValidGitHubToken(validClassicPat))
        assertTrue(InputValidator.isValidGitHubToken(validFineGrainedPat))
    }

    @Test
    fun testInvalidGitHubTokens() {
        assertFalse(InputValidator.isValidGitHubToken(null))
        assertFalse(InputValidator.isValidGitHubToken(""))
        assertFalse(InputValidator.isValidGitHubToken("aniketbhatia02091")) // raw username must fail
        assertFalse(InputValidator.isValidGitHubToken("ghp_short"))
        assertFalse(InputValidator.isValidGitHubToken("github_pat_short"))
        assertFalse(InputValidator.isValidGitHubToken("manual_mode"))
    }

    @Test
    fun testEmailValidation() {
        assertTrue(InputValidator.isValidEmail("user@example.com"))
        assertTrue(InputValidator.isValidEmail("dev.person+tag@company.org"))
        assertFalse(InputValidator.isValidEmail("invalid-email"))
        assertFalse(InputValidator.isValidEmail("@domain.com"))
        assertFalse(InputValidator.isValidEmail(null))
    }
}
