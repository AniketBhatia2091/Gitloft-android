package com.example.gitloftandroid.util

import java.util.regex.Pattern

object InputValidator {

    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$")
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val CLASSIC_PAT_PATTERN = Pattern.compile("^ghp_[a-zA-Z0-9]{36}$")
    private val FINE_GRAINED_PAT_PATTERN = Pattern.compile("^github_pat_[a-zA-Z0-9_]{80,}$")

    /**
     * Validates whether a given string is a valid GitHub username.
     * GitHub usernames are 1-39 characters, alphanumeric with single non-consecutive hyphens,
     * and cannot begin or end with a hyphen.
     */
    fun isValidGitHubUsername(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        return USERNAME_PATTERN.matcher(username.trim()).matches()
    }

    /**
     * Validates whether a given token is a genuine GitHub Personal Access Token (Classic or Fine-Grained).
     */
    fun isValidGitHubToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val trimmed = token.trim()
        return CLASSIC_PAT_PATTERN.matcher(trimmed).matches() ||
               FINE_GRAINED_PAT_PATTERN.matcher(trimmed).matches() ||
               (trimmed.startsWith("ghp_") && trimmed.length in 38..42) ||
               (trimmed.startsWith("github_pat_") && trimmed.length >= 82) ||
               trimmed.startsWith("ghu_") ||
               trimmed.startsWith("gho_") ||
               trimmed.startsWith("ghs_")
    }

    /**
     * Validates email address syntax.
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return EMAIL_PATTERN.matcher(email.trim()).matches()
    }

    /**
     * Strips leading/trailing whitespace and control characters.
     */
    fun sanitize(input: String?): String {
        return input?.trim()?.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "") ?: ""
    }
}
