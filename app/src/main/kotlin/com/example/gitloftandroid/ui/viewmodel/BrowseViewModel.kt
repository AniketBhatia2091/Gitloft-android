package com.example.gitloftandroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.supabase.ShowcaseProfileResponse
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val supabaseClient = SupabaseClient.getInstance(application)

    private val _rawShowcases = MutableStateFlow<List<ShowcaseProfileResponse>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val searchText = MutableStateFlow("")
    val selectedFilter = MutableStateFlow("ALL")

    val filters = listOf("ALL", "SWIFT", "KOTLIN", "PYTHON", "JAVASCRIPT", "RUST", "GO", "TYPESCRIPT")

    val filteredShowcases: StateFlow<List<ShowcaseProfileResponse>> = combine(
        _rawShowcases,
        searchText,
        selectedFilter
    ) { showcases, query, filter ->
        var list = showcases
        if (query.isNotBlank()) {
            val q = query.trim()
            list = list.filter {
                it.username.contains(q, ignoreCase = true) ||
                        (it.fullName?.contains(q, ignoreCase = true) == true) ||
                        (it.bio?.contains(q, ignoreCase = true) == true)
            }
        }
        if (filter != "ALL") {
            list = list.filter { profile ->
                profile.pinnedRepos.any { repo ->
                    repo.language?.equals(filter, ignoreCase = true) == true
                }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadShowcases()
    }

    fun loadShowcases() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = supabaseClient.fetchGlobalShowcases(limit = 50)
                _rawShowcases.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to load showcases"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
