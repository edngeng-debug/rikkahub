package me.rerere.rikkahub.ui.pages.extensions

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.whale.DaFeiYuSettingsStore
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuSettingsPage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大肥鱼设置") },
                navigationIcon = { BackButton() },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    DaFeiYuSettingsWebView(context).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mediaPlaybackRequiresUserGesture = false
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        webViewClient = SettingsWebViewClient(context)
                        addJavascriptInterface(SettingsBridge(context), "RikkaDaFeiYuSettings")

                        val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
                        val html = """
                            <!doctype html><html><head>
                            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
                            </head><body style="margin:0;background:transparent;overflow:auto;width:100%;height:100%">
                            <div id="root"><textarea aria-hidden="true" tabindex="-1" style="position:absolute;left:-99999px;top:-99999px;width:1px;height:1px;opacity:0"></textarea></div>
                            <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;</script>
                            <script>(function(){var f=window.fetch;window.fetch=function(input,init){try{var u=typeof input==='string'?input:(input&&input.url)||'';var m=((init&&init.method)||((input&&input.method)||'GET')).toUpperCase();if(u.indexOf('/dsh-whale/size.json')>=0){if(m==='PUT'){window.RikkaDaFeiYuSettings.saveSize((init&&init.body)||'{}');return Promise.resolve(new Response(JSON.stringify({ok:true}),{status:200,headers:{'Content-Type':'application/json'}}));}return Promise.resolve(new Response(window.RikkaDaFeiYuSettings.getSize(),{status:200,headers:{'Content-Type':'application/json'}}));}if(u.indexOf('/dsh-whale/usage-settings.json')>=0){if(m==='PUT'){window.RikkaDaFeiYuSettings.saveUsage((init&&init.body)||'{}');return Promise.resolve(new Response(JSON.stringify({ok:true,settings:{}}),{status:200,headers:{'Content-Type':'application/json'}}));}return Promise.resolve(new Response(window.RikkaDaFeiYuSettings.getUsage(),{status:200,headers:{'Content-Type':'application/json'}}));}}catch(e){}return f.apply(this,arguments)}})();</script>
                            <script>$script</script>
                            <script>setTimeout(function(){var n=0,t=setInterval(function(){n++;var b=document.querySelector('.dshwv-menu-btn');if(b){clearInterval(t);try{b.click()}catch(e){}}if(n>20)clearInterval(t)},250)},250);</script>
                            </body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { webView -> webView.stopLoading(); webView.destroy() },
            )
        }
    }
}

private class DaFeiYuSettingsWebView(context: android.content.Context) : WebView(context)

private class SettingsBridge(private val context: android.content.Context) {
    @JavascriptInterface fun getSize(): String = DaFeiYuSettingsStore.json(context)
    @JavascriptInterface fun saveSize(json: String) { DaFeiYuSettingsStore.updateFromJson(context, json) }
    @JavascriptInterface fun getUsage(): String = DaFeiYuSettingsStore.usageJson(context)
    @JavascriptInterface fun saveUsage(json: String) { DaFeiYuSettingsStore.saveUsagePatch(context, json) }
}

private class SettingsWebViewClient(private val context: android.content.Context) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? = when (request.url.path) {
        "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png")
        "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif")
        "/dsh-whale/sound/press.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3", "audio/mpeg")
        "/dsh-whale/sound/release.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3", "audio/mpeg")
        "/dsh-whale/balance.json" -> json("{\"balance\":0,\"currency\":\"CNY\"}")
        "/dsh-whale/size.json" -> json(DaFeiYuSettingsStore.json(context))
        "/dsh-whale/usage-settings.json" -> json(DaFeiYuSettingsStore.usageJson(context))
        "/dsh-whale/last-turn.json" -> json("{\"seq\":0,\"cost\":0}")
        "/dsh-whale/bubble.json" -> json("{\"ok\":true,\"config\":{\"lib\":[]}}")
        else -> super.shouldInterceptRequest(view, request)
    }
    private fun asset(path: String, mime: String) = WebResourceResponse(mime, null, context.assets.open(path))
    private fun json(body: String) = WebResourceResponse("application/json", "UTF-8", ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)))
}
