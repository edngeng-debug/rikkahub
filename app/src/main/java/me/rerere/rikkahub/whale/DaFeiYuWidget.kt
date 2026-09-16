package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hostView = LocalView.current
    DisposableEffect(context, hostView) {
        val overlay = DaFeiYuOverlay(context, hostView)
        overlay.show()
        onDispose { overlay.dismiss() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class DaFeiYuOverlay(
    private val context: android.content.Context,
    private val hostView: View,
) {
    private val compactSize = (260f * context.resources.displayMetrics.density).roundToInt()
    private val webView = DaFeiYuWebView(context)
    private val popup = PopupWindow(webView, compactSize, compactSize, false)
    private var lastRootRect = floatArrayOf(Float.NaN, Float.NaN, Float.NaN, Float.NaN)
    private var lastViewport = floatArrayOf(Float.NaN, Float.NaN)
    private var popupX = 0
    private var popupY = 0
    private var interactive = false

    init {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webView.addJavascriptInterface(DaFeiYuBridge(this), "RikkaDaFeiYu")
        webView.webViewClient = DaFeiYuWebViewClient(context)

        val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
        val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
        val html = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
            </head><body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%">
            <div id="root"><textarea aria-hidden="true" tabindex="-1" style="position:absolute;left:-99999px;top:-99999px;width:1px;height:1px;opacity:0;pointer-events:none"></textarea></div>
            <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;window.__RIKKAHUB_DAFEIYU_SETTINGS=${DaFeiYuSettingsStore.json(context)};</script>
            <script>$script</script><script>$debugScript</script></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)

        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.isTouchable = true
        popup.isFocusable = false
        popup.isOutsideTouchable = false
        popup.isClippingEnabled = false
        popup.elevation = 0f
        popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
    }

    fun show() {
        hostView.post {
            if (popup.isShowing) return@post
            popupX = max(0, hostView.width - compactSize)
            popupY = max(0, hostView.height - compactSize)
            popup.showAtLocation(hostView, Gravity.TOP or Gravity.START, popupX, popupY)
        }
    }

    private fun cssScale(): Pair<Float, Float> {
        val vw = lastViewport[0]
        val vh = lastViewport[1]
        return if (vw > 1f && vh > 1f) {
            hostView.width.toFloat() / vw to hostView.height.toFloat() / vh
        } else {
            1f to 1f
        }
    }

    fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        hostView.post {
            lastRootRect = floatArrayOf(left, top, right, bottom)
            lastViewport = floatArrayOf(viewportWidth, viewportHeight)
            if (!popup.isShowing || interactive || viewportWidth <= 1f || viewportHeight <= 1f) return@post
            val (sx, sy) = cssScale()
            val rootWidth = ((right - left) * sx).coerceAtLeast(1f)
            val rootHeight = ((bottom - top) * sy).coerceAtLeast(1f)
            popupX = (left * sx + rootWidth - compactSize).roundToInt().coerceAtLeast(0)
            popupY = (top * sy + rootHeight - compactSize).roundToInt().coerceAtLeast(0)
            popup.update(popupX, popupY, compactSize, compactSize)
        }
    }

    private fun runOnUiAndWait(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
            return
        }
        val latch = CountDownLatch(1)
        hostView.post {
            try { action() } finally { latch.countDown() }
        }
        latch.await(800, TimeUnit.MILLISECONDS)
    }

    fun setInteractive(open: Boolean) {
        if (!popup.isShowing || open == interactive) return
        runOnUiAndWait {
            if (!popup.isShowing || open == interactive) return@runOnUiAndWait
            if (open) {
                val (sx, sy) = cssScale()
                val screenRootLeft = popupX + lastRootRect[0] * sx
                val screenRootTop = popupY + lastRootRect[1] * sy
                interactive = true
                // Expand before the original pointer-move handler is allowed to continue.
                popup.update(0, 0, -1, -1)
                val fullScaleX = hostView.width.toFloat() / lastViewport[0].coerceAtLeast(1f)
                val fullScaleY = hostView.height.toFloat() / lastViewport[1].coerceAtLeast(1f)
                val cssLeft = screenRootLeft / fullScaleX
                val cssTop = screenRootTop / fullScaleY
                webView.evaluateJavascript("window.__dshwPinRoot&&window.__dshwPinRoot($cssLeft,$cssTop)", null)
            } else {
                val (sx, sy) = cssScale()
                val rootLeftPx = lastRootRect[0] * sx
                val rootTopPx = lastRootRect[1] * sy
                val rootWidthPx = ((lastRootRect[2] - lastRootRect[0]) * sx).coerceAtLeast(1f)
                val rootHeightPx = ((lastRootRect[3] - lastRootRect[1]) * sy).coerceAtLeast(1f)
                popupX = (rootLeftPx + rootWidthPx - compactSize).roundToInt().coerceAtLeast(0)
                popupY = (rootTopPx + rootHeightPx - compactSize).roundToInt().coerceAtLeast(0)
                interactive = false
                popup.update(popupX, popupY, compactSize, compactSize)
                val localScaleX = hostView.width.toFloat() / lastViewport[0].coerceAtLeast(1f)
                val localScaleY = hostView.height.toFloat() / lastViewport[1].coerceAtLeast(1f)
                val localLeft = lastRootRect[0] - popupX / localScaleX
                val localTop = lastRootRect[1] - popupY / localScaleY
                webView.evaluateJavascript("window.__dshwPinRoot&&window.__dshwPinRoot($localLeft,$localTop)", null)
            }
        }
    }

    fun consumePanelAction(): String = DaFeiYuSettingsStore.consumePanel(context)

    fun saveUsageSettings(json: String) {
        DaFeiYuSettingsStore.saveUsagePatch(context, json)
    }

    fun dismiss() {
        webView.stopLoading()
        if (popup.isShowing) popup.dismiss()
        webView.destroy()
    }
}

private class DaFeiYuBridge(private val overlay: DaFeiYuOverlay) {
    @JavascriptInterface fun setInteractive(value: Boolean) = overlay.setInteractive(value)
    @JavascriptInterface fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) = overlay.setWidgetRect(left, top, right, bottom, viewportWidth, viewportHeight)
    @JavascriptInterface fun saveSizeConfig(json: String) = Unit
    @JavascriptInterface fun saveUsageSettings(json: String) = overlay.saveUsageSettings(json)
    @JavascriptInterface fun consumePanelAction(): String = overlay.consumePanelAction()
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context)

private class DaFeiYuWebViewClient(private val context: android.content.Context) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? = when (request.url.path) {
        "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png")
        "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif")
        "/dsh-whale/sound/press.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3", "audio/mpeg")
        "/dsh-whale/sound/release.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3", "audio/mpeg")
        "/dsh-whale/balance.json" -> json("{\"balance\":0,\"currency\":\"CNY\"}")
        "/dsh-whale/size.json" -> json(DaFeiYuSettingsStore.json(context))
        "/dsh-whale/usage-settings.json" -> json(DaFeiYuSettingsStore.usageJson(context))
        "/dsh-whale/last-turn.json" -> json("{\"seq\":0,\"cost\":0}")
        else -> super.shouldInterceptRequest(view, request)
    }
    private fun asset(path: String, mime: String) = WebResourceResponse(mime, null, context.assets.open(path))
    private fun json(body: String) = WebResourceResponse("application/json", "UTF-8", ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)))
}
