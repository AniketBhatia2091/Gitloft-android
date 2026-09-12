package com.example.gitloftandroid.ui.curator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.gitloftandroid.BuildConfig
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gitloftandroid.R
import com.example.gitloftandroid.data.model.CustomLink
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.StoredRepo
import com.example.gitloftandroid.data.model.supabase.NotificationEvent
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.ui.analytics.AnalyticsScreen
import com.example.gitloftandroid.ui.browse.BrowseScreen
import com.example.gitloftandroid.ui.components.*
import com.example.gitloftandroid.ui.pro.ProGateDialog
import com.example.gitloftandroid.ui.share.ShareScreen
import com.example.gitloftandroid.ui.showcase.ShowcaseScreen
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.CuratorViewModel
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun CuratorDashboardScreen(
    onLogout: () -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit,
    onSelectProfile: (ShowcaseProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val curatorViewModel = remember { CuratorViewModel(context.applicationContext as android.app.Application) }
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }

    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        CyberTabItem(Icons.Default.Dashboard, "HOME"),
        CyberTabItem(Icons.Default.Public, "DISCOVER"),
        CyberTabItem(Icons.Default.Person, "PROFILE"),
        CyberTabItem(Icons.Default.Share, "SHARE"),
        CyberTabItem(Icons.Default.BarChart, "STATS")
    )

    val homeNavRequest by sessionViewModel.homeNavigationRequest.collectAsStateWithLifecycle()
    LaunchedEffect(homeNavRequest) {
        if (homeNavRequest > 0) {
            selectedTab = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
    ) {
        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp)
        ) {
            when (selectedTab) {
                0 -> CuratorHomeScreen(
                    viewModel = curatorViewModel,
                    onNavigateToStats = { selectedTab = 4 },
                    onSelectRepo = onSelectRepo
                )
                1 -> BrowseScreen(
                    onSelectProfile = onSelectProfile
                )
                2 -> {
                    val profile = curatorViewModel.buildShowcaseProfile()
                    if (profile != null) {
                        ShowcaseScreen(
                            profile = profile,
                            isRecruiterView = false,
                            onSelectRepo = onSelectRepo,
                            onLogout = onLogout
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CyberBentoPanel(
                                cornerRadius = 12.dp,
                                padding = 20.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "NO GITHUB PROFILE LINKED",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = GitloftColors.Volt,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Connect your GitHub username on the Home tab to curate repositories and preview your showcase.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = GitloftColors.TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PrimaryCyberButton(
                                        title = "Go to Home Dashboard",
                                        onClick = { selectedTab = 0 }
                                    )
                                    SecondaryCyberButton(
                                        title = "Sign Out",
                                        onClick = onLogout
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> ShareScreen(
                    username = curatorViewModel.currentUser.collectAsStateWithLifecycle().value?.login ?: "username"
                )
                4 -> AnalyticsScreen(
                    username = curatorViewModel.currentUser.collectAsStateWithLifecycle().value?.login ?: "username"
                )
            }
        }

        // Docked Cyber Bottom Bar
        CyberBottomTabBar(
            tabs = tabs,
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun CuratorHomeScreen(
    viewModel: CuratorViewModel,
    onNavigateToStats: () -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allRepos by viewModel.allRepos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val publishSuccess by viewModel.publishSuccess.collectAsStateWithLifecycle()
    val isShowcaseLive by viewModel.isShowcaseLive.collectAsStateWithLifecycle()
    val profileStats by viewModel.profileStats.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()

    val pinnedRepos = remember(allRepos) { allRepos.filter { it.isPinned }.sortedBy { it.orderIndex } }

    var showEditRepoDialog by remember { mutableStateOf<StoredRepo?>(null) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showProGateDialog by remember { mutableStateOf(false) }

    val username = currentUser?.login ?: "username"
    val webUrl = "https://gitloft.vercel.app/u/$username"

    // Language DNA
    val languageCounts = remember(allRepos) {
        val map = mutableMapOf<String, Int>()
        allRepos.forEach { repo ->
            val lang = repo.language ?: "Other"
            map[lang] = (map[lang] ?: 0) + 1
        }
        map.entries.sortedByDescending { it.value }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            ScreenHeroHeader(
                title = "DASHBOARD",
                subtitle = "Manage presence and track impact.",
                modifier = Modifier.padding(horizontal = 0.dp),
                actions = {
                    // Settings button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitloftColors.Surface2)
                            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = GitloftColors.Volt,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Notifications button
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitloftColors.Surface2)
                            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = GitloftColors.Volt,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Sync button
                    IconButton(
                        onClick = { viewModel.syncRepos() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitloftColors.Surface2)
                            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = GitloftColors.Volt,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Repos",
                                tint = GitloftColors.Volt,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )
        }

        // Link GitHub Card if no user profile linked
        if (currentUser == null) {
            item {
                CyberBentoPanel(
                    cornerRadius = 8.dp,
                    padding = 16.dp,
                    borderColor = GitloftColors.Volt.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "LINK YOUR GITHUB ACCOUNT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = GitloftColors.Volt,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To protect developer identity and prevent impersonation, you must authenticate ownership of your GitHub account.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GitloftColors.TextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryCyberButton(
                        title = "Authorize with GitHub",
                        icon = Icons.Default.Code,
                        onClick = {
                            val authUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/authorize?provider=github&redirect_to=gitloft://oauth-callback&scopes=read:user%20user:email%20repo"
                            launchOAuthCustomTab(context, authUrl)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    var isPatVisible by remember { mutableStateOf(false) }
                    var patInput by remember { mutableStateOf("") }
                    var patLoading by remember { mutableStateOf(false) }
                    var patError by remember { mutableStateOf<String?>(null) }

                    if (!isPatVisible) {
                        SecondaryCyberButton(
                            title = "Link with Personal Access Token (PAT)",
                            icon = Icons.Default.Key,
                            onClick = { isPatVisible = true }
                        )
                    } else {
                        OutlinedTextField(
                            value = patInput,
                            onValueChange = { patInput = it; patError = null },
                            label = { Text("GITHUB PAT (ghp_... OR github_pat_...)", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                            placeholder = { Text("ghp_...", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GitloftColors.Volt,
                                unfocusedBorderColor = GitloftColors.Border,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = GitloftColors.Surface2,
                                unfocusedContainerColor = GitloftColors.Surface2
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (patError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = patError!!.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = GitloftColors.Error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PrimaryCyberButton(
                            title = "Verify & Link Token",
                            icon = Icons.Default.Check,
                            isLoading = patLoading,
                            enabled = patInput.isNotBlank(),
                            onClick = {
                                patLoading = true
                                patError = null
                                viewModel.linkGitHubAccount(patInput) { success, err ->
                                    patLoading = false
                                    if (!success) {
                                        patError = err ?: "Invalid GitHub token."
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "[ CANCEL ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = GitloftColors.TextSecondary,
                            modifier = Modifier
                                .clickable { isPatVisible = false }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // Error message banner (only if not unlinked notice)
        if (errorMessage != null && currentUser != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GitloftColors.Error.copy(alpha = 0.1f))
                        .border(1.dp, GitloftColors.Error.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    val displayMsg = if (errorMessage!!.contains("401")) {
                        "GITHUB AUTH REQUIRED: PLEASE RE-LINK YOUR GITHUB ACCOUNT."
                    } else {
                        errorMessage!!.uppercase()
                    }
                    Text(
                        text = displayMsg,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = GitloftColors.Error
                    )
                }
            }
        }

        // Live Showcase Status Banner
        item {
            CyberBentoPanel(
                cornerRadius = 8.dp,
                padding = 16.dp,
                borderColor = if (isShowcaseLive) GitloftColors.Volt.copy(alpha = 0.6f) else GitloftColors.Border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveStatusPill(
                        status = if (isShowcaseLive) "LIVE SHOWCASE" else "DRAFT MODE",
                        isLive = isShowcaseLive
                    )

                    Text(
                        text = "gitloft.app/u/$username",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = GitloftColors.Volt
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryCyberButton(
                        title = "Copy Link",
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Showcase URL", webUrl))
                            Toast.makeText(context, "Showcase URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PrimaryCyberButton(
                        title = if (publishSuccess) "PUBLISHED!" else if (isPublishing) "PUBLISHING..." else "PUBLISH SHOWCASE",
                        icon = if (publishSuccess) Icons.Default.Check else Icons.Default.Publish,
                        isLoading = isPublishing,
                        modifier = Modifier.weight(1.3f),
                        onClick = { viewModel.publishShowcase() }
                    )
                }
            }
        }

        // Language DNA Section
        if (languageCounts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberSectionTitle(title = "LANGUAGE DNA", detail = "${languageCounts.size} LANGUAGES")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languageCounts.take(3).forEach { entry ->
                            CyberBentoPanel(
                                modifier = Modifier.weight(1f),
                                padding = 10.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GitloftColors.colorForLanguage(entry.key))
                                    )
                                    Text(
                                        text = entry.key.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${entry.value} repos",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = GitloftColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned Repositories Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberSectionTitle(
                    title = "PINNED REPOSITORIES",
                    detail = "${pinnedRepos.size} / ${viewModel.pinLimit}"
                )
            }
        }

        if (pinnedRepos.isEmpty()) {
            item {
                CyberBentoPanel(
                    padding = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "NO PINNED REPOSITORIES",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GitloftColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pin up to ${viewModel.pinLimit} public repositories to feature on your showcase.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = GitloftColors.TextSecondary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PrimaryCyberButton(
                            title = "Choose From Library",
                            icon = Icons.Default.Add,
                            onClick = { showLibraryDialog = true }
                        )
                    }
                }
            }
        } else {
            itemsIndexed(pinnedRepos, key = { _, repo -> repo.id }) { index, repo ->
                CuratorRepoRow(
                    repo = repo,
                    index = index,
                    totalCount = pinnedRepos.size,
                    onMoveUp = { viewModel.moveRepo(index, index - 1) },
                    onMoveDown = { viewModel.moveRepo(index, index + 1) },
                    onEdit = { showEditRepoDialog = repo },
                    onUnpin = { viewModel.togglePin(repo) },
                    onClick = {
                        val ghRepo = GitHubRepo(
                            id = repo.id,
                            name = repo.name,
                            description = repo.originalDescription,
                            customDescription = repo.customDescription,
                            language = repo.language,
                            stargazersCount = repo.stargazersCount,
                            isPrivate = repo.isPrivate,
                            htmlUrl = repo.htmlUrlString,
                            customLink = repo.customLink,
                            hideGitHubLink = repo.hideGitHubLink
                        )
                        onSelectRepo(ghRepo)
                    }
                )
            }

            // Add more repo button if under limit
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryCyberButton(
                        title = "+ Add From Library",
                        modifier = Modifier.weight(1f),
                        onClick = { showLibraryDialog = true }
                    )

                    if (viewModel.pinLimit < 6) {
                        SecondaryCyberButton(
                            title = "★ Upgrade to 6 Pins",
                            textColor = GitloftColors.Volt,
                            borderColor = GitloftColors.Volt.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1.2f),
                            onClick = { showProGateDialog = true }
                        )
                    }
                }
            }
        }

        // Quick Stats Strip
        item {
            CyberBentoPanel(
                cornerRadius = 8.dp,
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberSectionTitle(title = "TELEMETRY", detail = "LIVE COUNTS")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profileStats?.views ?: 0}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = GitloftColors.Volt
                        )
                        Text(
                            text = "PROFILE VIEWS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = GitloftColors.TextSecondary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profileStats?.clicks ?: 0}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "REPO CLICKS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = GitloftColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SecondaryCyberButton(
                    title = "[ View Full Stats & Analytics ]",
                    onClick = onNavigateToStats
                )
            }
        }
    }

    // Dialogs
    if (showEditRepoDialog != null) {
        RepoEditDialog(
            repo = showEditRepoDialog!!,
            onDismiss = { showEditRepoDialog = null },
            onSave = { desc, linkType, linkUrl, hideGh ->
                viewModel.updateRepoCustomization(showEditRepoDialog!!.id, desc, linkType, linkUrl, hideGh)
                showEditRepoDialog = null
            }
        )
    }

    if (showLibraryDialog) {
        RepoLibraryDialog(
            repos = allRepos,
            pinLimit = viewModel.pinLimit,
            onDismiss = { showLibraryDialog = false },
            onTogglePin = { viewModel.togglePin(it) }
        )
    }

    if (showSettingsDialog) {
        DeveloperSettingsDialog(
            currentUser = currentUser,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, avatar, bio ->
                viewModel.saveDeveloperProfile(name, avatar, bio) {
                    showSettingsDialog = false
                }
            },
            onLogout = {
                showSettingsDialog = false
                sessionViewModel.logout()
            },
            onSwitchRole = {
                showSettingsDialog = false
                sessionViewModel.switchRole(com.example.gitloftandroid.data.model.UserRole.RECRUITER)
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            username = username,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showProGateDialog) {
        ProGateDialog(
            onDismiss = { showProGateDialog = false },
            onUpgrade = {
                coroutineScope.launch {
                    val uid = try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { null }
                    if (uid != null) {
                        try { supabaseClient.updatePlan(uid.toString(), "pro") } catch (e: Exception) { }
                    }
                    sessionViewModel.hydrateProfile()
                    showProGateDialog = false
                    Toast.makeText(context, "Upgraded to Gitloft Pro!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun CuratorRepoRow(
    repo: StoredRepo,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onUnpin: () -> Unit,
    onClick: () -> Unit
) {
    CyberBentoPanel(
        cornerRadius = 8.dp,
        padding = 14.dp,
        modifier = Modifier.clickable(onClick = onClick)
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
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Move / Edit / Unpin controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = GitloftColors.TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = GitloftColors.TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Customization", tint = GitloftColors.Volt, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Unpin", tint = GitloftColors.Error, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = repo.displayDescription,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = GitloftColors.TextSecondary,
            lineHeight = 15.sp,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (repo.language != null) {
                    LanguagePill(language = repo.language)
                }
                Text(
                    text = "★ ${repo.stargazersCount}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = GitloftColors.TextSecondary
                )
            }

            if (repo.customLink != null) {
                Text(
                    text = "[ ${repo.customLink!!.displayTitle.uppercase()} ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = GitloftColors.Volt
                )
            }
        }
    }
}

// MARK: - Repo Edit Dialog
@Composable
fun RepoEditDialog(
    repo: StoredRepo,
    onDismiss: () -> Unit,
    onSave: (desc: String?, linkType: String?, linkUrl: String?, hideGitHub: Boolean) -> Unit
) {
    var description by remember { mutableStateOf(repo.customDescription ?: repo.originalDescription ?: "") }
    var linkType by remember { mutableStateOf(repo.customLinkType ?: "Live Demo") }
    var linkUrl by remember { mutableStateOf(repo.customLinkUrl ?: "") }
    var hideGitHub by remember { mutableStateOf(repo.hideGitHubLink) }

    val linkTypes = listOf("Live Demo", "App Store", "Website", "View")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberBentoPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                cornerRadius = 12.dp,
                padding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT: ${repo.name.uppercase()}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = GitloftColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("CUSTOM DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Link Type Selector
                Text(
                    text = "ACTION LINK TYPE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = GitloftColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    linkTypes.forEach { type ->
                        val isSelected = linkType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) GitloftColors.Volt else GitloftColors.Surface2)
                                .border(1.dp, if (isSelected) GitloftColors.Volt else GitloftColors.Border, RoundedCornerShape(4.dp))
                                .clickable { linkType = type }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (isSelected) Color.Black else GitloftColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Link URL field
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("LINK TARGET URL", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    placeholder = { Text("https://...", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hide GitHub link toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HIDE GITHUB SOURCE LINK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = hideGitHub,
                        onCheckedChange = { hideGitHub = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GitloftColors.Volt,
                            uncheckedTrackColor = GitloftColors.Surface2
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryCyberButton(
                    title = "Save Changes",
                    onClick = {
                        val cleanDesc = description.trim().takeIf { it.isNotBlank() }
                        val cleanUrl = linkUrl.trim().takeIf { it.isNotBlank() }
                        val cleanType = if (cleanUrl != null) linkType else null
                        onSave(cleanDesc, cleanType, cleanUrl, hideGitHub)
                    }
                )
            }
        }
    }
}

// MARK: - Repo Library Dialog
@Composable
fun RepoLibraryDialog(
    repos: List<StoredRepo>,
    pinLimit: Int,
    onDismiss: () -> Unit,
    onTogglePin: (StoredRepo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(repos, query) {
        if (query.isBlank()) repos else repos.filter { it.name.contains(query, ignoreCase = true) }
    }
    val pinnedCount = repos.count { it.isPinned }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberBentoPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                cornerRadius = 12.dp,
                padding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REPOSITORY LIBRARY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$pinnedCount / $pinLimit PINNED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = GitloftColors.Volt
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = GitloftColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("FILTER REPOSITORIES…", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { repo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GitloftColors.Surface2)
                                .border(1.dp, if (repo.isPinned) GitloftColors.Volt else GitloftColors.Border, RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repo.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                if (repo.language != null) {
                                    Text(
                                        text = repo.language.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = GitloftColors.colorForLanguage(repo.language)
                                    )
                                }
                            }

                            Text(
                                text = if (repo.isPinned) "[ PINNED ]" else "[ PIN ]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (repo.isPinned) GitloftColors.Volt else GitloftColors.TextSecondary,
                                modifier = Modifier
                                    .clickable { onTogglePin(repo) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PrimaryCyberButton(
                    title = "Done",
                    onClick = onDismiss
                )
            }
        }
    }
}

// MARK: - Developer Settings Dialog
@Composable
fun DeveloperSettingsDialog(
    currentUser: com.example.gitloftandroid.data.model.GitHubUser?,
    onDismiss: () -> Unit,
    onSave: (name: String, avatar: String, bio: String) -> Unit,
    onLogout: () -> Unit,
    onSwitchRole: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var avatar by remember { mutableStateOf(currentUser?.avatarUrl ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberBentoPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                cornerRadius = 12.dp,
                padding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROFILE SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = GitloftColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("DISPLAY NAME", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("AVATAR IMAGE URL", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("ONE-LINE BIO / HEADLINE", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitloftColors.Volt,
                        unfocusedBorderColor = GitloftColors.Border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = GitloftColors.Surface2,
                        unfocusedContainerColor = GitloftColors.Surface2
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryCyberButton(
                    title = "Save Profile",
                    onClick = { onSave(name.trim(), avatar.trim(), bio.trim()) }
                )

                if (onSwitchRole != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryCyberButton(
                        title = "Switch to Hiring Mode",
                        textColor = GitloftColors.Volt,
                        borderColor = GitloftColors.Volt.copy(alpha = 0.5f),
                        onClick = onSwitchRole
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "[ SIGN OUT OF GITLOFT ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = GitloftColors.Error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLogout)
                        .padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// MARK: - Notifications Dialog
@Composable
fun NotificationsDialog(
    username: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    var events by remember { mutableStateOf<List<NotificationEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(username) {
        isLoading = true
        try {
            events = supabaseClient.fetchNotificationEvents(username, includeViews = true)
        } catch (e: Exception) { }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberBentoPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                cornerRadius = 12.dp,
                padding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTIFICATIONS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = GitloftColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GitloftColors.Volt)
                    }
                } else if (events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO NOTIFICATIONS YET",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GitloftColors.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events, key = { it.id }) { item ->
                            val title = when (item.eventType) {
                                "rating_update" -> "★ Recruiter rated your showcase ${item.rating ?: 5} stars"
                                "shortlist_add" -> "+ Recruiter added you to their candidate shortlist"
                                "click" -> "↗ Recruiter clicked repo: ${item.repoName ?: "repository"}"
                                "view" -> "👁 New profile view"
                                else -> "Activity recorded"
                            }
                            CyberBentoPanel(
                                padding = 10.dp,
                                backgroundColor = GitloftColors.Surface2
                            ) {
                                Text(
                                    text = title,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.createdAt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = GitloftColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PrimaryCyberButton(
                    title = "Close",
                    onClick = onDismiss
                )
            }
        }
    }
}

private fun launchOAuthCustomTab(context: Context, url: String) {
    try {
        val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

