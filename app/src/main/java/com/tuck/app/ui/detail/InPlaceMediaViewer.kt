package com.tuck.app.ui.detail

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.tuck.app.domain.model.SavedItem
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InPlaceMediaViewer(
    item: SavedItem,
    modifier: Modifier = Modifier,
    onCopyUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val rawUrl = item.originalUrl ?: return

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val embedData = remember(rawUrl, isDark) { resolveEmbedData(rawUrl, isDark) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Progress Bar while loading
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Live WebView In-Place Player & Post Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            val previewImage = item.thumbnailPath ?: item.localFilePath
            if (!previewImage.isNullOrBlank()) {
                val imageModel: Any = if (previewImage.startsWith("http://") || previewImage.startsWith("https://")) {
                    previewImage
                } else {
                    File(previewImage)
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                                if (newProgress >= 90) {
                                    isLoading = false
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // Keep navigation inside the in-place embed viewer
                                val targetUrl = request?.url?.toString() ?: ""
                                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                    return false
                                }
                                return false
                            }
                        }

                        when (embedData) {
                            is EmbedData.Html -> {
                                loadDataWithBaseURL(
                                    embedData.baseUrl,
                                    embedData.htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            is EmbedData.Url -> {
                                loadUrl(embedData.targetUrl)
                            }
                        }

                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Action Toolbar below the live embed
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonLabel = when {
                    rawUrl.contains("linkedin.com") -> "Open on LinkedIn"
                    rawUrl.contains("instagram.com") -> "Open in Instagram"
                    rawUrl.contains("reddit.com") -> "Open on Reddit"
                    rawUrl.contains("youtube.com") || rawUrl.contains("youtu.be") -> "Open on YouTube"
                    rawUrl.contains("tiktok.com") -> "Open on TikTok"
                    rawUrl.contains("twitter.com") || rawUrl.contains("x.com") -> "Open on X"
                    else -> "Open in Browser"
                }

                Button(
                    onClick = { openBrowserUrl(context, rawUrl) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.detail_reload_post),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onCopyUrl(rawUrl) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.detail_copy_link),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }
}

private sealed interface EmbedData {
    data class Html(val htmlContent: String, val baseUrl: String) : EmbedData
    data class Url(val targetUrl: String) : EmbedData
}

private fun resolveEmbedData(url: String, isDark: Boolean): EmbedData {
    val bgColor = if (isDark) "#121212" else "#ffffff"
    val textColor = if (isDark) "#ffffff" else "#000000"

    // 1. Instagram Reels, Posts, and Videos (Strict domain check)
    if (url.contains("instagram.com")) {
        val instagramMatch = Regex("/(?:reel|reels|p|tv|share/reel|share/p)/([^/?#]+)").find(url)
        if (instagramMatch != null) {
            val shortcode = instagramMatch.groupValues[1]
            val embedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            padding: 8px;
                            background-color: $bgColor;
                            display: flex;
                            justify-content: center;
                            align-items: flex-start;
                            min-height: 100vh;
                            overflow-x: hidden;
                        }
                        .instagram-media {
                            width: 100% !important;
                            max-width: 100% !important;
                            min-width: 100% !important;
                            border-radius: 14px !important;
                            margin: 0 auto !important;
                            box-shadow: none !important;
                        }
                        iframe {
                            width: 100% !important;
                            min-height: 560px !important;
                            border: none !important;
                            border-radius: 14px !important;
                        }
                    </style>
                </head>
                <body>
                    <iframe 
                        src="https://www.instagram.com/p/$shortcode/embed/captioned/" 
                        allowtransparency="true" 
                        allowfullscreen="true" 
                        frameborder="0" 
                        scrolling="no">
                    </iframe>
                    <script async src="https://www.instagram.com/embed.js"></script>
                </body>
                </html>
            """.trimIndent()
            return EmbedData.Html(embedHtml, "https://www.instagram.com")
        }
    }

    // 2. YouTube Videos & Shorts
    val youtubeId = when {
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
        url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&").substringBefore("#")
        else -> null
    }

    if (!youtubeId.isNullOrBlank()) {
        val youtubeHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        background-color: #000000;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                    }
                    iframe {
                        width: 100%;
                        height: 100vh;
                        border: none;
                    }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://www.youtube-nocookie.com/embed/$youtubeId?playsinline=1&modestbranding=1&rel=0" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                    allowfullscreen>
                </iframe>
            </body>
            </html>
        """.trimIndent()
        return EmbedData.Html(youtubeHtml, "https://www.youtube.com")
    }

    // 3. Reddit Posts, Threads & Discussions
    if (url.contains("reddit.com")) {
        return EmbedData.Url(url)
    }

    // 4. TikTok Videos
    if (url.contains("tiktok.com")) {
        return EmbedData.Url(url)
    }

    // 5. Default: Direct in-app live Web view
    return EmbedData.Url(url)
}
