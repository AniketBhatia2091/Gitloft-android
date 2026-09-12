package com.example.gitloftandroid.data.model.supabase

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStats(
    val views: Int = 0,
    val clicks: Int = 0
)
