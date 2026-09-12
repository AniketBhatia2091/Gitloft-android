package com.example.gitloftandroid.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gitloftandroid.ui.components.CyberBentoPanel
import com.example.gitloftandroid.ui.components.PrimaryCyberButton
import com.example.gitloftandroid.ui.components.ScreenHeroHeader
import com.example.gitloftandroid.ui.components.SecondaryCyberButton
import com.example.gitloftandroid.ui.theme.GitloftColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun ShareScreen(
    username: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fullUrl = "https://gitloft.vercel.app/u/$username"
    val displayUrl = "gitloft.app/u/$username"

    val qrBitmap = remember(fullUrl) {
        generateQRCodeBitmap(fullUrl, 512)
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
            title = "SHARE SHOWCASE",
            subtitle = "Polished portal — shareable anywhere.",
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        // QR Box
        CyberBentoPanel(
            padding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(2.dp, GitloftColors.Border, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = displayUrl,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = GitloftColors.Volt,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Scan to open showcase on mobile or web.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = GitloftColors.TextSecondary
                )
            }
        }

        // Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryCyberButton(
                title = "Copy Showcase Link",
                icon = Icons.Default.ContentCopy,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Gitloft Showcase", fullUrl))
                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )

            SecondaryCyberButton(
                title = "Share via Android",
                icon = Icons.Default.Share,
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out my developer showcase on Gitloft: $fullUrl")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Showcase"))
                }
            )

            SecondaryCyberButton(
                title = "Open Web Showcase",
                icon = Icons.Default.OpenInBrowser,
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                    context.startActivity(browserIntent)
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun generateQRCodeBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
