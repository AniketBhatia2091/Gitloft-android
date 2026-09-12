package com.example.gitloftandroid.data.model.supabase

import com.example.gitloftandroid.data.model.CustomLink
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.GitHubUser
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.util.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.abs

@Serializable
data class ShowcaseProfileResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("pinned_repos") val pinnedRepos: List<StoredRepoSnapshot> = emptyList()
) {
    val asShowcaseProfile: ShowcaseProfile
        get() {
            val user = GitHubUser(
                id = abs(username.hashCode()).toLong(),
                login = username,
                avatarUrl = avatarUrl ?: "https://github.com/identicons/$username.png",
                name = fullName,
                bio = bio
            )
            val repos = pinnedRepos.map { repo ->
                GitHubRepo(
                    id = repo.id,
                    name = repo.name,
                    description = repo.description,
                    customDescription = repo.customDescription,
                    language = repo.language,
                    stargazersCount = repo.stargazersCount,
                    isPrivate = false,
                    htmlUrl = repo.htmlUrl.ifBlank { "https://github.com/$username/${repo.name}" },
                    customLink = repo.customLinkUrl?.let { url ->
                        val linkType = CustomLink.LinkType.fromString(repo.customLinkType) ?: CustomLink.LinkType.CUSTOM
                        CustomLink(type = linkType, url = url)
                    },
                    hideGitHubLink = repo.hideGitHubLink
                )
            }
            return ShowcaseProfile(
                id = id,
                user = user,
                pinnedRepos = repos,
                rating = rating
            )
        }
}
