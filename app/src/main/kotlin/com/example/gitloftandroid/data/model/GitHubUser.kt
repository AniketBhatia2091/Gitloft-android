package com.example.gitloftandroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUser(
    val id: Long = 0,
    @SerialName("login") val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val name: String? = null,
    val bio: String? = null,
    @SerialName("public_repos") val publicRepos: Int? = 0,
    val followers: Int? = 0,
    val following: Int? = 0,
    // Gitloft-specific overrides (local-only, not from API)
    var customBio: String? = null,
    var customHeadline: String? = null
) {
    val username: String
        get() = login
}
