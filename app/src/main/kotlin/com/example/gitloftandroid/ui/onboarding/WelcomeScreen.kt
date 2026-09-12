package com.example.gitloftandroid.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitloftandroid.R
import com.example.gitloftandroid.ui.auth.AuthSheet
import com.example.gitloftandroid.ui.components.LiveStatusPill
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.components.TechGridBackground
import com.example.gitloftandroid.ui.theme.GitloftColors

@Composable
fun WelcomeScreen(
    onBrowseAsGuest: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthSheet by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appeared = true
    }

    TechGridBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Navbar
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_1024),
                        contentDescription = "Gitloft Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "GITLOFT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                }

                LiveStatusPill(status = "ONLINE", isLive = true)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Hero Section
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Category pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GitloftColors.Surface)
                            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GitloftColors.Volt,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "CURATE • PUBLISH • SHARE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = GitloftColors.TextSecondary
                        )
                    }

                    // Headline
                    Text(
                        text = "YOUR GITHUB,\nMADE UNMISTAKABLE.",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )

                    // Subtitle
                    Text(
                        text = "Turn raw repositories into a premium public showcase with curated highlights, live code previews, and a single terminal-grade link.",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        color = GitloftColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Stats block (3 sharp containers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WelcomeStatBlock(
                            icon = Icons.Default.PushPin,
                            value = "03",
                            label = "PINNED",
                            modifier = Modifier.weight(1f)
                        )
                        WelcomeStatBlock(
                            icon = Icons.Default.Code,
                            value = "LIVE",
                            label = "CODE",
                            modifier = Modifier.weight(1f)
                        )
                        WelcomeStatBlock(
                            icon = Icons.Default.Send,
                            value = "1 TAP",
                            label = "SHARE",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryCyberButton(
                    title = "Create My Showcase",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { showAuthSheet = true }
                )

                SecondaryCyberButton(
                    title = "Browse as Guest",
                    onClick = onBrowseAsGuest
                )

                Text(
                    text = "FREE TO START · GITLOFT PRO FROM $5.99/MO",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = GitloftColors.TextSecondary.copy(alpha = 0.8f)
                )
            }
        }

        if (showAuthSheet) {
            AuthSheet(
                onDismiss = { showAuthSheet = false },
                onSuccess = {
                    showAuthSheet = false
                    onAuthSuccess()
                }
            )
        }
    }
}

@Composable
private fun WelcomeStatBlock(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GitloftColors.Surface)
            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(GitloftColors.Surface2)
                .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GitloftColors.Volt,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            color = GitloftColors.TextSecondary
        )
    }
}
