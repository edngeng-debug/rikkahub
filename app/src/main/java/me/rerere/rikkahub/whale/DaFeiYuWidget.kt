package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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
import kotlin.math.ceil
import kotlin.math.max

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

            val renderPopup = PopupWindow(webView, -1, -1, false).apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                isTouchable = false
                isFocusable = false
                isOutsideTouchable = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setIsLaidOutInScreen(true)
                    setIsClippedToScreen(false)
                }
                setAttachedInDecor(true)
            }

            val touchView = TouchRelayView(context, webView)
            val touchPopup = PopupWindow(touchView, 1, 1, false).apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                isTouchable = true
                isFocusable = false
                isOutsideTouchable = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setTouchModal(false)
                    setIsLaidOutInScreen(true)
                    setIsClippedToScreen(true)
                }
                setAttachedInDecor(true)
            }

            val relay = TouchRelayController(decor, touchPopup, touchView)
            touchView.controller = relay
            webView.addJavascriptInterface(MenuStateBridge(relay), "RikkaDaFeiYu")
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

            fun show() {
                val w = decor.width
                val h = decor.height
                if (w <= 0 || h <= 0) {
                    decor.post { show() }
                    return
                }
                if (!renderPopup.isShowing) {
                    renderPopup.showAtLocation(decor, Gravity.TOP or Gravity.START, 0, 0)
                } else {
                    renderPopup.update(0, 0, w, h)
                }
                if (!touchPopup.isShowing) {
                    touchPopup.showAtLocation(decor, Gravity.TOP or Gravity.START, 0, 0)
                }
                relay.applyPending()
            }

            decor.post { show() }
            onDispose {
                try { touchPopup.dismiss() } catch (_: Throwable) { }
                try { renderPopup.dismiss() } catch (_: Throwable) { }
                try { webView.stopLoading(); webView.destroy() } catch (_: Throwable) { }
            }
        }
    }
    Box(modifier = modifier)
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context)

private class TouchRelayView(
    context: android.content.Context,
    private val target: DaFeiYuWebView,
) : View(context) {
    var controller: TouchRelayController? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val c = controller ?: return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) c.beginGesture()
        val copy = MotionEvent.obtain(event)
        try {
            copy.offsetLocation(c.forwardOffsetX, c.forwardOffsetY)
            target.dispatchTouchEvent(copy)
        } finally {
            copy.recycle()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            c.endGesture()
        }
        return true
    }
}

private class TouchRelayController(
    private val decor: View,
    private val touchPopup: PopupWindow,
    private val touchView: TouchRelayView,
) {
    @Volatile private var widgetRect = RectF(0f, 0f, 1f, 1f)
    @Volatile private var interactive = false
    @Volatile private var gestureActive = false
    @Volatile private var pendingApply = false

    var forwardOffsetX: Float = 0f
        private set
    var forwardOffsetY: Float = 0f
        private set

    fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        val w = max(1, decor.width).toFloat()
        val h = max(1, decor.height).toFloat()
        val sx = w / viewportWidth
        val sy = h / viewportHeight
        val l = left * sx
        val t = top * sy
        val r = right * sx
        val b = bottom * sy
        if (r <= l || b <= t) return
        widgetRect = RectF(l, t, r, b)
        applyPending()
    }

    fun setMenuOpen(open: Boolean) {
        interactive = open
        applyPending()
    }

    fun setInteractive(open: Boolean) {
        interactive = open
        applyPending()
    }

    fun beginGesture() {
        gestureActive = true
    }

    fun endGesture() {
        gestureActive = false
        pendingApply = true
        applyPending()
    }

    fun applyPending() {
        if (gestureActive || !touchPopup.isShowing) {
            pendingApply = true
            return
        }
        pendingApply = false
        val w = max(1, decor.width)
        val h = max(1, decor.height)
        if (interactive) {
            forwardOffsetX = 0f
            forwardOffsetY = 0f
            touchPopup.update(0, 0, w, h)
            touchView.layoutParams = touchView.layoutParams?.apply {
                width = w
                height = h
            } ?: android.view.ViewGroup.LayoutParams(w, h)
        } else {
            val l = widgetRect.left.toInt().coerceIn(0, max(0, w - 1))
            val t = widgetRect.top.toInt().coerceIn(0, max(0, h - 1))
            val r = ceil(widgetRect.right).toInt().coerceIn(l + 1, w)
            val b = ceil(widgetRect.bottom).toInt().coerceIn(t + 1, h)
            val pw = max(1, r - l)
            val ph = max(1, b - t)
            forwardOffsetX = l.toFloat()
            forwardOffsetY = t.toFloat()
            touchPopup.update(l, t, pw, ph)
            touchView.layoutParams = touchView.layoutParams?.apply {
                width = pw
                height = ph
            } ?: android.view.ViewGroup.LayoutParams(pw, ph)
        }
        touchView.requestLayout()
    }
}

private class MenuStateBridge(private val relay: TouchRelayController) {
    @JavascriptInterface fun setMenuOpen(open: Boolean) { relay.setMenuOpen(open) }
    @JavascriptInterface fun setInteractive(open: Boolean) { relay.setInteractive(open) }
    @JavascriptInterface fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        relay.setWidgetRect(left, top, right, bottom, viewportWidth, viewportHeight)
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
