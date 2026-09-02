package com.tuck.app.ui.detail

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.view.MotionEvent
import com.tuck.app.data.remote.AdHosts
import com.tuck.app.processing.extractors.GoogleMapsSourceExtractor
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
import androidx.compose.ui.unit.Dp
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
    /** Hidden when the caller draws its own controls over the media, as the detail header does. */
    showToolbar: Boolean = true,
    /** Overrides the per-platform height when the caller is sizing the media area itself. */
    heightOverride: Dp? = null,
    /**
     * Whether the embed may scroll internally. False makes the media a single fixed pane, so the
     * page it sits in scrolls as one surface from anywhere — including over the media.
     */
    scrollable: Boolean = true,
    /**
     * Reports the height the embed actually rendered at, in dp. Platforms publish media at
     * whatever aspect the poster was shot in, so a fixed pane either crops it or leaves a black
     * band; measuring the page is the only way to fit it.
     */
    onContentHeight: ((Dp) -> Unit)? = null,
    onCopyUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val rawUrl = item.originalUrl ?: return
    // Short Maps links encode nothing; the canonical URL resolved at save time does.
    val canonicalUrl = item.canonicalUrl

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val embedData = remember(rawUrl, canonicalUrl, isDark) { resolveEmbedData(rawUrl, canonicalUrl, isDark) }

    Column(
        modifier = modifier
            .fillMaxWidth()
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

        // A fixed height letterboxes a 9:16 reel and crops a 16:9 video at the same time, so
        // size the container to the shape the platform actually serves.
        val isVerticalMedia = listOf("instagram.com", "instagr.am", "ig.me", "tiktok.com", "/shorts/")
            .any { rawUrl.contains(it, ignoreCase = true) }
        val isYouTube = listOf("youtube.com", "youtu.be").any { rawUrl.contains(it, ignoreCase = true) }
        val containerHeight = heightOverride ?: when {
            // Player plus its comment thread, which the reader scrolls inside the view.
            isYouTube -> 560.dp
            isVerticalMedia -> 520.dp
            else -> 480.dp
        }

        // Live WebView In-Place Player & Post Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
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
                            userAgentString = (embedData as? EmbedData.Url)?.userAgent
                                ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                        }

                        isNestedScrollingEnabled = scrollable
                        isVerticalScrollBarEnabled = scrollable
                        setOnTouchListener { v, event ->
                            if (!scrollable) {
                                // Swallow drags but not taps, so the page scrolls through the
                                // media while play and mute still work.
                                return@setOnTouchListener event.actionMasked == MotionEvent.ACTION_MOVE
                            }
                            // The parent Column scrolls vertically, so without this the WebView
                            // never receives a drag and its own content cannot be scrolled.
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                            false
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
                            private val cleanupCss = (embedData as? EmbedData.Url)?.cleanupCss

                            // Injecting only at onPageFinished is too late: the section has
                            // already been fetched, laid out and painted, so the reader watches
                            // it load and then vanish. The stylesheet has to be in the document
                            // before those elements exist, and the script is idempotent so
                            // running it at each stage costs nothing.
                            private fun applyCleanup(view: WebView?) {
                                val css = cleanupCss ?: return
                                view?.evaluateJavascript(injectStyleScript(css), null)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                applyCleanup(view)
                            }

                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                applyCleanup(view)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val host = request?.url?.host ?: return null
                                return if (AdHosts.blocks(host)) emptyResponse() else null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                applyCleanup(view)
                                measureContent(view)
                            }

                            /**
                             * Reads the rendered height a few times rather than once: media loads
                             * after the document does, so the first measurement is usually of an
                             * empty box.
                             */
                            private fun measureContent(view: WebView?) {
                                val report = onContentHeight ?: return
                                val target = view ?: return
                                listOf(150L, 600L, 1500L, 3000L).forEach { delayMs ->
                                    target.postDelayed({
                                        target.evaluateJavascript(MEASURE_JS) { value ->
                                            val px = value?.trim('"')?.toFloatOrNull() ?: return@evaluateJavascript
                                            if (px > 40f) report(px.dp)
                                        }
                                    }, delayMs)
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString() ?: return false

                                // Instagram's logged-out embed carries the cover frame and a link
                                // out — never the video itself — so its Play button is a
                                // navigation, not a player. Following it in place lands on a
                                // login wall; handing it to the installed app lands on the reel.
                                val leavingInstagramEmbed = INSTAGRAM_HOSTS.any {
                                    targetUrl.contains(it, ignoreCase = true)
                                } && !targetUrl.contains("/embed", ignoreCase = true)

                                if (leavingInstagramEmbed) {
                                    openPlatformUrl(ctx, targetUrl)
                                    return true
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
                                if (embedData.headers.isEmpty()) {
                                    loadUrl(embedData.targetUrl)
                                } else {
                                    loadUrl(embedData.targetUrl, embedData.headers)
                                }
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

        if (showToolbar) {
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
                        rawUrl.contains("linkedin.com") -> stringResource(R.string.action_open_on, "LinkedIn")
                        rawUrl.contains("instagram.com") -> stringResource(R.string.action_open_on, "Instagram")
                        rawUrl.contains("reddit.com") -> stringResource(R.string.action_open_on, "Reddit")
                        rawUrl.contains("youtube.com") || rawUrl.contains("youtu.be") -> stringResource(R.string.action_open_on, "YouTube")
                        rawUrl.contains("tiktok.com") -> stringResource(R.string.action_open_on, "TikTok")
                        rawUrl.contains("twitter.com") || rawUrl.contains("x.com") -> stringResource(R.string.action_open_on, "X")
                        else -> stringResource(R.string.action_open_in_browser)
                    }

                    Button(
                        onClick = { openPlatformUrl(context, rawUrl) },
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

    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }
}

/**
 * Opens a post in its own app when that app is installed, since the native view is signed in and
 * the browser one is not. Falls back to the browser whenever the app is absent.
 */
private val INSTAGRAM_HOSTS = listOf("instagram.com", "instagr.am", "ig.me")

private fun openPlatformUrl(context: android.content.Context, rawUrl: String) {
    val isInstagram = INSTAGRAM_HOSTS.any { rawUrl.contains(it, ignoreCase = true) }
    val shortcode = Regex("/(?:reel|reels|p|tv|share/reel|share/p)/([^/?#]+)")
        .find(rawUrl)?.groupValues?.get(1)

    if (isInstagram && !shortcode.isNullOrBlank()) {
        val appIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.instagram.com/p/$shortcode/")
        ).apply { setPackage("com.instagram.android") }
        try {
            context.startActivity(appIntent)
            return
        } catch (e: Exception) {
            // Instagram is not installed; the browser is the fallback.
        }
    }
    openBrowserUrl(context, rawUrl)
}

/** A new stream per call: a single shared one would be consumed by the first request. */
private fun emptyResponse(): WebResourceResponse =
    WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))

/**
 * Reduces Instagram's embed to the media alone.
 *
 * The embed ships six sibling blocks — Header, the media, HoverCard, Feedback, Caption, Footer —
 * and the caption block alone measured 1636px against a 1820px page, which is what made the
 * preview a long scroll of its own. Tuck renders the author and caption itself underneath, so
 * everything except the media is duplicated chrome.
 */
private const val INSTAGRAM_CLEANUP_CSS = """
    .Header, .HoverCard, .Feedback, .Caption, .Footer,
    .CaptionComments, .CaptionCommentsExpand,
    .WatchOnInstagramContainer { display: none !important; }
    html, body { margin: 0 !important; padding: 0 !important; background: #000 !important;
                 overflow: hidden !important; }
    .EmbedFrame, .Content, .EmbeddedMedia { margin: 0 !important; border: 0 !important;
                 box-shadow: none !important; background: #000 !important; }
    .EmbeddedMediaImage { width: 100% !important; height: auto !important; }
"""

/**
 * Height of the tallest thing the page actually drew, in CSS pixels — which map 1:1 to dp,
 * because the WebView renders at the device's own density.
 */
/**
 * Leaves the player and the comment thread; hides the recommendation surfaces around them.
 *
 * An earlier, stricter version of this hid the player itself and left a black pane, so the rule
 * here is to name only what is definitely a rail or a promo. A stale selector then means a strip
 * reappears — never a video that will not play.
 */
private const val YOUTUBE_CLEANUP_CSS = """
    ytm-video-with-context-renderer,
    ytm-compact-video-renderer,
    ytm-compact-playlist-renderer,
    ytm-rich-shelf-renderer,
    ytm-reel-shelf-renderer,
    ytm-shorts-lockup-view-model,
    ytm-companion-slot,
    ytm-promoted-video-renderer,
    ytm-mealbar-promo-renderer,
    ytm-app-promo-renderer,
    ytm-pivot-bar-renderer,
    .mobile-topbar-header,
    .related-chips-slot-wrapper,
    ytm-continuation-item-renderer,
    ytm-ghost-card-renderer,
    .continuation-spinner,
    tp-yt-paper-spinner,
    ytm-spinner { display: none !important; }
    body { padding-bottom: 0 !important; }
"""

private val MEASURE_JS = """
    (function () {
      var best = 0;
      var nodes = document.querySelectorAll('img, video, iframe, .EmbeddedMedia, .EmbedFrame');
      for (var i = 0; i < nodes.length; i++) {
        var h = nodes[i].getBoundingClientRect().height;
        if (h > best) best = h;
      }
      if (!best && document.body) best = document.body.scrollHeight;
      return String(Math.round(best));
    })();
""".trimIndent()

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

/**
 * Reddit chrome, belt and braces.
 *
 * The desktop user agent is what actually removes the "open in the app" interstitial — it is
 * served to phone browsers and to nothing else. These rules are the second line of defence, and
 * they deliberately cover both the old markup and the current element names, because the two
 * sets cost nothing to keep and neither could be verified against a live page from here.
 */
private const val REDDIT_CLEANUP_CSS = """
    /* Current front end */
    shreddit-experience-tree,
    shreddit-async-loader[bundlename*="xpromo"],
    shreddit-async-loader[bundlename*="app_selector"],
    xpromo-app-selector,
    [slot="mobile-app-banner"],
    .xpromo-banner, #xpromo-banner,
    shreddit-app-banner,
    /* Old markup */
    #header, .side, #footer, .footer-parent,
    .infobar, .listingsignupbar, .premium-banner-outer,
    .promotedlink, .promoted, .sponsored-link,
    #eu-cookie-policy, .cookie-infobar,
    .debuginfo, .bottommenu { display: none !important; }
    html, body { overflow: auto !important; position: static !important; }
"""

/**
 * The official embed is player-only, so there is no surrounding chrome left to hide. What
 * remains is making it fill the view rather than sitting in its own letterbox.
 */
private const val TIKTOK_CLEANUP_CSS = """
    html, body { margin: 0 !important; padding: 0 !important; background: #000 !important; }
"""

/**
 * Builds a script that adds [css] to the page.
 *
 * The stylesheet is escaped into a JavaScript string literal rather than interpolated raw: a
 * stray quote or the newlines it certainly contains would otherwise end the literal early and
 * the whole injection would fail silently, which is exactly the kind of bug that looks like a
 * wrong selector.
 */
private fun injectStyleScript(css: String): String {
    val literal = css
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    return """
        (function () {
          var id = 'tuck-cleanup';
          if (document.getElementById(id)) return;
          var style = document.createElement('style');
          style.id = id;
          style.textContent = "$literal";
          (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()
}

private sealed interface EmbedData {
    data class Html(val htmlContent: String, val baseUrl: String) : EmbedData
    /**
     * [cleanupCss] is injected once the page settles, to strip chrome the reader did not ask
     * for. [userAgent] overrides the default when a site behaves better as another client.
     */
    data class Url(
        val targetUrl: String,
        val cleanupCss: String? = null,
        val userAgent: String? = null,
        /** Extra request headers, for embeds that refuse a request without them. */
        val headers: Map<String, String> = emptyMap()
    ) : EmbedData
}

private fun resolveEmbedData(url: String, canonicalUrl: String?, isDark: Boolean): EmbedData {
    val bgColor = if (isDark) "#121212" else "#ffffff"
    val textColor = if (isDark) "#ffffff" else "#000000"

    // 1. Instagram Reels, Posts, and Videos (Strict domain check)
    if (INSTAGRAM_HOSTS.any { url.contains(it, ignoreCase = true) }) {
        val instagramMatch = Regex("/(?:reel|reels|p|tv|share/reel|share/p)/([^/?#]+)").find(url)
        if (instagramMatch != null) {
            val shortcode = instagramMatch.groupValues[1]
            // Loaded directly rather than wrapped in an iframe: same-origin means the stylesheet
            // below can reach it. Inside an iframe it could not, which is why the embed used to
            // arrive with its own header, caption and footer attached.
            return EmbedData.Url(
                "https://www.instagram.com/p/$shortcode/embed/captioned/",
                INSTAGRAM_CLEANUP_CSS
            )
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
        // The player embed and nothing else. The watch page was tried and brought its comment
        // thread and recommendation rails with it, which is a page rather than a preview.
        //
        // Loaded as a real URL rather than an iframe inside a synthesised document: the embed
        // validates its embedding origin, and a `loadDataWithBaseURL` page has none to present —
        // that was the error overlay this used to open on.
        // The /embed/ iframe answers "error 152 - 4, video unavailable" inside a WebView, on
        // both hosts, with and without a Referer and an origin. The watch page plays reliably,
        // so that is what is used, with the rails stylesheet applied over it.
        return EmbedData.Url("https://m.youtube.com/watch?v=$youtubeId", YOUTUBE_CLEANUP_CSS)
    }

    // 3. Reddit Posts, Threads & Discussions
    if (url.contains("reddit.com")) {
        // Measured 2026-09-02: old.reddit.com now 302s every logged-out request to
        // /login/?reason=lor2, both desktop and mobile user agents, so routing the viewer there
        // replaced the post with a login page. www.reddit.com still answers 200.
        //
        // The "get the app" interstitial is a mobile-only behaviour, so the durable fix is not a
        // stylesheet chasing renamed elements — it is to stop presenting as a phone browser.
        val www = Regex("^https?://(?:[\\w-]+\\.)?reddit\\.com", RegexOption.IGNORE_CASE)
            .replace(url, "https://www.reddit.com")
        return EmbedData.Url(www, REDDIT_CLEANUP_CSS, DESKTOP_USER_AGENT)
    }

    // 4. TikTok Videos
    if (url.contains("tiktok.com")) {
        val tiktokId = Regex("/video/(\\d+)").find(url)?.groupValues?.get(1)
        // TikTok's class names are build-hashed, so a stylesheet aimed at the full page would
        // break on their next deploy. The official embed has no chrome to strip in the first
        // place, which is the more durable answer to the same problem.
        return if (tiktokId != null) {
            EmbedData.Url("https://www.tiktok.com/embed/v2/$tiktokId", TIKTOK_CLEANUP_CSS)
        } else {
            EmbedData.Url(url)
        }
    }

    // 5. Google Maps locations
    if (GoogleMapsSourceExtractor.canHandleUrl(url)) {
        // A Maps place page renders as the full Maps app, which in a WebView means a consent
        // prompt and an "open in the app" nag over a map the reader cannot use. The keyless
        // embed is just the map.
        val place = GoogleMapsSourceExtractor.parse(canonicalUrl ?: url)
        GoogleMapsSourceExtractor.embedUrl(place)?.let { return EmbedData.Url(it) }
    }

    // 6. Default: Direct in-app live Web view
    return EmbedData.Url(url)
}
