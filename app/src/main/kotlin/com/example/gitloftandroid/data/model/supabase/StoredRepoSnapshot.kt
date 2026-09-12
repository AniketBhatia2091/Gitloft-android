package com.example.gitloftandroid.data.model.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoredRepoSnapshot(
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    @SerialName("custom_description") val customDescription: String? = null,
    val language: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("custom_link_url") val customLinkUrl: String? = null,
    @SerialName("custom_link_type") val customLinkType: String? = null,
    @SerialName("hide_github_link") val hideGitHubLink: Boolean = false
)
