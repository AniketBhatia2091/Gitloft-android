package com.example.gitloftandroid.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gitloftandroid.BuildConfig
import com.example.gitloftandroid.R
import com.example.gitloftandroid.data.model.UserRole
import com.example.gitloftandroid.data.network.supabase.SupabaseClient
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.SessionViewModel
import com.example.gitloftandroid.util.InputValidator
import kotlinx.coroutines.launch

@Composable
fun AuthSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionViewModel = remember { SessionViewModel.getInstance(context.applicationContext as android.app.Application) }
    val supabaseClient = remember { SupabaseClient.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var isEmailFormVisible by remember { mutableStateOf(false) }
    var isDirectGitHubVisible by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gitHubInput by remember { mutableStateOf("") }

    var isSigningUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var resetEmailSent by remember { mutableStateOf(false) }
    var emailConfirmationSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .imePadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberBentoPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                cornerRadius = 12.dp,
                padding = 24.dp
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Header with close button
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
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "GITLOFT ACCESS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GitloftColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sign in to curate repositories, customize highlights, and track your telemetry.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GitloftColors.TextSecondary,
                        lineHeight = 16.sp
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GitloftColors.Error.copy(alpha = 0.1f))
                                .border(1.dp, GitloftColors.Error.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = GitloftColors.Error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when {
                        isEmailFormVisible -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (isForgotPassword) {
                                    // MARK: - Password Recovery Mode
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GitloftColors.Volt.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .border(1.dp, GitloftColors.Volt.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "[ MODE: PASSWORD RECOVERY ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = GitloftColors.Volt,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    Text(
                                        text = "Enter your registered email address. We will dispatch a secure link to reset your credentials.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = GitloftColors.TextSecondary,
                                        lineHeight = 14.sp
                                    )

                                    if (resetEmailSent) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(GitloftColors.Success.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, GitloftColors.Success.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "RECOVERY LINK DISPATCHED. Check your inbox and follow the instructions to reset your password.",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = GitloftColors.Success,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        SecondaryCyberButton(
                                            title = "Return to Sign In",
                                            onClick = {
                                                isForgotPassword = false
                                                resetEmailSent = false
                                                errorMessage = null
                                            }
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = email,
                                            onValueChange = { email = it; errorMessage = null },
                                            label = { Text("EMAIL ADDRESS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
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

                                        Spacer(modifier = Modifier.height(4.dp))

                                        PrimaryCyberButton(
                                            title = "Send Recovery Link",
                                            isLoading = isLoading,
                                            enabled = email.isNotBlank() && email.contains("@"),
                                            onClick = {
                                                coroutineScope.launch {
                                                    isLoading = true
                                                    errorMessage = null
                                                    try {
                                                        supabaseClient.resetPasswordForEmail(email.trim())
                                                        resetEmailSent = true
                                                    } catch (e: Exception) {
                                                        val raw = e.localizedMessage ?: "Failed to dispatch recovery link."
                                                        errorMessage = if (raw.contains("rate limit", ignoreCase = true) || raw.contains("429")) {
                                                            "Email rate limit reached by backend. Please wait a few minutes before trying again."
                                                        } else {
                                                            raw
                                                        }
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[ BACK TO SIGN IN ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = GitloftColors.Volt,
                                            modifier = Modifier.clickable {
                                                isForgotPassword = false
                                                errorMessage = null
                                            }
                                        )

                                        Text(
                                            text = "[ CANCEL ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = GitloftColors.TextSecondary,
                                            modifier = Modifier.clickable {
                                                isEmailFormVisible = false
                                                isForgotPassword = false
                                                errorMessage = null
                                            }
                                        )
                                    }
                                } else if (isSigningUp) {
                                    // MARK: - Create Account Mode
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GitloftColors.Volt.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .border(1.dp, GitloftColors.Volt.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "[ MODE: REGISTER NEW ACCOUNT ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = GitloftColors.Volt,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    if (emailConfirmationSent) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(GitloftColors.Success.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, GitloftColors.Success.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(14.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "VERIFICATION EMAIL DISPATCHED",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = GitloftColors.Success
                                                )
                                                Text(
                                                    text = "We have sent an activation link to:\n$email\n\nPlease check your inbox and click the link to confirm your account, then sign in.",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        SecondaryCyberButton(
                                            title = "Proceed to Sign In",
                                            onClick = {
                                                emailConfirmationSent = false
                                                isSigningUp = false
                                                errorMessage = null
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = "Create your new Gitloft profile credentials with email and password.",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = GitloftColors.TextSecondary,
                                            lineHeight = 14.sp
                                        )

                                        OutlinedTextField(
                                            value = email,
                                            onValueChange = { email = it; errorMessage = null },
                                            label = { Text("EMAIL ADDRESS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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

                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it; errorMessage = null },
                                            label = { Text("PASSWORD (MIN 6 CHARACTERS)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
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

                                        OutlinedTextField(
                                            value = confirmPassword,
                                            onValueChange = { confirmPassword = it; errorMessage = null },
                                            label = { Text("CONFIRM PASSWORD", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) GitloftColors.Error else GitloftColors.Volt,
                                                unfocusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) GitloftColors.Error.copy(alpha = 0.5f) else GitloftColors.Border,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = GitloftColors.Surface2,
                                                unfocusedContainerColor = GitloftColors.Surface2
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                                            Text(
                                                text = "Passwords do not match.",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = GitloftColors.Error
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        PrimaryCyberButton(
                                            title = "Create Account",
                                            isLoading = isLoading,
                                            enabled = email.isNotBlank() && password.length >= 6 && password == confirmPassword,
                                            onClick = {
                                                coroutineScope.launch {
                                                    isLoading = true
                                                    errorMessage = null
                                                    try {
                                                        val result = supabaseClient.signUpWithEmail(email.trim(), password)
                                                        when (result) {
                                                            is com.example.gitloftandroid.data.network.supabase.SignUpResult.Authenticated -> {
                                                                sessionViewModel.hydrateProfile()
                                                                onSuccess()
                                                            }
                                                            is com.example.gitloftandroid.data.network.supabase.SignUpResult.ConfirmationRequired -> {
                                                                emailConfirmationSent = true
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        val raw = e.localizedMessage ?: "Signup failed."
                                                        errorMessage = when {
                                                            raw.contains("rate limit", ignoreCase = true) || raw.contains("429") ->
                                                                "Email rate limit reached by backend. Please wait a few minutes before trying again, or use Google/GitHub sign-in."
                                                            raw.contains("already registered", ignoreCase = true) ->
                                                                "An account with this email already exists. Please switch to Sign In."
                                                            else -> raw
                                                        }
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[ ALREADY HAVE ACCOUNT? SIGN IN ]",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = GitloftColors.Volt,
                                                modifier = Modifier.clickable {
                                                    isSigningUp = false
                                                    errorMessage = null
                                                }
                                            )

                                            Text(
                                                text = "[ BACK ]",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = GitloftColors.TextSecondary,
                                                modifier = Modifier.clickable { isEmailFormVisible = false }
                                            )
                                        }
                                    }
                                } else {
                                    // MARK: - Sign In Mode
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GitloftColors.Volt.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .border(1.dp, GitloftColors.Volt.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "[ MODE: OPERATOR SIGN IN ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = GitloftColors.Volt,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it; errorMessage = null },
                                        label = { Text("EMAIL ADDRESS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it; errorMessage = null },
                                            label = { Text("PASSWORD", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
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

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "[ FORGOT PASSWORD? ]",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = GitloftColors.Volt,
                                                modifier = Modifier
                                                    .clickable {
                                                        isForgotPassword = true
                                                        resetEmailSent = false
                                                        errorMessage = null
                                                    }
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    PrimaryCyberButton(
                                        title = "Confirm Login",
                                        isLoading = isLoading,
                                        enabled = email.isNotBlank() && password.length >= 6,
                                        onClick = {
                                            coroutineScope.launch {
                                                isLoading = true
                                                errorMessage = null
                                                try {
                                                    supabaseClient.loginWithEmail(email.trim(), password)
                                                    sessionViewModel.hydrateProfile()
                                                    onSuccess()
                                                } catch (e: Exception) {
                                                    val raw = e.localizedMessage ?: "Authentication failed."
                                                    errorMessage = if (raw.contains("Email not confirmed", ignoreCase = true)) {
                                                        "Email not confirmed. Please check your inbox and verify your email before logging in."
                                                    } else {
                                                        raw
                                                    }
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[ NEED ACCOUNT? SIGN UP ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = GitloftColors.Volt,
                                            modifier = Modifier.clickable {
                                                isSigningUp = true
                                                errorMessage = null
                                            }
                                        )

                                        Text(
                                            text = "[ BACK ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = GitloftColors.TextSecondary,
                                            modifier = Modifier.clickable { isEmailFormVisible = false }
                                        )
                                    }
                                }
                            }
                        }
                        isDirectGitHubVisible -> {
                            // Direct GitHub Personal Access Token Form
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "GITHUB PERSONAL ACCESS TOKEN",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = GitloftColors.Volt
                                )
                                Text(
                                    text = "Enter your GitHub Personal Access Token (classic 'ghp_...' or fine-grained 'github_pat_...') to securely authenticate your identity and enable showcase publishing.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = GitloftColors.TextSecondary,
                                    lineHeight = 13.sp
                                )

                                OutlinedTextField(
                                    value = gitHubInput,
                                    onValueChange = { 
                                        gitHubInput = it 
                                        errorMessage = null
                                    },
                                    label = { Text("TOKEN (ghp_... OR github_pat_...)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
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

                                Spacer(modifier = Modifier.height(4.dp))

                                PrimaryCyberButton(
                                    title = "Verify & Sign In",
                                    isLoading = isLoading,
                                    enabled = gitHubInput.isNotBlank(),
                                    onClick = {
                                        val token = gitHubInput.trim()
                                        if (!InputValidator.isValidGitHubToken(token)) {
                                            errorMessage = "A valid GitHub Personal Access Token starting with 'ghp_' or 'github_pat_' is required. Raw usernames are not permitted."
                                            return@PrimaryCyberButton
                                        }

                                        isLoading = true
                                        errorMessage = null
                                        sessionViewModel.loginWithGitHubToken(token) { success, err ->
                                            isLoading = false
                                            if (success) {
                                                onSuccess()
                                                onDismiss()
                                            } else {
                                                errorMessage = err ?: "GitHub token validation failed."
                                            }
                                        }
                                    }
                                )

                                Text(
                                    text = "[ BACK ]",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = GitloftColors.TextSecondary,
                                    modifier = Modifier
                                        .clickable { isDirectGitHubVisible = false }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                        else -> {
                            // Main Options
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryCyberButton(
                                    title = "Continue with GitHub",
                                    icon = Icons.Default.Code,
                                    onClick = {
                                        val encodedRedirect = Uri.encode("gitloft://oauth-callback")
                                        val authUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/authorize?provider=github&redirect_to=$encodedRedirect&scopes=read:user%20user:email%20repo"
                                        launchOAuthCustomTab(context, authUrl)
                                        onDismiss()
                                    }
                                )

                                SecondaryCyberButton(
                                    title = "Continue with Google",
                                    icon = Icons.Default.AccountCircle,
                                    onClick = {
                                        val encodedRedirect = Uri.encode("gitloft://oauth-callback")
                                        val authUrl = "${BuildConfig.SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=$encodedRedirect"
                                        launchOAuthCustomTab(context, authUrl)
                                        onDismiss()
                                    }
                                )

                                SecondaryCyberButton(
                                    title = "Personal Access Token (PAT)",
                                    icon = Icons.Default.Terminal,
                                    onClick = { isDirectGitHubVisible = true }
                                )

                                SecondaryCyberButton(
                                    title = "Use Email Access",
                                    icon = Icons.Default.Email,
                                    onClick = { isEmailFormVisible = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun launchOAuthCustomTab(context: android.content.Context, url: String) {
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
