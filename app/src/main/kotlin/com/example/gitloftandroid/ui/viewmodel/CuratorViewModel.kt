package com.example.gitloftandroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.CustomLink
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.GitHubUser
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.StoredRepo
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.model.supabase.ProfileStats
import com.example.gitloftandroid.data.model.supabase.StoredRepoSnapshot
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.data.repository.GitHubRepository
import com.example.gitloftandroid.data.repository.LocalDataStore
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CuratorViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val localDataStore = LocalDataStore.getInstance(application)
    private val supabaseClient = SupabaseClient.getInstance(application)
    private val gitHubRepository = GitHubRepository(tokenStorage)
    private val sessionViewModel = SessionViewModel.getInstance(application)

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser.asStateFlow()

    private val _isHydratingUser = MutableStateFlow(false)
    val isHydratingUser: StateFlow<Boolean> = _isHydratingUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _profileStats = MutableStateFlow<ProfileStats?>(null)
    val profileStats: StateFlow<ProfileStats?> = _profileStats.asStateFlow()

    private val _isShowcaseLive = MutableStateFlow(false)
    val isShowcaseLive: StateFlow<Boolean> = _isShowcaseLive.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _publishSuccess = MutableStateFlow(false)
    val publishSuccess: StateFlow<Boolean> = _publishSuccess.asStateFlow()

    val allRepos: StateFlow<List<StoredRepo>> = localDataStore.storedReposFlow

    val pinLimit: Int
        get() = if (sessionViewModel.profile.value?.isPro == true) 6 else 3

    init {
        hydrateCurrentUser()
    }

    fun hydrateCurrentUser(force: Boolean = false) {
        if (_isHydratingUser.value) return
        if (!force && _currentUser.value != null) return

        viewModelScope.launch {
            _isHydratingUser.value = true
            _error.value = null

            val profile = sessionViewModel.profile.value
            val profileUsername = profile?.username?.takeIf { it.isNotBlank() }
            val token = tokenStorage.getGitHubToken()

            try {
                val user = if (!token.isNullOrBlank() && token != "manual_mode") {
                    try {
                        gitHubRepository.getUser()
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("401") || msg.contains("403")) {
                            tokenStorage.clearGitHubToken()
                            if (!profileUsername.isNullOrBlank()) {
                                gitHubRepository.getUserByUsername(profileUsername)
                            } else {
                                null
                            }
                        } else {
                            throw e
                        }
                    }
                } else if (!profileUsername.isNullOrBlank()) {
                    gitHubRepository.getUserByUsername(profileUsername)
                } else {
                    null
                }

                if (user == null) {
                    _currentUser.value = null
                    _error.value = "Link your GitHub account to load your repositories and profile."
                    return@launch
                }

                // Apply local / Supabase profile overrides
                val userWithOverrides = user.copy(
                    name = profile?.fullName?.takeIf { it.isNotBlank() } ?: user.name,
                    avatarUrl = profile?.avatarUrl?.takeIf { it.startsWith("http") } ?: user.avatarUrl,
                    bio = profile?.bio?.takeIf { it.isNotBlank() } ?: user.bio
                )

                _currentUser.value = userWithOverrides

                val username = userWithOverrides.login
                try {
                    _profileStats.value = supabaseClient.fetchStats(username)
                } catch (e: Exception) {
                    _profileStats.value = null
                }
                _isShowcaseLive.value = try { supabaseClient.checkShowcaseExists(username) } catch (e: Exception) { false }

                // Check showcase bio
                val showcase = try { supabaseClient.fetchShowcase(username) } catch (e: Exception) { null }
                if (!showcase?.bio.isNullOrBlank()) {
                    _currentUser.value = _currentUser.value?.copy(bio = showcase?.bio)
                }

                syncRepos()
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to load GitHub user"
            } finally {
                _isHydratingUser.value = false
            }
        }
    }

    fun syncRepos() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val profileUsername = sessionViewModel.profile.value?.username?.takeIf { it.isNotBlank() }
                ?: _currentUser.value?.login?.takeIf { it.isNotBlank() }
            val token = tokenStorage.getGitHubToken()

            if ((token.isNullOrBlank() || token == "manual_mode") && profileUsername.isNullOrBlank()) {
                _isLoading.value = false
                return@launch
            }

            try {
                val repos = if (!token.isNullOrBlank() && token != "manual_mode") {
                    try {
                        gitHubRepository.getRepos()
                    } catch (e: Exception) {
                        if (!profileUsername.isNullOrBlank()) {
                            gitHubRepository.getReposByUsername(profileUsername)
                        } else {
                            throw e
                        }
                    }
                } else if (!profileUsername.isNullOrBlank()) {
                    gitHubRepository.getReposByUsername(profileUsername)
                } else {
                    emptyList()
                }

                localDataStore.syncPublicRepos(repos)
                refreshProfileStats()
            } catch (e: Exception) {
                _error.value = "Repo sync failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun linkGitHubAccount(token: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isHydratingUser.value = true
            _error.value = null
            try {
                val trimmed = token.trim()
                val isToken = com.example.gitloftandroid.util.InputValidator.isValidGitHubToken(trimmed)
                if (!isToken) {
                    val err = "Authentication required: Raw usernames cannot be linked without verification. Please authorize via GitHub OAuth or provide a valid Personal Access Token (ghp_... or github_pat_...)."
                    _error.value = err
                    onResult(false, err)
                    return@launch
                }

                tokenStorage.saveGitHubToken(trimmed)
                val ghUser = gitHubRepository.getUser()
                tokenStorage.saveProfileUsername(ghUser.login)

                _currentUser.value = ghUser
                sessionViewModel.setRole(com.example.gitloftandroid.data.model.UserRole.DEVELOPER, ghUser.login)
                hydrateCurrentUser(force = true)
                syncRepos()
                refreshProfileStats()
                onResult(true, null)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to verify GitHub token."
                _error.value = err
                onResult(false, err)
            } finally {
                _isHydratingUser.value = false
            }
        }
    }

    fun refreshProfileStats() {
        val username = _currentUser.value?.login ?: return
        viewModelScope.launch {
            try {
                _profileStats.value = supabaseClient.fetchStats(username)
                _isShowcaseLive.value = supabaseClient.checkShowcaseExists(username)
            } catch (e: Exception) { }
        }
    }

    fun togglePin(repo: StoredRepo) {
        val currentPinned = localDataStore.getPinnedRepos(pinLimit)
        if (!repo.isPinned && currentPinned.size >= pinLimit) {
            _error.value = "Pin limit reached ($pinLimit repos). Upgrade to Pro to pin up to 6!"
            return
        }

        val updated = repo.copy(
            isPinned = !repo.isPinned,
            orderIndex = if (!repo.isPinned) currentPinned.size else 0
        )
        localDataStore.updateRepo(updated)
    }

    fun updateRepoCustomization(
        repoId: Int,
        customDesc: String?,
        linkType: String?,
        linkUrl: String?,
        hideGitHub: Boolean
    ) {
        val repo = localDataStore.getStoredRepos().firstOrNull { it.id == repoId } ?: return
        val updated = repo.copy(
            customDescription = customDesc,
            customLinkType = linkType,
            customLinkUrl = linkUrl,
            hideGitHubLink = hideGitHub
        )
        localDataStore.updateRepo(updated)
    }

    fun moveRepo(fromIndex: Int, toIndex: Int) {
        val pinned = localDataStore.getPinnedRepos(pinLimit).toMutableList()
        if (fromIndex in pinned.indices && toIndex in pinned.indices) {
            val item = pinned.removeAt(fromIndex)
            pinned.add(toIndex, item)
            val updatedAll = localDataStore.getStoredRepos().toMutableList()
            pinned.forEachIndexed { index, repo ->
                val allIdx = updatedAll.indexOfFirst { it.id == repo.id }
                if (allIdx != -1) {
                    updatedAll[allIdx] = updatedAll[allIdx].copy(orderIndex = index)
                }
            }
            localDataStore.saveStoredRepos(updatedAll)
        }
    }

    fun publishShowcase() {
        val user = _currentUser.value ?: return
        val pinned = localDataStore.getPinnedRepos(pinLimit)

        if (pinned.isEmpty()) {
            _error.value = "Pin at least one public repository before publishing."
            return
        }

        viewModelScope.launch {
            _isPublishing.value = true
            _publishSuccess.value = false
            _error.value = null

            val snapshots = pinned.map { repo ->
                StoredRepoSnapshot(
                    id = repo.id,
                    name = repo.name,
                    description = repo.originalDescription,
                    customDescription = repo.customDescription,
                    language = repo.language,
                    stargazersCount = repo.stargazersCount,
                    htmlUrl = repo.htmlUrlString,
                    customLinkUrl = repo.customLinkUrl,
                    customLinkType = repo.customLinkType,
                    hideGitHubLink = repo.hideGitHubLink
                )
            }

            try {
                supabaseClient.publishShowcase(
                    username = user.login,
                    fullName = user.name,
                    avatarUrl = user.avatarUrl,
                    bio = user.bio,
                    pinnedRepos = snapshots
                )
                _publishSuccess.value = true
                _isShowcaseLive.value = true
                delay(2500)
                _publishSuccess.value = false
            } catch (e: Exception) {
                _error.value = "Publish failed: ${e.localizedMessage}"
            } finally {
                _isPublishing.value = false
            }
        }
    }

    fun saveDeveloperProfile(fullName: String, avatarUrl: String, bio: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val authUid = try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { UUID.randomUUID() }
                val username = _currentUser.value?.login ?: sessionViewModel.profile.value?.username

                supabaseClient.upsertProfile(
                    id = authUid,
                    username = username,
                    role = UserRole.DEVELOPER,
                    fullName = fullName,
                    avatarUrl = avatarUrl,
                    bio = bio
                )

                _currentUser.value = _currentUser.value?.copy(
                    name = fullName,
                    avatarUrl = if (avatarUrl.startsWith("http")) avatarUrl else _currentUser.value?.avatarUrl ?: "",
                    bio = bio
                )

                sessionViewModel.hydrateProfile()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _error.value = "Failed to save profile: ${e.localizedMessage}"
                onComplete?.invoke(false)
            }
        }
    }

    fun buildShowcaseProfile(): ShowcaseProfile? {
        val user = _currentUser.value ?: return null
        val pinned = localDataStore.getPinnedRepos(pinLimit)
        val repos = pinned.map { stored ->
            GitHubRepo(
                id = stored.id,
                name = stored.name,
                description = stored.originalDescription,
                customDescription = stored.customDescription,
                language = stored.language,
                stargazersCount = stored.stargazersCount,
                isPrivate = stored.isPrivate,
                htmlUrl = stored.htmlUrlString,
                customLink = stored.customLink,
                hideGitHubLink = stored.hideGitHubLink
            )
        }
        return ShowcaseProfile(
            id = UUID.randomUUID(),
            user = user,
            pinnedRepos = repos
        )
    }
}
