package com.example.gitloftandroid.ui.showcase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.gitloftandroid.data.model.GitHubContentItem
import com.example.gitloftandroid.data.model.GitHubRepo
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.LanguagePill
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.example.gitloftandroid.ui.viewmodel.RepoDetailViewModel

@Composable
fun RepoDetailScreen(
    repo: GitHubRepo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { RepoDetailViewModel(context.applicationContext as android.app.Application) }

    var selectedTab by remember { mutableStateOf(0) } // 0: README, 1: CODE, 2: PREVIEW
    val tabs = listOf("README", "CODE", "LIVE PREVIEW")

    val owner = repo.ownerLogin ?: repo.htmlUrl.split("/").getOrNull(3) ?: "github"

    LaunchedEffect(repo.name) {
        viewModel.loadReadme(owner, repo.name)
        viewModel.loadDirectory(owner, repo.name, "")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = GitloftColors.Volt,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = repo.name.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "★ ${repo.stargazersCount} · ${repo.language ?: "Code"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = GitloftColors.TextSecondary
                    )
                }
            }

            if (!repo.hideGitHubLink) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open GitHub",
                        tint = GitloftColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Custom Tab Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) GitloftColors.Volt else GitloftColors.Surface)
                        .border(
                            1.dp,
                            if (isSelected) GitloftColors.Volt else GitloftColors.Border,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected) "[$title]" else title,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.Black else GitloftColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> ReadmeTab(viewModel = viewModel, owner = owner, repo = repo)
                1 -> CodeExplorerTab(viewModel = viewModel, owner = owner, repo = repo)
                2 -> LivePreviewTab(repo = repo)
            }
        }
    }
}

// MARK: - README Tab
@Composable
fun ReadmeTab(
    viewModel: RepoDetailViewModel,
    owner: String,
    repo: GitHubRepo
) {
    val readmeContent by viewModel.readmeContent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isReadmeLoading.collectAsStateWithLifecycle()
    val error by viewModel.readmeError.collectAsStateWithLifecycle()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GitloftColors.Volt)
        }
    } else if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NO README AVAILABLE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GitloftColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                SecondaryCyberButton(
                    title = "Retry",
                    onClick = { viewModel.loadReadme(owner, repo.name) }
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            CyberBentoPanel(padding = 16.dp) {
                Text(
                    text = "README.MD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = GitloftColors.Volt,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = readmeContent ?: "No content",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}

// MARK: - Code Explorer Tab
@Composable
fun CodeExplorerTab(
    viewModel: RepoDetailViewModel,
    owner: String,
    repo: GitHubRepo
) {
    val codeItems by viewModel.codeItems.collectAsStateWithLifecycle()
    val isCodeLoading by viewModel.isCodeLoading.collectAsStateWithLifecycle()
    val codeError by viewModel.codeError.collectAsStateWithLifecycle()
    val pathStack by viewModel.pathStack.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val selectedFileContent by viewModel.selectedFileContent.collectAsStateWithLifecycle()
    val isFileLoading by viewModel.isFileLoading.collectAsStateWithLifecycle()
    val fileError by viewModel.fileError.collectAsStateWithLifecycle()

    val context = LocalContext.current

    if (selectedFile != null) {
        // File Viewer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // File Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { viewModel.closeFile() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GitloftColors.Volt, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = selectedFile!!.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                if (selectedFileContent != null) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("File Code", selectedFileContent))
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GitloftColors.Volt, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFileLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GitloftColors.Volt)
                }
            } else if (fileError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(fileError!!, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GitloftColors.Error)
                }
            } else if (selectedFile!!.isImage) {
                // Image viewer for PNG, JPG, GIF, WEBP, SVG, etc.
                val downloadUrl = selectedFile!!.downloadUrl ?: "https://raw.githubusercontent.com/$owner/${repo.name}/${repo.defaultBranch ?: "main"}/${selectedFile!!.path}"
                CyberBentoPanel(
                    padding = 16.dp,
                    backgroundColor = GitloftColors.Surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = downloadUrl,
                            contentDescription = selectedFile!!.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = GitloftColors.Volt, modifier = Modifier.size(32.dp))
                                }
                            },
                            error = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = GitloftColors.TextSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "UNABLE TO PREVIEW IMAGE",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = GitloftColors.Error
                                    )
                                    Text(
                                        text = "Could not fetch image stream from GitHub CDN.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = GitloftColors.TextSecondary
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GitloftColors.Surface, RoundedCornerShape(6.dp))
                                .border(1.dp, GitloftColors.Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FORMAT: ${selectedFile!!.name.substringAfterLast('.').uppercase()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = GitloftColors.Volt
                                )
                                selectedFile!!.size?.let { sizeBytes ->
                                    val sizeKb = sizeBytes / 1024.0
                                    val sizeText = if (sizeKb >= 1024) {
                                        String.format(java.util.Locale.US, "%.2f MB", sizeKb / 1024)
                                    } else {
                                        String.format(java.util.Locale.US, "%.1f KB", sizeKb)
                                    }
                                    Text(
                                        text = "SIZE: $sizeText",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = GitloftColors.TextSecondary
                                    )
                                }
                            }

                            SecondaryCyberButton(
                                title = "Open Full View",
                                onClick = {
                                    val targetUrl = selectedFile!!.htmlUrl ?: downloadUrl
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            } else {
                val lines = selectedFileContent?.lines() ?: emptyList()
                CyberBentoPanel(
                    padding = 12.dp,
                    backgroundColor = GitloftColors.Surface2,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lines.size) { i ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${i + 1}".padStart(4),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = GitloftColors.TextSecondary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = lines[i],
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Directory Browser
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Breadcrumbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pathStack.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.navigateBack(owner, repo.name) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GitloftColors.Volt, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                val pathString = if (pathStack.isEmpty()) repo.name else "${repo.name} / ${pathStack.joinToString(" / ")}"
                Text(
                    text = pathString,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GitloftColors.Volt
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isCodeLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GitloftColors.Volt)
                }
            } else if (codeItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files in this directory.", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GitloftColors.TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(codeItems, key = { it.path }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GitloftColors.Surface)
                                .border(1.dp, GitloftColors.Border, RoundedCornerShape(6.dp))
                                .clickable { viewModel.navigateInto(owner, repo.name, item) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (item.isDirectory) GitloftColors.Volt else GitloftColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.name,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (item.isDirectory) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.isDirectory) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GitloftColors.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Live Preview Tab (In-App WebView)
@Composable
fun LivePreviewTab(repo: GitHubRepo) {
    val context = LocalContext.current
    val customUrl = repo.customLink?.url

    if (customUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Web,
                    contentDescription = null,
                    tint = GitloftColors.TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "NO LIVE DEMO CONFIGURED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GitloftColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Edit this repo from the dashboard to attach an App Store or web preview URL.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = GitloftColors.TextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(customUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
