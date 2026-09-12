package com.example.gitloftandroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.DailyEventCount
import com.example.gitloftandroid.data.model.RepoClickCount
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val supabaseClient = SupabaseClient.getInstance(application)

    private val _totalViews = MutableStateFlow(0)
    val totalViews: StateFlow<Int> = _totalViews.asStateFlow()

    private val _totalClicks = MutableStateFlow(0)
    val totalClicks: StateFlow<Int> = _totalClicks.asStateFlow()

    private val _shortlistCount = MutableStateFlow(0)
    val shortlistCount: StateFlow<Int> = _shortlistCount.asStateFlow()

    private val _weeklyData = MutableStateFlow<List<DailyEventCount>>(emptyList())
    val weeklyData: StateFlow<List<DailyEventCount>> = _weeklyData.asStateFlow()

    private val _topRepos = MutableStateFlow<List<RepoClickCount>>(emptyList())
    val topRepos: StateFlow<List<RepoClickCount>> = _topRepos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAnalytics(username: String) {
        if (username.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _totalViews.value = supabaseClient.fetchTotalViews(username)
                _totalClicks.value = supabaseClient.fetchTotalClicks(username)
                _shortlistCount.value = supabaseClient.fetchShortlistCount(username)
                _weeklyData.value = supabaseClient.fetchWeeklyEvents(username)
                _topRepos.value = supabaseClient.fetchTopRepos(username)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to load telemetry"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
