package com.tuck.app.processing.capture

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tuck.app.data.remote.AdHosts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Renders a page in an offscreen WebView and returns the resulting HTML.
 *
 * This is Tier 2 of CAPTURE_ARCHITECTURE.md. It exists because some platforms no longer serve
 * their content to anything but a browser — Instagram's embed page returns 611 KB of JavaScript
 * and nothing else — so the only way to reach that content is to be a browser. The extractors
 * are unchanged: they receive HTML and parse it, and neither knows nor cares that this HTML came
 * from a rendered DOM rather than a socket.
 *
 * Cookies are shared with the in-place viewer's WebView, so a platform the reader has signed
 * into once is captured as them from then on. That is the point of the tier, and also why the
 * hardening below is not optional.
 *
 * Costs, which is why callers should try Tier 0/1 first: 50-100 MB of memory while a capture is
 * in flight, a few seconds per page, and real battery. Captures are serialised by [mutex] — two
 * WebViews rendering at once on a mid-range phone is how a background worker gets killed.
 */
@Singleton
class CaptureEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val LOAD_TIMEOUT_MS = 20_000L
        const val SETTLE_POLL_MS = 200L
        /** Grace period after load for late XHR content, when no selector is declared. */
        const val SETTLE_GRACE_MS = 1_200L
        const val MAX_HTML_CHARS = 4_000_000
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }

    private val mutex = Mutex()

    /**
     * Loads [url] and returns the rendered HTML, or null if it could not be captured in time.
     *
     * [readySelector] is a CSS selector that means "the content has arrived". When given, the
     * capture returns as soon as it matches, which is usually far sooner than the page is
     * finished loading. Without one the engine waits for load plus a short grace period, which
     * works but is slower and less certain.
     *
     * Never throws: a failed capture must degrade to whatever Tier 0/1 produced, never fail the
     * save (Product Law 2).
     */
    suspend fun capture(url: String, readySelector: String? = null): String? = mutex.withLock {
        try {
            withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                withContext(Dispatchers.Main) { render(url, readySelector) }
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun render(url: String, readySelector: String?): String? {
        // WebView must be constructed on a thread with a Looper, which is why this whole
        // function runs on Dispatchers.Main.
        val webView = WebView(context)
        try {
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false // Nothing here is ever displayed.
                blockNetworkImage = true
                mediaPlaybackRequiresUserGesture = true
                userAgentString = USER_AGENT
                // A capture WebView holds the reader's session cookies and renders pages Tuck
                // did not write. It has no business reaching the filesystem.
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
            }

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }

            val loaded = suspendCancellableCoroutine { continuation ->
                webView.webViewClient = object : WebViewClient() {
                    private var settled = false

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (settled) return
                        settled = true
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        // Only the main document failing is fatal; a blocked subresource is not.
                        if (request?.isForMainFrame != true || settled) return
                        settled = true
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? =
                        if (AdHosts.blocks(request?.url?.host)) {
                            WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                        } else {
                            null
                        }
                }

                continuation.invokeOnCancellation { webView.stopLoading() }
                webView.loadUrl(url)
            }

            if (!loaded) return null

            awaitContent(webView, readySelector)
            return outerHtml(webView)
        } finally {
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
    }

    /**
     * Waits for the page to actually contain something worth parsing.
     *
     * `onPageFinished` fires when the document has loaded, which on a single-page app is before
     * the content exists — the whole reason Tier 1 fails on these pages in the first place.
     */
    private suspend fun awaitContent(webView: WebView, readySelector: String?) {
        if (readySelector == null) {
            delay(SETTLE_GRACE_MS)
            return
        }
        val script = "(function(){return !!document.querySelector(${quote(readySelector)});})();"
        // Bounded by the caller's withTimeoutOrNull, so this loop cannot run away.
        while (true) {
            if (evaluate(webView, script) == "true") return
            delay(SETTLE_POLL_MS)
        }
    }

    private suspend fun outerHtml(webView: WebView): String? {
        val raw = evaluate(webView, "document.documentElement.outerHTML") ?: return null
        // evaluateJavascript hands back a JSON-encoded value, so the HTML arrives as a quoted,
        // escaped string rather than as markup.
        val decoded = try {
            JSONTokener(raw).nextValue() as? String
        } catch (e: Exception) {
            null
        } ?: return null
        return decoded.take(MAX_HTML_CHARS).takeIf { it.isNotBlank() }
    }

    private suspend fun evaluate(webView: WebView, script: String): String? =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { value ->
                if (continuation.isActive) continuation.resume(value)
            }
        }

    /** JSON-quotes a string for safe interpolation into a script. */
    private fun quote(value: String): String =
        org.json.JSONObject.quote(value)
}
