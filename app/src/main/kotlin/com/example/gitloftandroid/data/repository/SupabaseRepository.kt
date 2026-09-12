package com.example.gitloftandroid.data.repository

import com.example.gitloftandroid.data.model.AnalyticsEvent
import com.example.gitloftandroid.data.model.supabase.*
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.util.TokenStorage

class SupabaseRepository(
    private val context: android.content.Context,
    private val tokenStorage: TokenStorage
) {
    private val supabaseClient: SupabaseClient = SupabaseClient.getInstance(context)

    suspend fun publishShowcase(
        username: String,
        fullName: String?,
        avatarUrl: String?,
        bio: String?,
        pinnedRepos: List<StoredRepoSnapshot>
    ) {
        supabaseClient.publishShowcase(username, fullName, avatarUrl, bio, pinnedRepos)
    }

    suspend fun fetchShowcase(username: String): ShowcaseProfileResponse? {
        return supabaseClient.fetchShowcase(username)
    }

    suspend fun logEvent(event: AnalyticsEvent) {
        supabaseClient.logEvent(event)
    }

    // TODO: Add other methods as needed (fetchStats, fetchNotificationEvents, etc.)
}
