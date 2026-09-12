package com.example.gitloftandroid.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.TechGridBackground
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.util.InputValidator

@Composable
fun RoleSelectionScreen(
    hasGitHubToken: Boolean,
    onRoleSelected: (UserRole, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf(UserRole.DEVELOPER) }
    var manualUsername by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    TechGridBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header Icon Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GitloftColors.Surface2)
                    .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = GitloftColors.Volt,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CHOOSE YOUR PATH",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 1.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "shapes the workspace tools you access",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = GitloftColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Role Options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RoleSelectionCard(
                    role = UserRole.DEVELOPER,
                    title = "DEVELOPER PORTFOLIO",
                    subtitle = "CURATE REPOSITORIES, HIGHLIGHT CODE DNA, AND TRACK VIEWS.",
                    icon = Icons.Default.Code,
                    isSelected = selectedRole == UserRole.DEVELOPER,
                    onSelect = { selectedRole = UserRole.DEVELOPER }
                )

                RoleSelectionCard(
                    role = UserRole.RECRUITER,
                    title = "TALENT ACQUISITION",
                    subtitle = "DISCOVER TOP TALENT, DEEP DIVE INTO REPOS, AND BUILD SHORTLISTS.",
                    icon = Icons.Default.Search,
                    isSelected = selectedRole == UserRole.RECRUITER,
                    onSelect = { selectedRole = UserRole.RECRUITER }
                )
            }

            // GitHub PAT field if developer role and no github token
            AnimatedVisibility(visible = selectedRole == UserRole.DEVELOPER && !hasGitHubToken) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GitloftColors.Surface)
                        .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "GITHUB USERNAME OR PAT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = GitloftColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualUsername,
                        onValueChange = { 
                            manualUsername = it 
                            errorMessage = null
                        },
                        placeholder = { Text("e.g. octocat or ghp_...", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = GitloftColors.Error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryCyberButton(
                title = "Continue",
                icon = Icons.Default.ArrowForward,
                isLoading = isSubmitting,
                enabled = !(selectedRole == UserRole.DEVELOPER && !hasGitHubToken && manualUsername.isBlank()),
                onClick = {
                    val input = manualUsername.trim()
                    if (selectedRole == UserRole.DEVELOPER && !hasGitHubToken && input.isEmpty()) {
                        errorMessage = "Please enter your GitHub username or token."
                        return@PrimaryCyberButton
                    }
                    val tokenParam = if (selectedRole == UserRole.DEVELOPER && !hasGitHubToken) input else null
                    isSubmitting = true
                    onRoleSelected(selectedRole, tokenParam)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleSelectionCard(
    role: UserRole,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GitloftColors.Surface)
            .border(
                1.5.dp,
                if (isSelected) GitloftColors.Volt else GitloftColors.Border,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) GitloftColors.Volt else GitloftColors.Surface2)
                .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 9.sp,
                color = GitloftColors.TextSecondary,
                lineHeight = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    1.5.dp,
                    if (isSelected) GitloftColors.Volt else GitloftColors.Border,
                    RoundedCornerShape(4.dp)
                )
                .background(if (isSelected) GitloftColors.Volt else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
