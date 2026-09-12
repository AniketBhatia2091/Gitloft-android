package com.example.gitloftandroid.data.model

import com.example.gitloftandroid.util.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class UserRole(val value: String) {
    @SerialName("developer")
    DEVELOPER("developer"),

    @SerialName("recruiter")
    RECRUITER("recruiter");

    val displayName: String
        get() = when (this) {
            DEVELOPER -> "Developer"
            RECRUITER -> "Hiring Manager"
        }

    companion object {
        fun fromString(str: String?): UserRole? {
            return when (str?.lowercase()?.trim()) {
                "developer" -> DEVELOPER
                "recruiter" -> RECRUITER
                else -> null
            }
        }
    }
}

@Serializable
data class GitloftProfile(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val bio: String? = null,
    val role: UserRole? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val plan: String = "free"
) {
    val isPro: Boolean
        get() = plan.equals("pro", ignoreCase = true)
}

@Serializable
data class StoredRepo(
    val id: Int,
    val name: String,
    val originalDescription: String? = null,
    val language: String? = null,
    val stargazersCount: Int = 0,
    val isPrivate: Boolean = false,
    val htmlUrlString: String = "",
    // Gitloft specific fields
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
    val customDescription: String? = null,
    val hideGitHubLink: Boolean = false,
    val customLinkType: String? = null,
    val customLinkUrl: String? = null,
    val customLinkTitleOverride: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
) {
    val displayDescription: String
        get() = customDescription?.takeIf { it.isNotBlank() }
            ?: originalDescription?.takeIf { it.isNotBlank() }
            ?: "No description provided."

    val customLink: CustomLink?
        get() {
            val typeStr = customLinkType ?: return null
            val urlStr = customLinkUrl ?: return null
            val type = CustomLink.LinkType.fromString(typeStr) ?: CustomLink.LinkType.CUSTOM
            return CustomLink(type = type, url = urlStr, titleOverride = customLinkTitleOverride)
        }
}

@Serializable
data class SavedDeveloper(
    val username: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val rating: Int = 0,
    val savedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CustomLink(
    val type: LinkType,
    val url: String,
    val titleOverride: String? = null
) {
    @Serializable
    enum class LinkType(val rawValue: String) {
        @SerialName("App Store")
        APP_STORE("App Store"),

        @SerialName("Live Demo")
        LIVE_DEMO("Live Demo"),

        @SerialName("Website")
        WEBSITE("Website"),

        @SerialName("View")
        CUSTOM("View");

        companion object {
            fun fromString(str: String?): LinkType? {
                return values().firstOrNull { it.rawValue.equals(str, ignoreCase = true) }
            }
        }
    }

    val displayTitle: String
        get() = titleOverride?.takeIf { it.isNotBlank() } ?: type.rawValue
}

@Serializable
data class AnalyticsEvent(
    val event_type: String,
    val showcase_username: String,
    val repo_name: String? = null,
    val target_url: String? = null,
    val rating: Int? = null,
    val source: String = "android_app",
    val viewer_role: String = "developer",
    val viewer_username: String? = null,
    val viewer_display_name: String? = null,
    val platform: String = "android",
    val created_at: String? = null
)

@Serializable
data class DailyEventCount(
    val date: String,
    val views: Int = 0,
    val clicks: Int = 0
)

@Serializable
data class RepoClickCount(
    val repoName: String,
    val clicks: Int = 0
)
