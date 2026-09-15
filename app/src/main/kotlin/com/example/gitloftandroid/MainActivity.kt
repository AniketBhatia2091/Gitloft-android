package com.example.gitloftandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.ui.curator.CuratorDashboardScreen
import com.example.gitloftandroid.ui.hiring.HiringDashboardScreen
import com.example.gitloftandroid.ui.onboarding.GuestBrowseScreen
import com.example.gitloftandroid.ui.onboarding.RoleSelectionScreen
import com.example.gitloftandroid.ui.onboarding.WelcomeScreen
import com.example.gitloftandroid.ui.showcase.RepoDetailScreen
import com.example.gitloftandroid.ui.showcase.ShowcaseScreen
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.theme.GitloftTheme
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SessionViewModel.getInstance(application) as T
            }
        }
    }

    private var deepLinkUsernameState = mutableStateOf<String?>(null)
    private var lastProcessedOAuthUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            GitloftTheme {
                MainAppScreen(
                    sessionViewModel = sessionViewModel,
                    deepLinkUsername = deepLinkUsernameState.value,
                    onDismissDeepLink = { deepLinkUsernameState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val uriString = data.toString()

        // Deduplication: prevent processing the same OAuth callback URI multiple times
        if (uriString == lastProcessedOAuthUri) {
            android.util.Log.d("GitloftOAuth", "Ignoring duplicate OAuth callback: $uriString")
            return
        }

        // Handle gitloft://oauth-callback or https://gitloft-app.vercel.app/oauth-callback or https://gitloft.vercel.app/oauth-callback
        val isOAuthCallback = (data.scheme == "gitloft" && data.host == "oauth-callback") ||
                ((data.host == "gitloft-app.vercel.app" || data.host == "gitloft.vercel.app") &&
                 (data.path == "/oauth-callback" || (data.fragment != null && data.fragment!!.contains("access_token"))))

        if (isOAuthCallback) {
            lastProcessedOAuthUri = uriString
            intent.data = null

            android.util.Log.d("GitloftOAuth", "Received OAuth callback URI: $uriString")
            android.util.Log.d("GitloftOAuth", "OAuth Query: ${data.query}")
            android.util.Log.d("GitloftOAuth", "OAuth Fragment: ${data.fragment}")

            val queryParams = parseUrlParams(data.query ?: data.encodedQuery ?: "")
            val fragmentParams = parseUrlParams(data.fragment ?: data.encodedFragment ?: "")
            val params = queryParams + fragmentParams
            android.util.Log.d("GitloftOAuth", "OAuth parsed keys: ${params.keys}")

            val ghToken = params["provider_token"]
            val sbJwt = params["access_token"]
            val sbRefreshToken = params["refresh_token"]
            val authCode = params["code"] ?: params["auth_code"]
            val expiresIn = params["expires_in"]?.toLongOrNull()
            val expiresAt = params["expires_at"]?.toLongOrNull()
            val oauthError = params["error_description"] ?: params["error"]

            if (!oauthError.isNullOrBlank()) {
                android.util.Log.e("GitloftOAuth", "OAuth error callback: $oauthError")
                val decodedMsg = Uri.decode(oauthError).replace('+', ' ')
                android.widget.Toast.makeText(this, "Auth Failed: $decodedMsg", android.widget.Toast.LENGTH_LONG).show()
                sessionViewModel.setAuthError(decodedMsg)
                return
            }

            val tokenStorage = com.example.gitloftandroid.util.TokenStorage(this)
            if (!ghToken.isNullOrBlank()) {
                tokenStorage.saveGitHubToken(ghToken)
            }
            if (!sbJwt.isNullOrBlank()) {
                tokenStorage.saveSupabaseSession(
                    accessToken = sbJwt,
                    refreshToken = sbRefreshToken,
                    expiresInSeconds = expiresIn,
                    expiresAtEpochSeconds = expiresAt
                )
                sessionViewModel.checkAuth()
            } else if (!authCode.isNullOrBlank()) {
                android.util.Log.d("GitloftOAuth", "Received auth code, exchanging via SupabaseClient: $authCode")
                lifecycleScope.launch {
                    try {
                        val client = com.example.gitloftandroid.data.network.supabase.SupabaseClient.getInstance(this@MainActivity)
                        client.exchangeCodeForSession(authCode)
                        sessionViewModel.checkAuth()
                    } catch (e: Exception) {
                        android.util.Log.e("GitloftOAuth", "Code exchange failed", e)
                        val msg = e.message ?: "Code exchange failed"
                        android.widget.Toast.makeText(this@MainActivity, "Auth Failed: $msg", android.widget.Toast.LENGTH_LONG).show()
                        sessionViewModel.setAuthError(msg)
                    }
                }
            } else {
                sessionViewModel.checkAuth()
            }
            return
        }

        // Handle gitloft://home
        if (data.scheme == "gitloft" && data.host == "home") {
            sessionViewModel.navigateHomeFromDeepLink()
            intent.data = null
            return
        }

        // Handle gitloft://u/{username}
        if (data.scheme == "gitloft" && data.host == "u") {
            val username = data.pathSegments.firstOrNull()
            if (!username.isNullOrBlank()) {
                deepLinkUsernameState.value = username
                intent.data = null
                return
            }
        }

        // Handle https://gitloft-app.vercel.app/u/{username} or https://gitloft.vercel.app/u/{username}
        if (data.host == "gitloft-app.vercel.app" || data.host == "gitloft.vercel.app") {
            val parts = data.pathSegments
            if (parts.size >= 2 && parts[0] == "u") {
                deepLinkUsernameState.value = parts[1]
                intent.data = null
            }
        }
    }

    private fun parseUrlParams(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val trimmed = raw.trim().trimStart('#', '?')
        for (pair in trimmed.split("&")) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                val key = Uri.decode(parts[0])
                val value = Uri.decode(parts[1])
                result[key] = value
            }
        }
        return result
    }
}

@Composable
fun MainAppScreen(
    sessionViewModel: SessionViewModel,
    deepLinkUsername: String?,
    onDismissDeepLink: () -> Unit
) {
    val isCheckingAuth by sessionViewModel.isCheckingAuth.collectAsStateWithLifecycle()
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isGuest by sessionViewModel.isGuest.collectAsStateWithLifecycle()
    val resolvedRole by sessionViewModel.resolvedRole.collectAsStateWithLifecycle()
    val profile by sessionViewModel.profile.collectAsStateWithLifecycle()

    var selectedRepoForDetail by remember { mutableStateOf<GitHubRepo?>(null) }
    var selectedProfileForShowcase by remember { mutableStateOf<ShowcaseProfile?>(null) }

    // Deep link showcase profile loading
    val context = androidx.compose.ui.platform.LocalContext.current
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    var deepLinkProfile by remember { mutableStateOf<ShowcaseProfile?>(null) }
    var isDeepLinkLoading by remember { mutableStateOf(false) }

    LaunchedEffect(deepLinkUsername) {
        if (!deepLinkUsername.isNullOrBlank()) {
            isDeepLinkLoading = true
            try {
                val resp = supabaseClient.fetchShowcase(deepLinkUsername)
                deepLinkProfile = resp?.asShowcaseProfile
            } catch (e: Exception) {
                deepLinkProfile = null
            } finally {
                isDeepLinkLoading = false
            }
        } else {
            deepLinkProfile = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            isCheckingAuth -> {
                SplashScreen()
            }
            isAuthenticated -> {
                when (resolvedRole) {
                    UserRole.DEVELOPER -> {
                        CuratorDashboardScreen(
                            onLogout = { sessionViewModel.logout() },
                            onSelectRepo = { selectedRepoForDetail = it },
                            onSelectProfile = { selectedProfileForShowcase = it }
                        )
                    }
                    UserRole.RECRUITER -> {
                        HiringDashboardScreen(
                            onLogout = { sessionViewModel.logout() },
                            onSelectRepo = { selectedRepoForDetail = it },
                            onSelectProfile = { selectedProfileForShowcase = it }
                        )
                    }
                    null -> {
                        val tokenStorage = remember { com.example.gitloftandroid.util.TokenStorage(context) }
                        val ghToken = tokenStorage.getGitHubToken()
                        val hasToken = !profile?.username.isNullOrBlank() ||
                            (!ghToken.isNullOrBlank() && ghToken != "manual_mode")
                        RoleSelectionScreen(
                            hasGitHubToken = hasToken,
                            onRoleSelected = { role, tokenOrUsername ->
                                sessionViewModel.setRole(role, tokenOrUsername)
                            }
                        )
                    }
                }
            }
            isGuest -> {
                GuestBrowseScreen(
                    onSignUpTapped = { sessionViewModel.exitGuestMode() },
                    onSelectProfile = { selectedProfileForShowcase = it }
                )
            }
            else -> {
                WelcomeScreen(
                    onBrowseAsGuest = { sessionViewModel.enterGuestMode() },
                    onAuthSuccess = { sessionViewModel.checkAuth() }
                )
            }
        }

        // Overlay: Candidate / Profile Showcase Sheet
        AnimatedVisibility(
            visible = selectedProfileForShowcase != null,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(300)) { it } + fadeOut()
        ) {
            if (selectedProfileForShowcase != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GitloftColors.Background)
                ) {
                    Column {
                        // Dismiss Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CANDIDATE SHOWCASE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = GitloftColors.Volt
                            )
                            IconButton(onClick = { selectedProfileForShowcase = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = GitloftColors.TextSecondary)
                            }
                        }

                        ShowcaseScreen(
                            profile = selectedProfileForShowcase!!,
                            isRecruiterView = resolvedRole == UserRole.RECRUITER,
                            onSelectRepo = { selectedRepoForDetail = it }
                        )
                    }
                }
            }
        }

        // Overlay: Deep Link Profile Sheet
        AnimatedVisibility(
            visible = deepLinkUsername != null,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(300)) { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GitloftColors.Background)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEEP LINK SHOWCASE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = GitloftColors.Volt
                        )
                        IconButton(onClick = onDismissDeepLink) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GitloftColors.TextSecondary)
                        }
                    }

                    if (isDeepLinkLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GitloftColors.Volt)
                        }
                    } else if (deepLinkProfile != null) {
                        ShowcaseScreen(
                            profile = deepLinkProfile!!,
                            isRecruiterView = resolvedRole == UserRole.RECRUITER,
                            onSelectRepo = { selectedRepoForDetail = it }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SHOWCASE NOT FOUND",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "@$deepLinkUsername hasn't published yet.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = GitloftColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay: Full Repository Detail Sheet (Top Layer)
        AnimatedVisibility(
            visible = selectedRepoForDetail != null,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(300)) { it } + fadeOut()
        ) {
            if (selectedRepoForDetail != null) {
                RepoDetailScreen(
                    repo = selectedRepoForDetail!!,
                    onBack = { selectedRepoForDetail = null }
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GitloftColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_1024),
                contentDescription = "Gitloft Logo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(8.dp))
            )

            Text(
                text = "GITLOFT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
                color = Color.White
            )

            CircularProgressIndicator(
                color = GitloftColors.Volt,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
