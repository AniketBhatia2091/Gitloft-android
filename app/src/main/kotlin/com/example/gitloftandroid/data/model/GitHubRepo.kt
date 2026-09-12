package com.example.gitloftandroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepo(
    val id: Int,
    val name: String,
    @SerialName("description") val description: String? = null,
    val language: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("default_branch") val defaultBranch: String? = "main",
    // Gitloft-specific customization (local-only)
    var customDescription: String? = null,
    var customLink: CustomLink? = null,
    var hideGitHubLink: Boolean = false
) {
    val displayDescription: String
        get() = customDescription ?: description ?: "No description provided."

    val ownerLogin: String?
        get() {
            val parts = htmlUrl.split("/").filter { it.isNotBlank() }
            if (parts.size >= 4) {
                return parts[2] // e.g. https: / github.com / owner / repo
            }
            return parts.getOrNull(parts.size - 2)
        }
}
