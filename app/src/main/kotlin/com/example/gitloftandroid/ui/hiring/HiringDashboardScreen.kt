package com.example.gitloftandroid.ui.hiring

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gitloftandroid.R
import com.example.gitloftandroid.data.model.AnalyticsEvent
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.data.model.SavedDeveloper
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.data.repository.LocalDataStore
import com.example.gitloftandroid.ui.browse.BrowseScreen
import com.example.gitloftandroid.ui.components.*
import com.example.gitloftandroid.ui.showcase.ShowcaseScreen
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun HiringDashboardScreen(
    onLogout: () -> Unit,
    onSelectRepo: (GitHubRepo) -> Unit,
    onSelectProfile: (ShowcaseProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }
    val localDataStore = remember { LocalDataStore.getInstance(context) }
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val shortlist by localDataStore.shortlistFlow.collectAsStateWithLifecycle()

    val tabs = listOf(
        CyberTabItem(Icons.Default.Search, "DISCOVER"),
        CyberTabItem(Icons.Default.Bookmark, "SHORTLIST"),
        CyberTabItem(Icons.Default.Person, "SETTINGS")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp)
        ) {
            when (selectedTab) {
                0 -> BrowseScreen(
                    headerTitle = "TALENT ACQUISITION",
                    headerSubtitle = "Discover and shortlist top engineering talent.",
                    onSelectProfile = onSelectProfile
                )
                1 -> ShortlistScreen(
                    shortlist = shortlist,
                    onSelectDeveloper = { dev ->
                        coroutineScope.launch {
                            val resp = supabaseClient.fetchShowcase(dev.username)
                            if (resp != null) {
                                onSelectProfile(resp.asShowcaseProfile)
                            }
                        }
                    },
                    onUpdateRating = { dev, rating ->
                        localDataStore.updateDeveloperRating(dev.username, rating)
                        coroutineScope.launch {
                            supabaseClient.logEvent(
                                AnalyticsEvent(
                                    event_type = "rating_update",
                                    showcase_username = dev.username,
                                    rating = rating,
                                    viewer_role = "recruiter",
                                    viewer_username = sessionViewModel.profile.value?.username
                                )
                            )
                        }
                    },
                    onRemove = { dev ->
                        localDataStore.removeSavedDeveloper(dev.username)
                    }
                )
                2 -> RecruiterSettingsScreen(
                    sessionViewModel = sessionViewModel,
                    onLogout = onLogout
                )
            }
        }

        // Bottom Tab Bar
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
fun ShortlistScreen(
    shortlist: List<SavedDeveloper>,
    onSelectDeveloper: (SavedDeveloper) -> Unit,
    onUpdateRating: (SavedDeveloper, Int) -> Unit,
    onRemove: (SavedDeveloper) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
            .padding(horizontal = 24.dp)
    ) {
        ScreenHeroHeader(
            title = "CANDIDATE SHORTLIST",
            subtitle = "${shortlist.size} developers bookmarked.",
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        if (shortlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = GitloftColors.TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NO CANDIDATES SHORTLISTED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GitloftColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Browse showcases and tap '+ SHORTLIST' to bookmark profiles.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GitloftColors.TextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(shortlist, key = { it.username }) { dev ->
                    CyberBentoPanel(
                        cornerRadius = 8.dp,
                        padding = 16.dp,
                        modifier = Modifier.clickable { onSelectDeveloper(dev) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GitloftColors.Surface2)
                                    .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = dev.avatarUrl ?: "https://github.com/identicons/${dev.username}.png",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${dev.username.uppercase()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = GitloftColors.Volt
                                )
                                if (!dev.fullName.isNullOrBlank()) {
                                    Text(
                                        text = dev.fullName,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onRemove(dev) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = GitloftColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!dev.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dev.bio,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = GitloftColors.TextSecondary,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cyber Rating Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EVALUATION RATING:",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = GitloftColors.TextSecondary
                            )

                            CyberStarRating(
                                rating = dev.rating,
                                onRatingChanged = { newRating ->
                                    onUpdateRating(dev, newRating)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecruiterSettingsScreen(
    sessionViewModel: SessionViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val profile by sessionViewModel.profile.collectAsStateWithLifecycle()

    var fullName by remember { mutableStateOf(profile?.fullName ?: "") }
    var avatarUrl by remember { mutableStateOf(profile?.avatarUrl ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
            .padding(24.dp)
    ) {
        ScreenHeroHeader(
            title = "RECRUITER SETTINGS",
            subtitle = "Manage company profile and authentication.",
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        CyberBentoPanel(padding = 20.dp) {
            Text(
                text = "HIRING MANAGER DETAILS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = GitloftColors.Volt
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("RECRUITER NAME", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
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
                value = avatarUrl,
                onValueChange = { avatarUrl = it },
                label = { Text("AVATAR URL", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
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

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryCyberButton(
                title = "Save Profile",
                isLoading = isSaving,
                onClick = {
                    coroutineScope.launch {
                        isSaving = true
                        try {
                            val uid = try { supabaseClient.fetchCurrentAuthUser() } catch (e: Exception) { UUID.randomUUID() }
                            supabaseClient.upsertProfile(
                                id = uid,
                                username = profile?.username,
                                role = UserRole.RECRUITER,
                                fullName = fullName.trim(),
                                avatarUrl = avatarUrl.trim()
                            )
                            sessionViewModel.hydrateProfile()
                        } catch (e: Exception) { }
                        isSaving = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryCyberButton(
                title = "Switch to Developer Mode",
                textColor = GitloftColors.Volt,
                borderColor = GitloftColors.Volt.copy(alpha = 0.5f),
                onClick = {
                    sessionViewModel.switchRole(UserRole.DEVELOPER)
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
