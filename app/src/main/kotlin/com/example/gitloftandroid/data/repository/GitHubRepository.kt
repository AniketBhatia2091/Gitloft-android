package com.example.gitloftandroid.data.repository

import com.example.gitloftandroid.data.model.GitHubContentItem
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.GitHubTreeResponse
import com.example.gitloftandroid.data.model.TreeItem
import com.example.gitloftandroid.data.model.GitHubUser
import com.example.gitloftandroid.data.network.GitHubApiService
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class GitHubRepository(
    private val tokenStorage: TokenStorage,
    private val gitHubApiService: GitHubApiService = defaultService
) {

    private val contentCache = ConcurrentHashMap<String, String>()

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val defaultService: GitHubApiService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            retrofit.create(GitHubApiService::class.java)
        }
    }

    private fun authHeader(): String? {
        val token = tokenStorage.getGitHubToken() ?: return null
        if (token.isBlank() || token == "manual_mode") {
            return null
        }
        return "Bearer $token"
    }

    suspend fun getUser(): GitHubUser = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getUser(header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            throw IOException("Failed to fetch user: code ${response.code()}")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getUserByUsername(username: String): GitHubUser = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getUserByUsername(username, header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            throw IOException("User '@$username' not found")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getRepos(): List<GitHubRepo> = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getRepos(header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            throw IOException("Failed to fetch repos: code ${response.code()}")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getReposByUsername(username: String): List<GitHubRepo> = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getReposByUsername(username, header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            throw IOException("Failed to fetch repos for '@$username'")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getRepoTree(owner: String, repo: String, sha: String = "main"): GitHubTreeResponse = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getRepoTree(owner, repo, sha, header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            // If main fails, try HEAD
            if (sha == "main") {
                val headResponse = gitHubApiService.getRepoTree(owner, repo, "HEAD", header).execute()
                if (headResponse.isSuccessful && headResponse.body() != null) {
                    return@withContext headResponse.body()!!
                }
            }
            throw IOException("Failed to fetch repository tree: code ${response.code()}")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getRepoContents(owner: String, repo: String, path: String = ""): List<GitHubContentItem> = withContext(Dispatchers.IO) {
        val header = authHeader()
        try {
            val response = gitHubApiService.getRepoContents(owner, repo, path, header).execute()
            if (response.isSuccessful && response.body() != null) {
                return@withContext response.body()!!
            }
            throw IOException("Failed to fetch repository contents")
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String): String = withContext(Dispatchers.IO) {
        val cacheKey = "$owner/$repo/$path"
        contentCache[cacheKey]?.let { return@withContext it }

        val header = authHeader()
        try {
            val response = gitHubApiService.getFileContent(owner, repo, path, authHeader = header).execute()
            val text = response.body()?.string() ?: throw IOException("Empty file content")
            contentCache[cacheKey] = text
            text
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getReadme(owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val cacheKey = "readme/$owner/$repo"
        contentCache[cacheKey]?.let { return@withContext it }

        val header = authHeader()
        try {
            val response = gitHubApiService.getReadme(owner, repo, authHeader = header).execute()
            val text = response.body()?.string() ?: throw IOException("README not found")
            contentCache[cacheKey] = text
            text
        } catch (e: Exception) {
            throw IOException(e)
        }
    }
}
