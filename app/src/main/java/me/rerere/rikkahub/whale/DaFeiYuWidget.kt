package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
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
import kotlin.math.max

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
                    isFocusable = false
                    isFocusableInTouchMode = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    overScrollMode = View.OVER_SCROLL_NEVER

                    val bridge = MenuStateBridge(this)
                    addJavascriptInterface(bridge, "RikkaDaFeiYu")
                    webViewClient = DaFeiYuWebViewClient(context)

                    val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                    val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
                    val html = """
                        <!doctype html>
                        <html>
                        <head>
                          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                        </head>
                        <body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%;min-height:100%">
                          <div id="root"></div>
                          <script>$script</script>
                          <script>$debugScript</script>
                        </body>
                        </html>
                    """.trimIndent()

                    // Keep WebView construction off the first Compose frame. The view stays
                    // in the normal Compose hierarchy, so it cannot create a separate full-screen
                    // window that steals RikkaHub input or covers the IME.
                    postDelayed({
                        if (!isAttachedToWindow) return@postDelayed
                        loadDataWithBaseURL(
                            "https://rikkahub.local/",
                            html,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                    }, 80L)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
        )
    }
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context) {
    @Volatile
    var widgetRect = RectF(0f, 0f, 1f, 1f)

    @Volatile
    var interactiveOpen = false

    private var touchInside = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val r = widgetRect
            touchInside = interactiveOpen || r.contains(event.x, event.y)
            if (!touchInside) return false
        } else if (!touchInside && !interactiveOpen) {
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
        webView.interactiveOpen = open
    }

    @JavascriptInterface
    fun setInteractive(open: Boolean) {
        webView.interactiveOpen = open
    }

    @JavascriptInterface
    fun setWidgetRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        val width = max(1, webView.width).toFloat()
        val height = max(1, webView.height).toFloat()
        val sx = width / viewportWidth
        val sy = height / viewportHeight
        val next = RectF(left * sx, top * sy, right * sx, bottom * sy)
        if (next.right > next.left && next.bottom > next.top) {
            webView.widgetRect = next
        }
    }
}

private class DaFeiYuWebViewClient(private val context: android.content.Context) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        return when (request.url.path) {
            "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png", null)
            "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif", null)
            "/dsh-whale/sound/press.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3",
                "audio/mpeg",
                null,
            )
            "/dsh-whale/sound/release.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3",
                "audio/mpeg",
                null,
            )
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
