package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(160.dp)
                    .height(160.dp),
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
                        webViewClient = DaFeiYuWebViewClient(context)

                        val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                        val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
                        val settingsJson = DaFeiYuSettingsStore.json(context)
                        val html = """
                            <!doctype html>
                            <html>
                            <head>
                              <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                            </head>
                            <body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%;min-height:100%">
                              <div id="root">
                                <textarea aria-hidden="true" tabindex="-1" style="position:absolute;left:-99999px;top:-99999px;width:1px;height:1px;opacity:0"></textarea>
                              </div>
                              <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;window.__RIKKAHUB_DAFEIYU_SETTINGS=$settingsJson;</script>
                              <script>$script</script>
                              <script>$debugScript</script>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                },
            )
        }
    }
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context)

private class DaFeiYuWebViewClient(private val context: android.content.Context) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        return when (request.url.path) {
            "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png")
            "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif")
            "/dsh-whale/sound/press.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3",
                "audio/mpeg",
            )
            "/dsh-whale/sound/release.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3",
                "audio/mpeg",
            )
            "/dsh-whale/balance.json" -> json("{\"balance\":0,\"currency\":\"CNY\"}")
            "/dsh-whale/size.json" -> json("{\"scale\":1,\"sound\":true,\"vol\":1,\"soundSet\":\"duck\",\"usageMode\":\"ledger\",\"peakMode\":\"auto\",\"bubbleOn\":true,\"turnCostOn\":true,\"turnCostCloseMs\":5000}")
            "/dsh-whale/last-turn.json" -> json("{\"seq\":0,\"cost\":0}")
            else -> super.shouldInterceptRequest(view, request)
        }
    }

    private fun asset(path: String, mime: String): WebResourceResponse =
        WebResourceResponse(mime, null, context.assets.open(path))

    private fun json(body: String) = WebResourceResponse(
        "application/json",
        "UTF-8",
        ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
    )
}
