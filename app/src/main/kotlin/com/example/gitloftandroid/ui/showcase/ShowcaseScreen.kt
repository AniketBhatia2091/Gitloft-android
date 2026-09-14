package com.example.gitloftandroid.ui.showcase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gitloftandroid.data.model.AnalyticsEvent
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.SavedDeveloper
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.data.repository.LocalDataStore
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.CyberSectionTitle
import com.example.gitloftandroid.ui.components.CyberStarRating
import com.example.gitloftandroid.ui.components.LanguagePill
import com.example.gitloftandroid.ui.components.LiveStatusPill
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun ShowcaseScreen(
    profile: ShowcaseProfile,
    isRecruiterView: Boolean = false,
    onSelectRepo: (GitHubRepo) -> Unit,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localDataStore = remember { LocalDataStore.getInstance(context) }
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }
    val coroutineScope = rememberCoroutineScope()

    val shortlist by localDataStore.shortlistFlow.collectAsStateWithLifecycle()
    val savedEntry = remember(shortlist, profile.user.login) {
        shortlist.firstOrNull { it.username.equals(profile.user.login, ignoreCase = true) }
    }
    val isShortlisted = savedEntry != null

    // Log view event once on enter
    LaunchedEffect(profile.user.login) {
        val viewerRole = sessionViewModel.resolvedRole.value?.value ?: "anonymous"
        val viewerUsername = sessionViewModel.profile.value?.username
        supabaseClient.logEvent(
            AnalyticsEvent(
                event_type = "view",
                showcase_username = profile.user.login,
                viewer_role = viewerRole,
                viewer_username = viewerUsername
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Developer HUD Header
        item {
            CyberBentoPanel(padding = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GitloftColors.Surface2)
                            .border(1.5.dp, GitloftColors.Volt, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = profile.user.avatarUrl.takeIf { it.isNotBlank() } ?: "https://github.com/identicons/${profile.user.login}.png",
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "@${profile.user.login.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = GitloftColors.Volt,
                                letterSpacing = 1.2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            LiveStatusPill("ONLINE", isLive = true)
                        }

                        if (!profile.user.name.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.user.name,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                if (!profile.user.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.user.bio,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GitloftColors.TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Language pills
                val languages = profile.pinnedRepos.mapNotNull { it.language }.distinct()
                if (languages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        languages.take(4).forEach { lang ->
                            LanguagePill(language = lang)
                        }
                    }
                }
            }
        }

        // Recruiter Action Bar (only visible to recruiters)
        if (isRecruiterView) {
            item {
                CyberBentoPanel(
                    padding = 14.dp,
                    backgroundColor = GitloftColors.Surface2
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TALENT EVALUATION",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = GitloftColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CyberStarRating(
                                rating = savedEntry?.rating ?: 0,
                                onRatingChanged = { rating ->
                                    if (savedEntry != null) {
                                        localDataStore.updateDeveloperRating(profile.user.login, rating)
                                    } else {
                                        localDataStore.saveDeveloper(
                                            SavedDeveloper(
                                                username = profile.user.login,
                                                fullName = profile.user.name,
                                                avatarUrl = profile.user.avatarUrl,
                                                bio = profile.user.bio,
                                                rating = rating
                                            )
                                        )
                                    }
                                    coroutineScope.launch {
                                        supabaseClient.logEvent(
                                            AnalyticsEvent(
                                                event_type = "rating_update",
                                                showcase_username = profile.user.login,
                                                rating = rating,
                                                viewer_role = "recruiter",
                                                viewer_username = sessionViewModel.profile.value?.username
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        // Shortlist button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isShortlisted) GitloftColors.Volt else GitloftColors.Surface)
                                .border(1.5.dp, if (isShortlisted) GitloftColors.Volt else GitloftColors.Border, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isShortlisted) {
                                        localDataStore.removeSavedDeveloper(profile.user.login)
                                    } else {
                                        localDataStore.saveDeveloper(
                                            SavedDeveloper(
                                                username = profile.user.login,
                                                fullName = profile.user.name,
                                                avatarUrl = profile.user.avatarUrl,
                                                bio = profile.user.bio,
                                                rating = 5
                                            )
                                        )
                                        coroutineScope.launch {
                                            supabaseClient.logEvent(
                                                AnalyticsEvent(
                                                    event_type = "shortlist_add",
                                                    showcase_username = profile.user.login,
                                                    viewer_role = "recruiter",
                                                    viewer_username = sessionViewModel.profile.value?.username
                                                )
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isShortlisted) "[ SHORTLISTED ]" else "+ SHORTLIST",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = if (isShortlisted) Color.Black else GitloftColors.Volt
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            CyberSectionTitle(
                title = "CURATED WORK",
                detail = "${profile.pinnedRepos.size} PINNED"
            )
        }

        // Pinned Repositories List
        items(profile.pinnedRepos, key = { it.id }) { repo ->
            ShowcaseRepoCard(
                repo = repo,
                onClick = {
                    // Log repo click telemetry
                    coroutineScope.launch {
                        val viewerRole = sessionViewModel.resolvedRole.value?.value ?: "anonymous"
                        supabaseClient.logEvent(
                            AnalyticsEvent(
                                event_type = "click",
                                showcase_username = profile.user.login,
                                repo_name = repo.name,
                                target_url = repo.htmlUrl,
                                viewer_role = viewerRole,
                                viewer_username = sessionViewModel.profile.value?.username
                            )
                        )
                    }
                    onSelectRepo(repo)
                }
            )
        }

        // Logout / Switch footer if provided
        if (onLogout != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SecondaryCyberButton(
                    title = "Switch to Hiring Mode",
                    textColor = GitloftColors.Volt,
                    borderColor = GitloftColors.Volt.copy(alpha = 0.5f),
                    onClick = {
                        sessionViewModel.switchRole(com.example.gitloftandroid.data.model.UserRole.RECRUITER)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                SecondaryCyberButton(
                    title = "Sign Out of Gitloft",
                    textColor = GitloftColors.Error,
                    borderColor = GitloftColors.Error.copy(alpha = 0.4f),
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun ShowcaseRepoCard(
    repo: GitHubRepo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberBentoPanel(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 8.dp,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GitloftColors.colorForLanguage(repo.language))
                )
                Text(
                    text = repo.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Text(
                text = "★ ${repo.stargazersCount}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = GitloftColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = repo.displayDescription,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = GitloftColors.TextSecondary,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (repo.language != null) {
                LanguagePill(language = repo.language)
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (repo.customLink != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GitloftColors.Volt.copy(alpha = 0.1f))
                            .border(1.dp, GitloftColors.Volt, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "[ ${repo.customLink!!.displayTitle.uppercase()} ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = GitloftColors.Volt
                        )
                    }
                }

                Text(
                    text = "[ VIEW CODE & README ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = GitloftColors.TextSecondary
                )
            }
        }
    }
}
