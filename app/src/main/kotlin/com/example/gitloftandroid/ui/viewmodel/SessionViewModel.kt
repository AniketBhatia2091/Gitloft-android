package com.example.gitloftandroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.GitloftProfile
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.data.repository.GitHubRepository
import com.example.gitloftandroid.data.repository.LocalDataStore
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val localDataStore = LocalDataStore.getInstance(application)
    private val supabaseClient = SupabaseClient.getInstance(application)
    private val gitHubRepository = GitHubRepository(tokenStorage)

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _profile = MutableStateFlow<GitloftProfile?>(null)
    val profile: StateFlow<GitloftProfile?> = _profile.asStateFlow()

    private val _resolvedRole = MutableStateFlow<UserRole?>(null)
    val resolvedRole: StateFlow<UserRole?> = _resolvedRole.asStateFlow()

    private val _homeNavigationRequest = MutableStateFlow(0)
    val homeNavigationRequest: StateFlow<Int> = _homeNavigationRequest.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: SessionViewModel? = null

        fun getInstance(application: Application): SessionViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionViewModel(application).also { INSTANCE = it }
            }
        }
    }

    init {
        INSTANCE = this
        checkAuth()
    }

    fun checkAuth() {
        viewModelScope.launch {
            val hasGitHubToken = !tokenStorage.getGitHubToken().isNullOrBlank()
            val hasSupabaseToken = !tokenStorage.getSupabaseJwt().isNullOrBlank()

            if (hasGitHubToken || hasSupabaseToken) {
                hydrateProfile()
            } else {
                _isAuthenticated.value = false
                _isCheckingAuth.value = false
            }
        }
    }

    suspend fun hydrateProfile() {
        _isCheckingAuth.value = true
        _authError.value = null

        try {
            val ghToken = tokenStorage.getGitHubToken()
            val hasRealGhToken = !ghToken.isNullOrBlank() && ghToken != "manual_mode"

            if (hasRealGhToken) {
                val githubUser = gitHubRepository.getUser()
                var spProfile = try {
                    supabaseClient.fetchProfileByUsername(githubUser.login)
                } catch (e: Exception) { null }

                val cachedRole = localDataStore.getCachedRole()
                if (spProfile?.role == null && cachedRole != null) {
                    val authUid = try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { UUID.randomUUID() }
                    try {
                        supabaseClient.upsertProfile(
                            id = authUid,
                            username = githubUser.login,
                            role = cachedRole,
                            fullName = spProfile?.fullName ?: githubUser.name,
                            avatarUrl = spProfile?.avatarUrl ?: githubUser.avatarUrl,
                            bio = spProfile?.bio ?: githubUser.bio,
                            plan = spProfile?.plan
                        )
                        spProfile = supabaseClient.fetchProfileByUsername(githubUser.login)
                    } catch (e: Exception) { }
                }

                val finalRole = spProfile?.role ?: cachedRole
                val finalProfile = GitloftProfile(
                    id = spProfile?.id ?: UUID.randomUUID(),
                    username = githubUser.login,
                    fullName = spProfile?.fullName ?: githubUser.name,
                    avatarUrl = spProfile?.avatarUrl ?: githubUser.avatarUrl,
                    bio = spProfile?.bio ?: githubUser.bio,
                    role = finalRole,
                    createdAt = spProfile?.createdAt,
                    plan = spProfile?.plan ?: "free"
                )

                _profile.value = finalProfile
                _resolvedRole.value = finalRole
                _isAuthenticated.value = true
                _isGuest.value = false
                if (finalRole != null) localDataStore.setCachedRole(finalRole)
            } else {
                // Supabase / Google / Email / manual mode
                val cachedRole = localDataStore.getCachedRole()
                val authUser = try { supabaseClient.fetchCurrentUserDetails() } catch (e: Exception) { null }
                val authUid = authUser?.id ?: try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { UUID.randomUUID() }
                var spProfile = try {
                    supabaseClient.fetchProfile(authUid.toString())
                } catch (e: Exception) { null }

                val resolvedUsername = spProfile?.username ?: authUser?.username ?: tokenStorage.getProfileUsername()
                if (!resolvedUsername.isNullOrBlank()) {
                    tokenStorage.saveProfileUsername(resolvedUsername)
                }

                // If we have a username, fetch public GitHub user info
                val publicGhUser = if (!resolvedUsername.isNullOrBlank()) {
                    try { gitHubRepository.getUserByUsername(resolvedUsername) } catch (e: Exception) { null }
                } else null

                val finalRole = spProfile?.role ?: cachedRole
                val finalName = spProfile?.fullName ?: publicGhUser?.name ?: authUser?.fullName
                val finalAvatar = spProfile?.avatarUrl ?: publicGhUser?.avatarUrl ?: authUser?.avatarUrl
                val finalBio = spProfile?.bio ?: publicGhUser?.bio

                if (spProfile?.role == null && cachedRole != null && !resolvedUsername.isNullOrBlank()) {
                    try {
                        supabaseClient.upsertProfile(
                            id = authUid,
                            username = resolvedUsername,
                            role = cachedRole,
                            fullName = finalName,
                            avatarUrl = finalAvatar,
                            bio = finalBio,
                            plan = spProfile?.plan
                        )
                    } catch (e: Exception) { }
                }

                val finalProfile = GitloftProfile(
                    id = authUid,
                    username = resolvedUsername,
                    fullName = finalName,
                    avatarUrl = finalAvatar,
                    bio = finalBio,
                    role = finalRole,
                    createdAt = spProfile?.createdAt,
                    plan = spProfile?.plan ?: "free"
                )

                _profile.value = finalProfile
                _resolvedRole.value = finalRole
                _isAuthenticated.value = true
                _isGuest.value = false
                if (finalRole != null) localDataStore.setCachedRole(finalRole)
            }
        } catch (e: Exception) {
            val cachedRole = localDataStore.getCachedRole()
            if (cachedRole != null) {
                // Offline fallback
                _profile.value = GitloftProfile(
                    id = UUID.randomUUID(),
                    username = null,
                    fullName = null,
                    avatarUrl = null,
                    bio = null,
                    role = cachedRole,
                    plan = "free"
                )
                _resolvedRole.value = cachedRole
                _isAuthenticated.value = true
            } else {
                _isAuthenticated.value = false
            }
        } finally {
            _isCheckingAuth.value = false
        }
    }

    fun setRole(role: UserRole, username: String? = null, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val ghToken = tokenStorage.getGitHubToken()
                val hasRealGhToken = !ghToken.isNullOrBlank() && ghToken != "manual_mode"

                val authUser = try { supabaseClient.fetchCurrentUserDetails() } catch (e: Exception) { null }
                val authUid = authUser?.id ?: try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { UUID.randomUUID() }

                var finalName: String? = _profile.value?.fullName ?: authUser?.fullName
                var finalAvatar: String? = _profile.value?.avatarUrl ?: authUser?.avatarUrl
                var finalUsername = username ?: _profile.value?.username

                if (hasRealGhToken) {
                    val ghUser = gitHubRepository.getUser()
                    finalUsername = ghUser.login
                    finalName = ghUser.name ?: finalName
                    finalAvatar = ghUser.avatarUrl ?: finalAvatar
                } else if (!_profile.value?.username.isNullOrBlank()) {
                    finalUsername = _profile.value?.username
                    finalName = _profile.value?.fullName ?: finalName
                    finalAvatar = _profile.value?.avatarUrl ?: finalAvatar
                } else if (!username.isNullOrBlank()) {
                    val trimmed = username.trim()
                    if (com.example.gitloftandroid.util.InputValidator.isValidGitHubToken(trimmed)) {
                        tokenStorage.saveGitHubToken(trimmed)
                        val ghUser = gitHubRepository.getUser()
                        finalUsername = ghUser.login
                        finalName = ghUser.name ?: finalName
                        finalAvatar = ghUser.avatarUrl ?: finalAvatar
                    } else if (role == UserRole.DEVELOPER) {
                        throw IllegalArgumentException("A verified Personal Access Token or GitHub OAuth authorization is required to curate repositories.")
                    }
                }

                try {
                    supabaseClient.upsertProfile(
                        id = authUid,
                        username = finalUsername,
                        role = role,
                        fullName = finalName,
                        avatarUrl = finalAvatar,
                        bio = null
                    )
                } catch (e: Exception) { }

                val freshProfile = GitloftProfile(
                    id = authUid,
                    username = finalUsername,
                    fullName = finalName,
                    avatarUrl = finalAvatar,
                    bio = null,
                    role = role,
                    plan = "free"
                )

                _profile.value = freshProfile
                _resolvedRole.value = role
                _isAuthenticated.value = true
                _isGuest.value = false
                localDataStore.setCachedRole(role)
                onComplete?.invoke(true, null)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
                onComplete?.invoke(false, e.localizedMessage ?: "Failed to set role.")
            }
        }
    }

    fun enterGuestMode() {
        _isGuest.value = true
        _isAuthenticated.value = false
        _profile.value = null
    }

    fun exitGuestMode() {
        _isGuest.value = false
    }

    fun loginWithGitHubToken(
        token: String,
        role: UserRole = UserRole.DEVELOPER,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isCheckingAuth.value = true
            _authError.value = null
            try {
                val trimmed = token.trim()
                if (!com.example.gitloftandroid.util.InputValidator.isValidGitHubToken(trimmed)) {
                    throw IllegalArgumentException("A valid GitHub Personal Access Token is required (starting with 'ghp_' or 'github_pat_').")
                }

                tokenStorage.saveGitHubToken(trimmed)

                // Verifies the token against GitHub API (will throw exception on 401 Unauthorized)
                val ghUser = gitHubRepository.getUser()

                val authUid = UUID.randomUUID()
                var spProfile = try {
                    supabaseClient.fetchProfileByUsername(ghUser.login)
                } catch (e: Exception) { null }

                val finalRole = spProfile?.role ?: role
                val finalProfile = GitloftProfile(
                    id = spProfile?.id ?: authUid,
                    username = ghUser.login,
                    fullName = spProfile?.fullName ?: ghUser.name,
                    avatarUrl = spProfile?.avatarUrl ?: ghUser.avatarUrl,
                    bio = spProfile?.bio ?: ghUser.bio,
                    role = finalRole,
                    plan = spProfile?.plan ?: "free"
                )

                _profile.value = finalProfile
                _resolvedRole.value = finalRole
                _isAuthenticated.value = true
                _isGuest.value = false
                localDataStore.saveStoredRepos(emptyList())
                localDataStore.setCachedRole(finalRole)
                onComplete(true, null)
            } catch (e: Exception) {
                tokenStorage.clearGitHubToken()
                _authError.value = e.localizedMessage ?: "Failed to authenticate"
                onComplete(false, e.localizedMessage)
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }

    fun loginWithGitHubTokenOrUsername(
        input: String,
        role: UserRole = UserRole.DEVELOPER,
        onComplete: (Boolean, String?) -> Unit
    ) {
        loginWithGitHubToken(input, role, onComplete)
    }

    fun logout() {
        viewModelScope.launch {
            try { supabaseClient.signOut() } catch (e: Exception) { }
            tokenStorage.clearGitHubToken()
            tokenStorage.clearSupabaseJwt()
            tokenStorage.clearProfileUsername()
            localDataStore.saveStoredRepos(emptyList())
            localDataStore.setCachedRole(null)

            _profile.value = null
            _resolvedRole.value = null
            _isAuthenticated.value = false
            _isGuest.value = false
        }
    }

    fun setAuthError(error: String?) {
        _authError.value = error
    }

    fun switchRole(newRole: UserRole) {
        viewModelScope.launch {
            _resolvedRole.value = newRole
            localDataStore.setCachedRole(newRole)
            val current = _profile.value
            if (current != null) {
                val updated = current.copy(role = newRole)
                _profile.value = updated
                try {
                    val authUid = try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { current.id }
                    supabaseClient.upsertProfile(
                        id = authUid,
                        username = current.username,
                        role = newRole,
                        fullName = current.fullName,
                        avatarUrl = current.avatarUrl,
                        bio = current.bio,
                        plan = current.plan
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SessionViewModel", "Failed to update role in DB: ${e.message}")
                }
            }
        }
    }

    fun navigateHomeFromDeepLink() {
        if (_isAuthenticated.value) {
            _homeNavigationRequest.value += 1
        } else {
            _isGuest.value = false
        }
    }
}
