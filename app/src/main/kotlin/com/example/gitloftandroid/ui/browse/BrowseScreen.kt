package com.example.gitloftandroid.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.data.model.supabase.ShowcaseProfileResponse
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.LanguagePill
import com.example.gitloftandroid.ui.components.ScreenHeroHeader
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.BrowseViewModel

@Composable
fun BrowseScreen(
    onSelectProfile: (ShowcaseProfile) -> Unit,
    modifier: Modifier = Modifier,
    headerTitle: String = "DISCOVER SHOWCASES",
    headerSubtitle: String = "Explore curated GitHub portfolios."
) {
    val context = LocalContext.current
    val viewModel = remember { BrowseViewModel(context.applicationContext as android.app.Application) }

    val filteredList by viewModel.filteredShowcases.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchText.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
    ) {
        ScreenHeroHeader(
            title = headerTitle,
            subtitle = headerSubtitle,
            actions = {
                IconButton(
                    onClick = { viewModel.loadShowcases() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = GitloftColors.Volt,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchText.value = it },
                placeholder = {
                    Text(
                        "SEARCH SHOWCASES…",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GitloftColors.TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) GitloftColors.Volt else GitloftColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchText.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = GitloftColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GitloftColors.Volt,
                    unfocusedBorderColor = GitloftColors.Border,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = GitloftColors.Surface,
                    unfocusedContainerColor = GitloftColors.Surface
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) GitloftColors.Volt else GitloftColors.Surface)
                        .border(
                            1.dp,
                            if (isSelected) GitloftColors.Volt else GitloftColors.Border,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.selectedFilter.value = filter }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = if (isSelected) Color.Black else GitloftColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content list
        if (isLoading && filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GitloftColors.Volt)
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO SHOWCASES FOUND",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GitloftColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search or language filter",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GitloftColors.TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredList, key = { it.username }) { item ->
                    ShowcasePreviewCard(
                        showcase = item,
                        onClick = { onSelectProfile(item.asShowcaseProfile) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShowcasePreviewCard(
    showcase: ShowcaseProfileResponse,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GitloftColors.Surface2)
                    .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = showcase.avatarUrl ?: "https://github.com/identicons/${showcase.username}.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${showcase.username.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = GitloftColors.Volt,
                    letterSpacing = 1.sp
                )
                if (!showcase.fullName.isNullOrBlank()) {
                    Text(
                        text = showcase.fullName,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            // Pinned count tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GitloftColors.Surface2)
                    .border(1.dp, GitloftColors.Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${showcase.pinnedRepos.size} PINNED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = GitloftColors.TextSecondary
                )
            }
        }

        if (!showcase.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = showcase.bio,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = GitloftColors.TextSecondary,
                maxLines = 2
            )
        }

        // Language badges row
        val languages = showcase.pinnedRepos.mapNotNull { it.language }.distinct().take(4)
        if (languages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                languages.forEach { lang ->
                    LanguagePill(language = lang)
                }
            }
        }
    }
}
