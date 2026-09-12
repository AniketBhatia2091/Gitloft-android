package com.example.gitloftandroid.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.GitHubUser
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.supabase.ShowcaseProfileResponse
import com.example.gitloftandroid.data.repository.GitHubRepository
import com.example.gitloftandroid.data.repository.SupabaseRepository
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShowcaseUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val githubUser: GitHubUser? = null,
    val showcaseProfile: ShowcaseProfile? = null,
    val isSignedIn: Boolean = false
)

class ShowcaseViewModel(
    private val gitHubRepository: GitHubRepository,
    private val supabaseRepository: SupabaseRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowcaseUiState())
    val uiState: StateFlow<ShowcaseUiState> = _uiState.asStateFlow()

    init {
        loadAppState()
    }

    private fun loadAppState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Check if we have a GitHub token
                val gitHubToken = tokenStorage.getGitHubToken()
                val isSignedIn = !gitHubToken.isNullOrEmpty()
                _uiState.update { it.copy(isSignedIn = isSignedIn) }

                if (isSignedIn) {
                    // Fetch user data
                    val githubUser = gitHubRepository.getUser()
                    // Fetch showcase profile from Supabase using the GitHub username
                    val showcaseProfileResponse = supabaseRepository.fetchShowcase(githubUser.login)
                    val showcaseProfile = showcaseProfileResponse?.let { toShowcaseProfile(it, githubUser) }
                    _uiState.update {
                        it.copy(
                            githubUser = githubUser,
                            showcaseProfile = showcaseProfile,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            githubUser = null,
                            showcaseProfile = null,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.localizedMessage ?: "Unknown error",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun toShowcaseProfile(response: ShowcaseProfileResponse, githubUser: GitHubUser): ShowcaseProfile {
        return response.asShowcaseProfile.copy(user = githubUser)
    }

    fun reload() {
        loadAppState()
    }

    fun signIn() {
        loadAppState()
    }

    fun signOut() {
        tokenStorage.clearGitHubToken()
        loadAppState()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val tokenStorage = TokenStorage(context)
                    val gitHubRepository = GitHubRepository(tokenStorage)
                    val supabaseRepository = SupabaseRepository(context, tokenStorage)
                    return ShowcaseViewModel(gitHubRepository, supabaseRepository, tokenStorage) as T
                }
            }
    }
}
