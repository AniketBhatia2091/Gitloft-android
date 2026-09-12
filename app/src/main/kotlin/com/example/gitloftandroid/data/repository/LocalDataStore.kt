package com.example.gitloftandroid.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.SavedDeveloper
import com.example.gitloftandroid.data.model.StoredRepo
import com.example.gitloftandroid.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalDataStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gitloft_local_datastore", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val _storedReposFlow = MutableStateFlow<List<StoredRepo>>(emptyList())
    val storedReposFlow: StateFlow<List<StoredRepo>> = _storedReposFlow.asStateFlow()

    private val _shortlistFlow = MutableStateFlow<List<SavedDeveloper>>(emptyList())
    val shortlistFlow: StateFlow<List<SavedDeveloper>> = _shortlistFlow.asStateFlow()

    companion object {
        private const val KEY_STORED_REPOS = "stored_repos"
        private const val KEY_SAVED_DEVELOPERS = "saved_developers"
        private const val KEY_CACHED_ROLE = "cached_role"
        private const val KEY_NOTIF_VIEWED = "last_viewed_notifications"

        @Volatile
        private var INSTANCE: LocalDataStore? = null

        fun getInstance(context: Context): LocalDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        _storedReposFlow.value = getStoredRepos()
        _shortlistFlow.value = getSavedDevelopers()
    }

    // MARK: - Stored Repositories

    @Synchronized
    fun getStoredRepos(): List<StoredRepo> {
        val raw = prefs.getString(KEY_STORED_REPOS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<StoredRepo>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveStoredRepos(repos: List<StoredRepo>) {
        val sorted = repos.sortedBy { it.orderIndex }
        val raw = json.encodeToString(sorted)
        prefs.edit().putString(KEY_STORED_REPOS, raw).apply()
        _storedReposFlow.value = sorted
    }

    @Synchronized
    fun syncPublicRepos(networkRepos: List<GitHubRepo>) {
        val publicRepos = networkRepos.filter { !it.isPrivate }
        val incomingIds = publicRepos.map { it.id }.toSet()
        val existing = getStoredRepos().associateBy { it.id }

        val merged = mutableListOf<StoredRepo>()
        for (repo in publicRepos) {
            val existingItem = existing[repo.id]
            if (existingItem != null) {
                merged.add(
                    existingItem.copy(
                        name = repo.name,
                        originalDescription = repo.description,
                        language = repo.language,
                        stargazersCount = repo.stargazersCount,
                        isPrivate = repo.isPrivate,
                        htmlUrlString = repo.htmlUrl,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            } else {
                merged.add(
                    StoredRepo(
                        id = repo.id,
                        name = repo.name,
                        originalDescription = repo.description,
                        language = repo.language,
                        stargazersCount = repo.stargazersCount,
                        isPrivate = repo.isPrivate,
                        htmlUrlString = repo.htmlUrl,
                        isPinned = false,
                        orderIndex = merged.size
                    )
                )
            }
        }

        saveStoredRepos(merged)
    }

    @Synchronized
    fun updateRepo(updatedRepo: StoredRepo) {
        val current = getStoredRepos().toMutableList()
        val index = current.indexOfFirst { it.id == updatedRepo.id }
        if (index != -1) {
            current[index] = updatedRepo
        } else {
            current.add(updatedRepo)
        }
        saveStoredRepos(current)
    }

    fun getPinnedRepos(maxPins: Int): List<StoredRepo> {
        return getStoredRepos()
            .filter { it.isPinned }
            .sortedBy { it.orderIndex }
            .take(maxPins)
    }

    // MARK: - Recruiter Shortlist

    @Synchronized
    fun getSavedDevelopers(): List<SavedDeveloper> {
        val raw = prefs.getString(KEY_SAVED_DEVELOPERS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedDeveloper>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveDeveloper(developer: SavedDeveloper) {
        val current = getSavedDevelopers().toMutableList()
        val existingIndex = current.indexOfFirst { it.username.equals(developer.username, ignoreCase = true) }
        if (existingIndex != null && existingIndex != -1) {
            current[existingIndex] = developer
        } else {
            current.add(0, developer)
        }
        val raw = json.encodeToString(current)
        prefs.edit().putString(KEY_SAVED_DEVELOPERS, raw).apply()
        _shortlistFlow.value = current
    }

    @Synchronized
    fun removeSavedDeveloper(username: String) {
        val current = getSavedDevelopers().filterNot { it.username.equals(username, ignoreCase = true) }
        val raw = json.encodeToString(current)
        prefs.edit().putString(KEY_SAVED_DEVELOPERS, raw).apply()
        _shortlistFlow.value = current
    }

    @Synchronized
    fun updateDeveloperRating(username: String, rating: Int) {
        val current = getSavedDevelopers().toMutableList()
        val existingIndex = current.indexOfFirst { it.username.equals(username, ignoreCase = true) }
        if (existingIndex != -1) {
            current[existingIndex] = current[existingIndex].copy(rating = rating)
            val raw = json.encodeToString(current)
            prefs.edit().putString(KEY_SAVED_DEVELOPERS, raw).apply()
            _shortlistFlow.value = current
        }
    }

    // MARK: - Cached Role

    fun getCachedRole(): UserRole? {
        val raw = prefs.getString(KEY_CACHED_ROLE, null) ?: return null
        return UserRole.fromString(raw)
    }

    fun setCachedRole(role: UserRole?) {
        if (role != null) {
            prefs.edit().putString(KEY_CACHED_ROLE, role.value).apply()
        } else {
            prefs.edit().remove(KEY_CACHED_ROLE).apply()
        }
    }

    // MARK: - Notifications

    fun getLastViewedNotifications(username: String): Long {
        return prefs.getLong("${KEY_NOTIF_VIEWED}_$username", 0L)
    }

    fun setLastViewedNotifications(username: String, timestamp: Long) {
        prefs.edit().putLong("${KEY_NOTIF_VIEWED}_$username", timestamp).apply()
    }
}
