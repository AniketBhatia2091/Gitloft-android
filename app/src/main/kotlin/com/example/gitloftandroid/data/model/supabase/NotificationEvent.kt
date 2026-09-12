package com.example.gitloftandroid.data.model.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("showcase_username") val showcaseUsername: String? = null,
    @SerialName("event_type") val eventType: String,
    @SerialName("repo_name") val repoName: String? = null,
    @SerialName("target_url") val targetUrl: String? = null,
    val rating: Int? = null,
    @SerialName("view_milestone") val viewMilestone: Int? = null,
    @SerialName("viewer_role") val viewerRole: String? = null,
    @SerialName("viewer_username") val viewerUsername: String? = null,
    @SerialName("viewer_display_name") val viewerDisplayName: String? = null,
    val source: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
