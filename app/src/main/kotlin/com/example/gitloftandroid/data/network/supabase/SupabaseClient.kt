package com.example.gitloftandroid.data.network.supabase

import android.content.Context
import android.net.Uri
import com.example.gitloftandroid.BuildConfig
import com.example.gitloftandroid.data.model.AnalyticsEvent
import com.example.gitloftandroid.data.model.DailyEventCount
import com.example.gitloftandroid.data.model.GitloftProfile
import com.example.gitloftandroid.data.model.RepoClickCount
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.model.supabase.NotificationEvent
import com.example.gitloftandroid.data.model.supabase.ProfileStats
import com.example.gitloftandroid.data.model.supabase.ShowcaseProfileResponse
import com.example.gitloftandroid.data.model.supabase.StoredRepoSnapshot
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val id: UUID,
    val email: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val username: String? = null,
    val provider: String? = null
)

sealed class SignUpResult {
    data class Authenticated(val token: String) : SignUpResult()
    data class ConfirmationRequired(val email: String) : SignUpResult()
}

class SupabaseClient private constructor(private val context: Context) {

    private val tokenStorage = TokenStorage(context)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    private val rawOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()

            // Skip interceptor for auth token/signup/recover endpoints to prevent recursion
            if (url.contains("/auth/v1/token") || url.contains("/auth/v1/signup") || url.contains("/auth/v1/recover")) {
                return@addInterceptor chain.proceed(originalRequest)
            }

            val currentJwt = tokenStorage.getSupabaseJwt()
            val authHeader = originalRequest.header("Authorization")

            // If request uses our Supabase JWT and it's expired or about to expire in 60s, refresh proactively
            if (!currentJwt.isNullOrBlank() && currentJwt != "demo_mode" && authHeader?.contains(currentJwt) == true) {
                if (tokenStorage.isSupabaseTokenExpired(bufferSeconds = 60)) {
                    val refreshedJwt = refreshSessionBlocking(force = false)
                    if (!refreshedJwt.isNullOrBlank() && refreshedJwt != currentJwt) {
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $refreshedJwt")
                            .build()
                        return@addInterceptor chain.proceed(newRequest)
                    }
                }
            }

            chain.proceed(originalRequest)
        }
        .authenticator { _, response ->
            if (responseCount(response) >= 3) return@authenticator null

            val originalRequest = response.request
            val url = originalRequest.url.toString()
            if (url.contains("/auth/v1/token") || url.contains("/auth/v1/signup") || url.contains("/auth/v1/recover")) {
                return@authenticator null
            }

            val currentJwt = tokenStorage.getSupabaseJwt()
            val refreshToken = tokenStorage.getSupabaseRefreshToken()

            if (!refreshToken.isNullOrBlank()) {
                val newJwt = refreshSessionBlocking(force = true)
                if (!newJwt.isNullOrBlank() && newJwt != currentJwt) {
                    return@authenticator originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newJwt")
                        .build()
                }
            }
            null
        }
        .build()

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    @Synchronized
    fun refreshSessionBlocking(force: Boolean = false): String? {
        val currentJwt = tokenStorage.getSupabaseJwt()
        if (!force && !tokenStorage.isSupabaseTokenExpired(bufferSeconds = 30)) {
            return currentJwt
        }

        val refreshToken = tokenStorage.getSupabaseRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            return null
        }

        return executeRefreshTokenCall(refreshToken)
    }

    suspend fun ensureValidSession(force: Boolean = false): String? = withContext(Dispatchers.IO) {
        refreshSessionBlocking(force)
    }

    private fun executeRefreshTokenCall(refreshToken: String): String? {
        return try {
            val payload = buildJsonObject {
                put("refresh_token", refreshToken)
            }
            val request = Request.Builder()
                .url("$projectUrl/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = rawOkHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val obj = json.parseToJsonElement(body).jsonObject
                val newAccessToken = obj["access_token"]?.jsonPrimitive?.contentOrNull
                val newRefreshToken = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
                val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull
                val expiresAt = obj["expires_at"]?.jsonPrimitive?.longOrNull

                if (!newAccessToken.isNullOrBlank()) {
                    tokenStorage.saveSupabaseSession(
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken ?: refreshToken,
                        expiresInSeconds = expiresIn,
                        expiresAtEpochSeconds = expiresAt
                    )
                    android.util.Log.d("SupabaseClient", "Supabase token refreshed successfully (expiresIn: ${expiresIn}s)")
                    newAccessToken
                } else {
                    null
                }
            } else {
                android.util.Log.w("SupabaseClient", "Supabase token refresh failed with HTTP ${response.code}: $body")
                if (response.code == 400 || response.code == 401) {
                    tokenStorage.clearSupabaseSession()
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseClient", "Exception during Supabase token refresh", e)
            null
        }
    }

    private val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.trimEnd('/')

    private val apiKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    companion object {
        @Volatile
        private var INSTANCE: SupabaseClient? = null

        fun getInstance(context: Context): SupabaseClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // MARK: - HTTP Helpers

    private fun newRequestBuilder(endpoint: String, requiresAuth: Boolean = false): Request.Builder {
        val url = if (endpoint.startsWith("http")) endpoint else "$projectUrl/rest/v1/$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)

        val jwt = tokenStorage.getSupabaseJwt()
        if (!jwt.isNullOrBlank() && jwt != "demo_mode") {
            builder.addHeader("Authorization", "Bearer $jwt")
        } else if (requiresAuth) {
            val ghToken = tokenStorage.getGitHubToken()
            val hasValidGhToken = !ghToken.isNullOrBlank() &&
                ghToken != "manual_mode" &&
                (ghToken.startsWith("ghp_") || ghToken.startsWith("github_pat_") ||
                 ghToken.startsWith("ghu_") || ghToken.startsWith("gho_"))

            if (hasValidGhToken) {
                // Verified GitHub PAT session: authorize via project API key
                builder.addHeader("Authorization", "Bearer $apiKey")
            } else {
                throw IOException("Authentication required: Please sign in with a valid GitHub Personal Access Token or account.")
            }
        } else {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }

        return builder
    }

    // MARK: - Showcase Publishing & Fetching

    suspend fun publishShowcase(
        username: String,
        fullName: String?,
        avatarUrl: String?,
        bio: String?,
        pinnedRepos: List<StoredRepoSnapshot>
    ) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("username", username)
            put("full_name", fullName)
            put("avatar_url", avatarUrl)
            put("bio", bio)
            put("pinned_repos", json.encodeToJsonElement(pinnedRepos))
        }

        val request = newRequestBuilder("showcases?on_conflict=username", requiresAuth = true)
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Publish showcase failed: ${response.code} ${response.body?.string()}")
        }
    }

    suspend fun checkShowcaseExists(username: String): Boolean = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val request = newRequestBuilder("showcases?username=eq.$safeUsername&select=id")
            .addHeader("Prefer", "count=exact")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val contentRange = response.header("Content-Range")
            val total = contentRange?.substringAfterLast('/')?.toIntOrNull()
            if (total != null) {
                return@withContext total > 0
            }
            val body = response.body?.string() ?: ""
            val array = json.parseToJsonElement(body) as? JsonArray
            return@withContext (array?.size ?: 0) > 0
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun fetchShowcase(username: String): ShowcaseProfileResponse? = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val request = newRequestBuilder("showcases?username=eq.$safeUsername&select=*")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val list = json.decodeFromString<List<ShowcaseProfileResponse>>(body)
                return@withContext list.firstOrNull()
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun fetchGlobalShowcases(limit: Int = 50, offset: Int = 0): List<ShowcaseProfileResponse> = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("showcases?select=*&limit=$limit&offset=$offset&order=updated_at.desc")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext emptyList()
                val list = json.decodeFromString<List<ShowcaseProfileResponse>>(body)
                return@withContext list
            }
            return@withContext emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    // MARK: - Analytics Pipeline

    suspend fun fetchStats(username: String): ProfileStats = withContext(Dispatchers.IO) {
        val views = fetchCount(username, "view")
        val clicks = fetchCount(username, "click")
        ProfileStats(views = views, clicks = clicks)
    }

    suspend fun fetchTotalViews(username: String): Int = fetchCount(username, "view")
    suspend fun fetchTotalClicks(username: String): Int = fetchCount(username, "click")
    suspend fun fetchShortlistCount(username: String): Int = fetchCount(username, "shortlist_add")

    private suspend fun fetchCount(username: String, type: String): Int = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val safeType = Uri.encode(type)
        val request = newRequestBuilder("events?showcase_username=eq.$safeUsername&event_type=eq.$safeType&select=id")
            .addHeader("Prefer", "count=exact")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val contentRange = response.header("Content-Range")
            val total = contentRange?.substringAfterLast('/')?.toIntOrNull()
            if (total != null) return@withContext total

            val body = response.body?.string() ?: ""
            val array = json.parseToJsonElement(body) as? JsonArray
            return@withContext array?.size ?: 0
        } catch (e: Exception) {
            return@withContext 0
        }
    }

    suspend fun fetchWeeklyEvents(username: String): List<DailyEventCount> = withContext(Dispatchers.IO) {
        val emptyDays = makeEmptyWeeklyCounts()
        try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val isoDate = Uri.encode(isoFormatter.format(cal.time))
            val safeUsername = Uri.encode(username)

            val request = newRequestBuilder("events?showcase_username=eq.$safeUsername&created_at=gte.$isoDate&select=created_at,event_type")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyDays

            val body = response.body?.string() ?: return@withContext emptyDays
            val array = json.parseToJsonElement(body).jsonArray

            val dayKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parseFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

            val countsMap = emptyDays.associateBy { it.date }.toMutableMap()

            for (item in array) {
                val obj = item.jsonObject
                val createdAtStr = obj["created_at"]?.jsonPrimitive?.contentOrNull ?: continue
                val eventType = obj["event_type"]?.jsonPrimitive?.contentOrNull ?: continue

                val date = try {
                    parseFormatter.parse(createdAtStr.take(19))
                } catch (e: Exception) { null } ?: continue

                val key = dayKeyFormatter.format(date)
                val current = countsMap[key] ?: continue

                if (eventType == "view") {
                    countsMap[key] = current.copy(views = current.views + 1)
                } else if (eventType == "click") {
                    countsMap[key] = current.copy(clicks = current.clicks + 1)
                }
            }

            return@withContext emptyDays.map { countsMap[it.date] ?: it }
        } catch (e: Exception) {
            return@withContext emptyDays
        }
    }

    suspend fun fetchTopRepos(username: String): List<RepoClickCount> = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val request = newRequestBuilder("events?showcase_username=eq.$safeUsername&event_type=eq.click&select=repo_name")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val array = json.parseToJsonElement(body).jsonArray

            val counts = mutableMapOf<String, Int>()
            for (elem in array) {
                val repoName = elem.jsonObject["repo_name"]?.jsonPrimitive?.contentOrNull
                if (!repoName.isNullOrBlank()) {
                    counts[repoName] = (counts[repoName] ?: 0) + 1
                }
            }

            return@withContext counts.entries
                .map { RepoClickCount(it.key, it.value) }
                .sortedByDescending { it.clicks }
                .take(3)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun fetchNotificationEvents(username: String, includeViews: Boolean = false): List<NotificationEvent> = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val eventFilters = if (includeViews)
            "event_type.eq.click,event_type.eq.shortlist_add,event_type.eq.rating_update,event_type.eq.view"
        else
            "event_type.eq.click,event_type.eq.shortlist_add,event_type.eq.rating_update"

        val endpoint = "events?showcase_username=eq.$safeUsername&or=($eventFilters)&select=id,showcase_username,event_type,repo_name,target_url,rating,viewer_role,viewer_username,viewer_display_name,source,created_at&order=created_at.desc"
        val request = newRequestBuilder(endpoint).get().build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            return@withContext json.decodeFromString<List<NotificationEvent>>(body)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun fetchAverageRating(username: String): Pair<Double, Int> = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val endpoint = "events?showcase_username=eq.$safeUsername&event_type=eq.rating_update&viewer_username=not.is.null&select=id,event_type,rating,viewer_username,created_at&order=created_at.desc"
        val request = newRequestBuilder(endpoint).get().build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Pair(0.0, 0)

            val body = response.body?.string() ?: return@withContext Pair(0.0, 0)
            val events = json.decodeFromString<List<NotificationEvent>>(body)

            val latestByViewer = mutableMapOf<String, Int>()
            for (ev in events) {
                val viewer = ev.viewerUsername ?: continue
                val rating = ev.rating ?: continue
                if (!latestByViewer.containsKey(viewer)) {
                    latestByViewer[viewer] = rating
                }
            }

            val count = latestByViewer.size
            if (count == 0) return@withContext Pair(0.0, 0)

            val avg = latestByViewer.values.sum().toDouble() / count
            return@withContext Pair(avg, count)
        } catch (e: Exception) {
            return@withContext Pair(0.0, 0)
        }
    }

    suspend fun logEvent(event: AnalyticsEvent) = withContext(Dispatchers.IO) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val payload = event.copy(created_at = nowIso)
        val request = newRequestBuilder("events")
            .post(json.encodeToString(payload).toRequestBody("application/json".toMediaType()))
            .build()

        try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            // Background telemetry fails silently
        }
    }

    // MARK: - Profile Management

    suspend fun fetchProfile(id: String): GitloftProfile? = withContext(Dispatchers.IO) {
        val request = newRequestBuilder("profiles?id=eq.$id&select=*", requiresAuth = true).get().build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val list = json.decodeFromString<List<GitloftProfile>>(body)
                return@withContext list.firstOrNull()
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun fetchProfileByUsername(username: String): GitloftProfile? = withContext(Dispatchers.IO) {
        val safeUsername = Uri.encode(username)
        val request = newRequestBuilder("profiles?username=eq.$safeUsername&select=*").get().build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val list = json.decodeFromString<List<GitloftProfile>>(body)
                return@withContext list.firstOrNull()
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun upsertProfile(
        id: UUID,
        username: String?,
        role: UserRole,
        fullName: String?,
        avatarUrl: String?,
        bio: String? = null,
        plan: String? = null
    ) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("id", id.toString().lowercase())
            put("username", username)
            put("role", role.value)
            put("full_name", fullName)
            put("avatar_url", avatarUrl)
            if (bio != null) put("bio", bio)
            if (plan != null) put("plan", plan)
        }

        val request = newRequestBuilder("profiles?on_conflict=id", requiresAuth = true)
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to upsert profile: ${response.code}")
        }
    }

    suspend fun updatePlan(id: String, plan: String) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("plan", plan)
        }

        val request = newRequestBuilder("profiles?id=eq.$id", requiresAuth = true)
            .addHeader("Prefer", "return=minimal")
            .patch(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to update plan: ${response.code}")
        }
    }

    suspend fun fetchCurrentUserDetails(): SupabaseUser = withContext(Dispatchers.IO) {
        ensureValidSession(force = false)
        val jwt = tokenStorage.getSupabaseJwt() ?: apiKey
        val request = Request.Builder()
            .url("$projectUrl/auth/v1/user")
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $jwt")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: throw IOException("Empty auth user response")
            val obj = json.parseToJsonElement(body).jsonObject
            val idStr = obj["id"]?.jsonPrimitive?.contentOrNull ?: throw IOException("Missing user ID")
            val email = obj["email"]?.jsonPrimitive?.contentOrNull
            val userMeta = obj["user_metadata"]?.jsonObject
            val fullName = userMeta?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: userMeta?.get("name")?.jsonPrimitive?.contentOrNull
            val avatarUrl = userMeta?.get("avatar_url")?.jsonPrimitive?.contentOrNull
            val metaUsername = userMeta?.get("user_name")?.jsonPrimitive?.contentOrNull
                ?: userMeta?.get("preferred_username")?.jsonPrimitive?.contentOrNull
                ?: userMeta?.get("nickname")?.jsonPrimitive?.contentOrNull
                ?: run {
                    val identities = obj["identities"]?.let { el -> if (el is kotlinx.serialization.json.JsonArray) el else null }
                    val ghIdentity = identities?.firstOrNull {
                        it.jsonObject["provider"]?.jsonPrimitive?.contentOrNull == "github"
                    }?.jsonObject
                    ghIdentity?.get("identity_data")?.jsonObject?.get("user_name")?.jsonPrimitive?.contentOrNull
                }
            val appMeta = obj["app_metadata"]?.jsonObject
            val provider = appMeta?.get("provider")?.jsonPrimitive?.contentOrNull

            return@withContext SupabaseUser(
                id = UUID.fromString(idStr),
                email = email,
                fullName = fullName,
                avatarUrl = avatarUrl,
                username = metaUsername,
                provider = provider
            )
        }
        throw IOException("Failed to fetch auth user: ${response.code}")
    }

    suspend fun fetchCurrentAuthUser(): UUID = withContext(Dispatchers.IO) {
        return@withContext fetchCurrentUserDetails().id
    }

    // MARK: - Email Auth

    suspend fun loginWithEmail(email: String, pass: String): String = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("email", email)
            put("password", pass)
        }

        val request = Request.Builder()
            .url("$projectUrl/auth/v1/token?grant_type=password")
            .addHeader("apikey", apiKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = rawOkHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val obj = json.parseToJsonElement(body).jsonObject
            val token = obj["access_token"]?.jsonPrimitive?.contentOrNull ?: throw IOException("Missing access token")
            val refreshToken = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
            val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull
            val expiresAt = obj["expires_at"]?.jsonPrimitive?.longOrNull
            tokenStorage.saveSupabaseSession(
                accessToken = token,
                refreshToken = refreshToken,
                expiresInSeconds = expiresIn,
                expiresAtEpochSeconds = expiresAt
            )
            return@withContext token
        }
        val errorDesc = try {
            val jsonObj = json.parseToJsonElement(body).jsonObject
            jsonObj["error_description"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["msg"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["message"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["error"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) { null }
        throw IOException(errorDesc ?: "Login failed (${response.code})")
    }

    suspend fun signUpWithEmail(email: String, pass: String): SignUpResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("email", email)
            put("password", pass)
        }

        val request = Request.Builder()
            .url("$projectUrl/auth/v1/signup")
            .addHeader("apikey", apiKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = rawOkHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val obj = json.parseToJsonElement(body).jsonObject
            val token = obj["access_token"]?.jsonPrimitive?.contentOrNull
            val refreshToken = obj["refresh_token"]?.jsonPrimitive?.contentOrNull
            val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull
            val expiresAt = obj["expires_at"]?.jsonPrimitive?.longOrNull
            if (!token.isNullOrBlank()) {
                tokenStorage.saveSupabaseSession(
                    accessToken = token,
                    refreshToken = refreshToken,
                    expiresInSeconds = expiresIn,
                    expiresAtEpochSeconds = expiresAt
                )
                return@withContext SignUpResult.Authenticated(token)
            }
            return@withContext SignUpResult.ConfirmationRequired(email)
        }
        val errorDesc = try {
            val jsonObj = json.parseToJsonElement(body).jsonObject
            jsonObj["error_description"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["msg"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["message"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["error"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) { null }
        throw IOException(errorDesc ?: "Signup failed (${response.code})")
    }

    suspend fun resetPasswordForEmail(email: String): Unit = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("email", email)
        }

        val request = Request.Builder()
            .url("$projectUrl/auth/v1/recover")
            .addHeader("apikey", apiKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = rawOkHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            val errorDesc = try {
                json.parseToJsonElement(body).jsonObject["error_description"]?.jsonPrimitive?.contentOrNull
                    ?: json.parseToJsonElement(body).jsonObject["msg"]?.jsonPrimitive?.contentOrNull
            } catch (e: Exception) { null }
            throw IOException(errorDesc ?: "Password recovery failed (${response.code})")
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            val jwt = tokenStorage.getSupabaseJwt()
            if (!jwt.isNullOrBlank() && jwt != "demo_mode") {
                val request = Request.Builder()
                    .url("$projectUrl/auth/v1/logout")
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $jwt")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                rawOkHttpClient.newCall(request).execute()
            }
        } catch (e: Exception) {
            // Ignore logout network errors
        } finally {
            tokenStorage.clearSupabaseSession()
        }
    }

    private fun makeEmptyWeeklyCounts(): List<DailyEventCount> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableListOf<DailyEventCount>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            result.add(DailyEventCount(formatter.format(c.time), 0, 0))
        }
        return result
    }
}
