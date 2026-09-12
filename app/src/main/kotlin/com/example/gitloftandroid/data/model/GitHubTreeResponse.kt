package com.example.gitloftandroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubTreeResponse(
    val sha: String? = null,
    val url: String? = null,
    val tree: List<TreeItem> = emptyList(),
    val truncated: Boolean = false
)

@Serializable
data class TreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val url: String? = null,
    val size: Long? = null
)
