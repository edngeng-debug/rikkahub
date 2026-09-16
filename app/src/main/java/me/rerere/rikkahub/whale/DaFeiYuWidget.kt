package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 大肥鱼独立浮层：普通状态只占据挂件本身的区域，不挡住 RikkaHub；
 * 打开二级 UI 或拖拽时临时扩展到全屏，结束后再回到挂件的实际位置。
 */
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
            val x = max(0, hostView.width - compactSize)
            val y = max(0, hostView.height - compactSize)
            popup.showAtLocation(hostView, Gravity.TOP or Gravity.START, x, y)
        }
    }

    fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) {
        hostView.post {
            lastRootRect = floatArrayOf(left, top, right, bottom)
            lastViewport = floatArrayOf(viewportWidth, viewportHeight)
            if (!popup.isShowing || interactive || viewportWidth <= 1f || viewportHeight <= 1f) return@post

            val sx = hostView.width.toFloat() / viewportWidth
            val sy = hostView.height.toFloat() / viewportHeight
            val rootWidth = ((right - left) * sx).coerceAtLeast(1f)
            val rootHeight = ((bottom - top) * sy).coerceAtLeast(1f)
            val x = (left * sx + rootWidth - compactSize).roundToInt()
            val y = (top * sy + rootHeight - compactSize).roundToInt()
            popup.update(x, y, compactSize, compactSize)
        }
    }

    fun setInteractive(open: Boolean) {
        hostView.post {
            interactive = open
            if (!popup.isShowing) return@post
            if (open) {
                popup.update(0, 0, -1, -1)
            } else {
                val viewportWidth = lastViewport[0]
                val viewportHeight = lastViewport[1]
                if (viewportWidth > 1f && viewportHeight > 1f) {
                    val sx = hostView.width.toFloat() / viewportWidth
                    val sy = hostView.height.toFloat() / viewportHeight
                    val left = lastRootRect[0]
                    val top = lastRootRect[1]
                    val right = lastRootRect[2]
                    val bottom = lastRootRect[3]
                    val rootWidth = ((right - left) * sx).coerceAtLeast(1f)
                    val rootHeight = ((bottom - top) * sy).coerceAtLeast(1f)
                    val x = (left * sx + rootWidth - compactSize).roundToInt()
                    val y = (top * sy + rootHeight - compactSize).roundToInt()
                    popup.update(x, y, compactSize, compactSize)
                } else {
                    popup.update(0, 0, compactSize, compactSize)
                }
            }
        }
    }

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
    @JavascriptInterface
    fun setInteractive(value: Boolean) = overlay.setInteractive(value)

    @JavascriptInterface
    fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, viewportWidth: Float, viewportHeight: Float) =
        overlay.setWidgetRect(left, top, right, bottom, viewportWidth, viewportHeight)

    @JavascriptInterface
    fun saveSizeConfig(json: String) = Unit

    @JavascriptInterface
    fun saveUsageSettings(json: String) = overlay.saveUsageSettings(json)
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

    private fun json(body: String) = WebResourceResponse(
        "application/json",
        "UTF-8",
        ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
    )
}
