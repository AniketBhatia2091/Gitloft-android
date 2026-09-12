package com.example.gitloftandroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitloftandroid.ui.theme.GitloftColors

/**
 * Stark Cyber Tech Grid Background with subtle geometric overlay lines
 */
@Composable
fun TechGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GitloftColors.Background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 36.dp.toPx()
            val lineColor = GitloftColors.Border.copy(alpha = 0.25f)
            
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }
        content()
    }
}

/**
 * CyberBentoPanel: carbon charcoal surface with crisp border
 */
@Composable
fun CyberBentoPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    padding: Dp = 16.dp,
    borderColor: Color = GitloftColors.Border,
    backgroundColor: Color = GitloftColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(padding),
        content = content
    )
}

/**
 * PrimaryCyberButton: High-visibility Volt green CTA button
 */
@Composable
fun PrimaryCyberButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) GitloftColors.Volt else GitloftColors.Volt.copy(alpha = 0.4f))
            .border(1.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Black,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title.uppercase(),
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * SecondaryCyberButton: Carbon/steel surface with crisp border
 */
@Composable
fun SecondaryCyberButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = GitloftColors.Border,
    textColor: Color = GitloftColors.Text,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GitloftColors.Surface2)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title.uppercase(),
                color = textColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * ScreenHeroHeader: Brutalist title and subtitle with right-side action slot
 */
@Composable
fun ScreenHeroHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.5.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = GitloftColors.TextSecondary
                )
            }
        }
        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

/**
 * Section title with right detail tag
 */
@Composable
fun CyberSectionTitle(
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = GitloftColors.TextSecondary
        )
        if (detail != null) {
            Text(
                text = "[ ${detail.uppercase()} ]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = GitloftColors.Volt
            )
        }
    }
}

/**
 * Docked Cyber Tab Bar
 */
data class CyberTabItem(
    val icon: ImageVector,
    val label: String
)

@Composable
fun CyberBottomTabBar(
    tabs: List<CyberTabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GitloftColors.Surface)
            .border(1.5.dp, GitloftColors.Border, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = selectedTabIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isActive) GitloftColors.Volt else GitloftColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isActive) "[${tab.label}]" else tab.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = if (isActive) GitloftColors.Volt else GitloftColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Live status pill (e.g. ONLINE)
 */
@Composable
fun LiveStatusPill(
    status: String = "ONLINE",
    isLive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GitloftColors.Surface)
            .border(1.dp, GitloftColors.Border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isLive) GitloftColors.Volt else GitloftColors.TextSecondary)
        )
        Text(
            text = status.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = if (isLive) GitloftColors.Volt else GitloftColors.TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Language Pill with color indicator dot
 */
@Composable
fun LanguagePill(
    language: String,
    modifier: Modifier = Modifier
) {
    val langColor = GitloftColors.colorForLanguage(language)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GitloftColors.Surface2)
            .border(1.dp, GitloftColors.Border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(langColor)
        )
        Text(
            text = language.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Color.White
        )
    }
}
