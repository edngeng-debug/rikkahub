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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
                    addJavascriptInterface(MenuStateBridge(this), "RikkaDaFeiYu")
                    webViewClient = DaFeiYuWebViewClient(context)
                    val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                    val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
                    loadDataWithBaseURL(
                        "https://rikkahub.local/",
                        """
                        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"></head>
                        <body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%;min-height:100%"><div id="root"><textarea aria-hidden="true" style="position:absolute;left:-9999px"></textarea></div>
                        <script>$script</script><script>$debugScript</script></body></html>
                        """.trimIndent(),
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            },
        )
    }
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context) {
    @Volatile
    var menuOpen = false

    @Volatile
    var interactiveOpen = false

    private var touchInside = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val edge = 360f * resources.displayMetrics.density
            val inWidget = event.x >= width - edge && event.y >= height - edge
            touchInside = inWidget || menuOpen || interactiveOpen
            if (!touchInside) return false
        } else if (!touchInside && !menuOpen && !interactiveOpen) {
            return false
        }
        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touchInside = false
        }
        return handled
    }
}

private class MenuStateBridge(private val webView: DaFeiYuWebView) {
    @JavascriptInterface
    fun setMenuOpen(open: Boolean) {
        webView.menuOpen = open
    }

    @JavascriptInterface
    fun setInteractive(open: Boolean) {
        webView.interactiveOpen = open
    }
}

private class DaFeiYuWebViewClient(private val context: android.content.Context) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        return when (request.url.path) {
            "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png", null)
            "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif", null)
            "/dsh-whale/sound/press.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3", "audio/mpeg", null)
            "/dsh-whale/sound/release.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3", "audio/mpeg", null)
            "/dsh-whale/balance.json" -> json("{\"balance\":0,\"currency\":\"CNY\"}")
            "/dsh-whale/size.json" -> json("{\"scale\":1,\"sound\":true,\"vol\":1,\"soundSet\":\"duck\",\"usageMode\":\"ledger\",\"peakMode\":\"auto\",\"bubbleOn\":true,\"turnCostOn\":true,\"turnCostCloseMs\":5000}")
            "/dsh-whale/last-turn.json" -> json("{\"seq\":0,\"cost\":0}")
            else -> super.shouldInterceptRequest(view, request)
        }
    }

    private fun asset(path: String, mime: String, encoding: String?): WebResourceResponse =
        WebResourceResponse(mime, encoding, context.assets.open(path))

    private fun json(body: String) = WebResourceResponse(
        "application/json",
        "UTF-8",
        ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
    )
}
