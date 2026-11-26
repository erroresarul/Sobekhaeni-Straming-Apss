package com.sobekhanei.stream

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

private class StreamGuard(private val context: android.content.Context) {
    private val blockedHosts: Set<String> by lazy {
        context.assets.open("streamguard-hosts.txt").bufferedReader().useLines { lines ->
            lines.map { it.substringBefore("#").trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()
        }
    }

    fun isBlocked(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
        return blockedHosts.any { host == it || host.endsWith(".$it") }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val guard = StreamGuard(context)
            WebView(context).apply {
                settings.javaScriptEnabled = true // Required by the third-party embed player.
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(true)
                settings.allowFileAccess = false
                settings.allowContentAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        // Reject new windows/pop-under requests in this MVP.
                        return false
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString() ?: return null
                        if (!guard.isBlocked(requestUrl)) return null
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            204,
                            "Blocked by StreamGuard",
                            emptyMap(),
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return true
                        val scheme = uri.scheme?.lowercase()
                        if (scheme != "https" && scheme != "http") return true
                        if (guard.isBlocked(uri.toString())) return true

                        // Keep top-level navigation inside the selected embed provider.
                        if (request.isForMainFrame) {
                            val host = uri.host?.lowercase().orEmpty()
                            return host != "vidapi.xyz" && host != "www.vidapi.xyz" &&
                                host != "vidapi.site" && host != "www.vidapi.site"
                        }
                        // Let nested player frames load unless their host is on the blocklist.
                        return false
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.cancel()
                    }

                    override fun onSafeBrowsingHit(
                        view: WebView?,
                        request: WebResourceRequest?,
                        threatType: Int,
                        callback: SafeBrowsingResponse?
                    ) {
                        callback?.backToSafety(true)
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.tag != url) {
                webView.tag = url
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}
