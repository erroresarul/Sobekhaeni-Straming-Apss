package com.sobekhanei.stream

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.util.Locale

private class StreamGuard(private val context: Context) {
    private val allowedMainFrameHosts = setOf(
        "vidapi.xyz",
        "vidapi.site",
        "vaplayer.ru"
    )

    private val builtInBlockedHosts = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "googletagservices.com",
        "adnxs.com",
        "adsrvr.org",
        "advertising.com",
        "adform.net",
        "adfox.ru",
        "adition.com",
        "adkernel.com",
        "ad-maven.com",
        "admaven.com",
        "adsterra.com",
        "adtng.com",
        "adnium.com",
        "amazon-adsystem.com",
        "appnexus.com",
        "bidswitch.net",
        "casalemedia.com",
        "chartboost.com",
        "clarity.ms",
        "criteo.com",
        "exoclick.com",
        "exosrv.com",
        "google-analytics.com",
        "googletagmanager.com",
        "hilltopads.net",
        "hotjar.com",
        "inmobi.com",
        "lijit.com",
        "mgid.com",
        "mixpanel.com",
        "monetag.com",
        "moatads.com",
        "openx.net",
        "outbrain.com",
        "popads.net",
        "popcash.net",
        "popunder.net",
        "propellerads.com",
        "pubmatic.com",
        "quantserve.com",
        "revcontent.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "segment.io",
        "sharethrough.com",
        "smartadserver.com",
        "taboola.com",
        "trafficjunky.com",
        "trafficjunky.net",
        "tsyndicate.com",
        "unityads.unity3d.com",
        "vungle.com",
        "zedo.com",
        "juicyads.com",
        "onclickads.net",
        "onclickpredictiv.com"
    )

    private val blockedHostFragments = listOf(
        "doubleclick",
        "googlesyndication",
        "googleadservices",
        "adservice",
        "adserver",
        "popads",
        "popcash",
        "popunder",
        "clickunder",
        "exoclick",
        "exosrv",
        "propellerads",
        "hilltopads",
        "juicyads",
        "trafficjunky",
        "onclickads",
        "adsterra",
        "adtng",
        "ad-maven",
        "admaven"
    )

    private val blockedUrlMarkers = listOf(
        "/popunder",
        "/clickunder",
        "/adserver",
        "/adsystem",
        "/adtag",
        "/prebid",
        "/vast",
        "/banner-ad",
        "/push-notification",
        "/track/",
        "/tracking/",
        "/pixel"
    )

    private val blockedHosts: Set<String> by lazy {
        val fromAsset = runCatching {
            context.assets.open("streamguard-hosts.txt").bufferedReader().useLines { lines ->
                lines.map { it.substringBefore("#").trim().lowercase(Locale.US) }
                    .filter { it.isNotBlank() && !it.startsWith("!") }
                    .toSet()
            }
        }.getOrDefault(emptySet())
        fromAsset + builtInBlockedHosts
    }

    fun isBlocked(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(Locale.US) ?: return false

        if (blockedHosts.any { host == it || host.endsWith(".$it") }) return true
        if (blockedHostFragments.any { fragment -> host.contains(fragment) }) return true

        val pathAndQuery = (
            uri.encodedPath.orEmpty() + "?" + uri.encodedQuery.orEmpty()
        ).lowercase(Locale.US)

        return blockedUrlMarkers.any { marker -> pathAndQuery.contains(marker) }
    }

    fun isAllowedMainFrame(hostValue: String?): Boolean {
        val host = hostValue?.lowercase(Locale.US) ?: return false
        return allowedMainFrameHosts.any { host == it || host.endsWith(".$it") }
    }
}

private class StreamGuardChromeClient(context: Context) : WebChromeClient() {
    private val appContext = context
    private var customView: View? = null
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var previousOrientation: Int? = null

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        // Reject target=_blank, JavaScript window.open and pop-under windows.
        return false
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // The embedded video player does not need camera or microphone access.
        request?.deny()
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.cancel()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.cancel()
        return true
    }

    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        val activity = appContext.findActivity()
        val decor = activity?.window?.decorView as? FrameLayout
        if (view == null || activity == null || decor == null || customView != null) {
            callback?.onCustomViewHidden()
            return
        }

        previousOrientation = activity.requestedOrientation
        customView = view
        customViewCallback = callback

        val container = FrameLayout(activity).apply {
            setBackgroundColor(AndroidColor.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        customViewContainer = container
        decor.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onHideCustomView() {
        hideCustomView()
    }

    fun hideCustomView() {
        val activity = appContext.findActivity()
        customViewContainer?.let { container ->
            (container.parent as? ViewGroup)?.removeView(container)
        }
        customViewContainer = null
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        previousOrientation?.let { orientation ->
            activity?.requestedOrientation = orientation
        }
        previousOrientation = null
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        if (current is Activity) return current
        if (current !is ContextWrapper) return null
        current = current.baseContext
    }
}

private const val POPUP_GUARD_SCRIPT = """
(function () {
  if (window.__sobekhaneiStreamGuardInstalled) return;
  window.__sobekhaneiStreamGuardInstalled = true;
  try {
    window.open = function () { return null; };
    document.addEventListener('click', function (event) {
      var target = event.target;
      var link = target && target.closest
        ? target.closest('a[target="_blank"], area[target="_blank"]')
        : null;
      if (link) {
        event.preventDefault();
        event.stopImmediatePropagation();
      }
    }, true);
    document.addEventListener('submit', function (event) {
      var form = event.target;
      if (form && String(form.target).toLowerCase() === '_blank') {
        event.preventDefault();
        event.stopImmediatePropagation();
      }
    }, true);
  } catch (e) {}
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamWebView(url: String, modifier: Modifier = Modifier, guardEnabled: Boolean = true) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val guard = StreamGuard(context)
            WebView(context).apply {
                tag = url
                setBackgroundColor(AndroidColor.BLACK)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(guardEnabled)
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webChromeClient = StreamGuardChromeClient(context)
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString() ?: return null
                        if (!guardEnabled || !guard.isBlocked(requestUrl)) return null

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
                        val scheme = uri.scheme?.lowercase(Locale.US)

                        if (scheme != "https" && scheme != "http") return true
                        if (guardEnabled && guard.isBlocked(uri.toString())) return true

                        // Keep top-level navigation in the selected embed provider.
                        // Nested player/CDN frames are allowed unless they match a block rule.
                        if (guardEnabled && request.isForMainFrame) {
                            return !guard.isAllowedMainFrame(uri.host)
                        }
                        return false
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        pageUrl: String?,
                        favicon: android.graphics.Bitmap?
                    ) {
                        val host = runCatching { Uri.parse(pageUrl.orEmpty()).host }.getOrNull()
                        if (guardEnabled && !guard.isAllowedMainFrame(host)) {
                            view?.stopLoading()
                        }
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        val host = runCatching { Uri.parse(pageUrl.orEmpty()).host }.getOrNull()
                        if (guardEnabled && guard.isAllowedMainFrame(host)) {
                            view?.evaluateJavascript(POPUP_GUARD_SCRIPT, null)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        // Never bypass certificate errors.
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

                // Downloads are intentionally disabled in this streaming-only app.
                setDownloadListener { _, _, _, _, _ -> }
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
            (webView.webChromeClient as? StreamGuardChromeClient)?.hideCustomView()
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}
