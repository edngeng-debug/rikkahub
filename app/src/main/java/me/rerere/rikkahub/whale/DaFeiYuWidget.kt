package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import java.io.ByteArrayInputStream

private const val POPUP_DP = 360

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    val hostView = LocalView.current
    DisposableEffect(hostView) {
        val context = hostView.context
        val activity = context as? Activity
        if (activity == null) {
            onDispose { }
        } else {
            val density = context.resources.displayMetrics.density
            val popupSize = (POPUP_DP * density).toInt()
            val decor = activity.window.decorView
            val webView = DaFeiYuWebView(context)
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.isVerticalScrollBarEnabled = false
            webView.isHorizontalScrollBarEnabled = false
            webView.addJavascriptInterface(MenuStateBridge(webView), "RikkaDaFeiYu")
            webView.webViewClient = DaFeiYuWebViewClient(context)

            val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
            val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
            webView.loadDataWithBaseURL(
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

            val popup = PopupWindow(webView, popupSize, popupSize, false).apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                isFocusable = false
                isOutsideTouchable = true
                isTouchable = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setTouchModal(false)
                    setIsLaidOutInScreen(true)
                    setIsClippedToScreen(true)
                }
                setAttachedInDecor(true)
                setSplitTouchEnabled(true)
                setTouchInterceptor { _, event ->
                    if (webView.menuOpen || webView.interactiveOpen) return@setTouchInterceptor false
                    val p = webView.hitRect
                    val inWidget = p.contains(event.x, event.y)
                    if (inWidget) return@setTouchInterceptor false
                    val copy = MotionEvent.obtain(event)
                    try {
                        copy.offsetLocation(event.rawX - event.x, event.rawY - event.y)
                        activity.dispatchTouchEvent(copy)
                    } finally {
                        copy.recycle()
                    }
                    true
                }
            }

            fun show() {
                val w = decor.width
                val h = decor.height
                if (w <= 0 || h <= 0) {
                    decor.post { show() }
                    return
                }
                val x = (w - popupSize).coerceAtLeast(0)
                val y = (h - popupSize).coerceAtLeast(0)
                if (!popup.isShowing) popup.showAtLocation(decor, Gravity.TOP or Gravity.START, x, y)
                else popup.update(x, y, popupSize, popupSize)
            }

            decor.post { show() }
            onDispose {
                try { popup.dismiss() } catch (_: Throwable) {}
                try { webView.stopLoading(); webView.destroy() } catch (_: Throwable) {}
            }
        }
    }
    Box(modifier = Modifier)
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context) {
    @Volatile var menuOpen = false
    @Volatile var interactiveOpen = false
    @Volatile var hitRect = RectF(0f, 0f, 0f, 0f)
}

private class MenuStateBridge(private val webView: DaFeiYuWebView) {
    @JavascriptInterface fun setMenuOpen(open: Boolean) { webView.menuOpen = open }
    @JavascriptInterface fun setInteractive(open: Boolean) { webView.interactiveOpen = open }
    @JavascriptInterface fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        val vw = viewportWidth.takeIf { it > 0f } ?: return
        val vh = viewportHeight.takeIf { it > 0f } ?: return
        val sx = webView.width.toFloat() / vw
        val sy = webView.height.toFloat() / vh
        webView.hitRect = RectF(left * sx, top * sy, right * sx, bottom * sy)
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

    private fun asset(path: String, mime: String, encoding: String?): WebResourceResponse = WebResourceResponse(mime, encoding, context.assets.open(path))
    private fun json(body: String) = WebResourceResponse("application/json", "UTF-8", ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)))
}
