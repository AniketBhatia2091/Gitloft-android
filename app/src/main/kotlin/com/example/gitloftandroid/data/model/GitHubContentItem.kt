package com.example.gitloftandroid.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val type: String, // "file" or "dir"
    val size: Long? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null
) {
    val isDirectory: Boolean get() = type == "dir"

    val isImage: Boolean
        get() {
            if (isDirectory) return false
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in listOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico")
        }
}
