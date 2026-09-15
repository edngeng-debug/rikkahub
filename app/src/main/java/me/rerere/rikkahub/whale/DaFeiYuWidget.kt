package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
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
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mediaPlaybackRequiresUserGesture = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    webViewClient = DaFeiYuWebViewClient()
                    loadDataWithBaseURL(
                        "https://rikkahub.local/",
                        DaFeiYuWidgetHtml,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            },
        )
    }
}

private class DaFeiYuWebViewClient : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        return when (request.url.path) {
            "/dsh-whale/balance.json" -> json("{\"balance\":0,\"currency\":\"CNY\"}")
            "/dsh-whale/size.json" -> json("{\"scale\":1,\"sound\":true,\"vol\":1,\"soundSet\":\"duck\",\"usageMode\":\"ledger\",\"peakMode\":\"auto\",\"bubbleOn\":true,\"turnCostOn\":true,\"turnCostCloseMs\":5000}")
            else -> super.shouldInterceptRequest(view, request)
        }
    }

    private fun json(body: String) = WebResourceResponse(
        "application/json",
        "UTF-8",
        ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
    )
}

private const val DaFeiYuWidgetHtml = """
<!doctype html>
<html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"></head>
<body style="margin:0;background:transparent;overflow:hidden">
<div id="root"><textarea aria-hidden="true" style="position:absolute;left:-9999px"></textarea></div>
<script src="https://raw.githubusercontent.com/MeteorNOX/DeepSeek-Balance-Whale-Widget/main/assets/whale-widget.js"></script>
</body></html>
"""
// Trigger the corrected build workflow.
