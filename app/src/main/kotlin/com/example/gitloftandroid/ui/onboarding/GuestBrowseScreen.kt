package com.example.gitloftandroid.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitloftandroid.data.model.ShowcaseProfile
import com.example.gitloftandroid.ui.browse.BrowseScreen
import com.example.gitloftandroid.ui.theme.GitloftColors

@Composable
fun GuestBrowseScreen(
    onSignUpTapped: () -> Unit,
    onSelectProfile: (ShowcaseProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
    ) {
        // Guest mode banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GitloftColors.Surface2)
                .border(1.dp, GitloftColors.Volt.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GitloftColors.Volt,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GUEST BROWSE MODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }

            Text(
                text = "[ SIGN IN / JOIN ]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = GitloftColors.Volt,
                modifier = Modifier.clickable(onClick = onSignUpTapped)
            )
        }

        // Embedded Browse Screen
        BrowseScreen(
            onSelectProfile = onSelectProfile,
            modifier = Modifier.weight(1f)
        )
    }
}
