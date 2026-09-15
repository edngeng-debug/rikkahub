package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300L)
        ready = true
    }
    Box(modifier = modifier) {
        if (ready) {
            AndroidView(
                modifier = Modifier,
                factory = { context ->
                    DaFeiYuWebView(context).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mediaPlaybackRequiresUserGesture = false
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        addJavascriptInterface(DaFeiYuBridge(this, context), "RikkaDaFeiYu")
                        webViewClient = DaFeiYuWebViewClient(context)
                        val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                        val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
                        val html = """
                            <!doctype html><html><head>
                            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                            </head><body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%;min-height:100%">
                            <div id="root"><textarea aria-hidden="true" tabindex="-1" style="position:absolute;left:-99999px;top:-99999px;width:1px;height:1px;opacity:0"></textarea></div>
                            <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;window.__RIKKAHUB_DAFEIYU_SETTINGS=${DaFeiYuSettingsStore.json(context)};</script>
                            <script>$script</script><script>$debugScript</script></body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { webView -> webView.stopLoading(); webView.destroy() },
            )
        }
    }
}

private class DaFeiYuBridge(private val webView: DaFeiYuWebView, private val context: android.content.Context) {
    @JavascriptInterface fun setInteractive(value: Boolean) { webView.interactiveOpen = value }

    @JavascriptInterface fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        webView.setWidgetRect(left, top, right, bottom, viewportWidth, viewportHeight)
    }

    @JavascriptInterface fun saveSizeConfig(json: String) { DaFeiYuSettingsStore.updateFromJson(context, json) }
    @JavascriptInterface fun saveUsageSettings(json: String) { DaFeiYuSettingsStore.saveUsagePatch(context, json) }
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context) {
    @Volatile var interactiveOpen = false
    @Volatile private var rect = floatArrayOf(0f, 0f, 0f, 0f)
    @Volatile private var viewport = floatArrayOf(0f, 0f)
    private var touchInside = false

    fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        rect = floatArrayOf(left, top, right, bottom)
        viewport = floatArrayOf(viewportWidth, viewportHeight)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchInside = interactiveOpen || isInsideWidget(event.x, event.y)
                if (!touchInside) return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!touchInside) return false
            }
            else -> if (!touchInside) return false
        }
        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) touchInside = false
        return handled
    }

    private fun isInsideWidget(x: Float, y: Float): Boolean {
        val r = rect
        val v = viewport
        if (r[2] <= r[0] || r[3] <= r[1]) return false
        val sx = if (v[0] > 1f) width.toFloat() / v[0] else resources.displayMetrics.density
        val sy = if (v[1] > 1f) height.toFloat() / v[1] else resources.displayMetrics.density
        val l = r[0] * sx; val t = r[1] * sy; val rr = r[2] * sx; val b = r[3] * sy
        return x >= l && x <= rr && y >= t && y <= b
    }
}

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
