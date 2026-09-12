package com.example.gitloftandroid.data.model

import com.example.gitloftandroid.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShowcaseProfile(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val user: GitHubUser,
    val pinnedRepos: List<GitHubRepo> = emptyList(),
    val customTheme: String? = null,
    val rating: Int? = null
)
