package com.example.gitloftandroid.ui.analytics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.CyberSectionTitle
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.ScreenHeroHeader
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.pro.ProGateDialog
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.AnalyticsViewModel
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(
    username: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { AnalyticsViewModel(context.applicationContext as android.app.Application) }
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val totalViews by viewModel.totalViews.collectAsStateWithLifecycle()
    val totalClicks by viewModel.totalClicks.collectAsStateWithLifecycle()
    val shortlistCount by viewModel.shortlistCount.collectAsStateWithLifecycle()
    val weeklyData by viewModel.weeklyData.collectAsStateWithLifecycle()
    val topRepos by viewModel.topRepos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val profile by sessionViewModel.profile.collectAsStateWithLifecycle()
    val isPro = profile?.isPro == true

    var showProGateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(username) {
        viewModel.loadAnalytics(username)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenHeroHeader(
            title = "ANALYTICS",
            subtitle = "Track portfolio traffic and talent interest.",
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        // Free Tier Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CyberSectionTitle(title = "FREE TIER", detail = "LIVE COUNTS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "PROFILE VIEWS",
                    value = "$totalViews",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "REPO CLICKS",
                    value = "$totalClicks",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pro Section or Paywall prompt
        if (isPro) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CyberSectionTitle(title = "PRO TIER", detail = "ADVANCED")

                StatCard(
                    title = "RECRUITER SHORTLISTS",
                    value = "$shortlistCount",
                    valueColor = GitloftColors.Volt,
                    modifier = Modifier.fillMaxWidth()
                )

                // Weekly Activity Bar Chart
                CyberBentoPanel(padding = 16.dp) {
                    CyberSectionTitle(title = "WEEKLY TRAFFIC", detail = "VIEWS + CLICKS")
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCount = maxOf(weeklyData.maxOfOrNull { it.views + it.clicks } ?: 1, 1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEach { day ->
                            val total = day.views + day.clicks
                            val fraction = (total.toFloat() / maxCount).coerceIn(0.08f, 1f)
                            val dayLabel = day.date.takeLast(2)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(GitloftColors.Volt)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = GitloftColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Top Clicked Repos
                if (topRepos.isNotEmpty()) {
                    CyberBentoPanel(padding = 16.dp) {
                        CyberSectionTitle(title = "TOP REPOSITORIES", detail = "BY CLICKS")
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topRepos.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GitloftColors.Surface2)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${item.repoName}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${item.clicks} clicks",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = GitloftColors.Volt
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Pro Paywall Prompt Card
            CyberBentoPanel(
                padding = 20.dp,
                borderColor = GitloftColors.Volt.copy(alpha = 0.4f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitloftColors.Volt),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "UNLOCK ADVANCED TELEMETRY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "GITLOFT PRO · $5.99/MO",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = GitloftColors.Volt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Gain deep insights with recruiter shortlist metrics, 7-day activity graphs, and detailed repository click performance.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = GitloftColors.TextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryCyberButton(
                    title = "Upgrade to Pro",
                    onClick = { showProGateDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
                    Toast.makeText(context, "Gitloft Pro Activated!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    CyberBentoPanel(
        modifier = modifier,
        padding = 16.dp
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = GitloftColors.TextSecondary
        )
    }
}
