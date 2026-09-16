package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.event.AppEvent
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.rikkahub.ui.context.LocalSettings
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.koin.compose.koinInject

private const val COMPACT_DP = 250
private const val BALANCE_CACHE_MS = 30_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val client: OkHttpClient = koinInject()
    val eventBus: AppEventBus = koinInject()
    var host by remember { mutableStateOf<DaFeiYuHostView?>(null) }
    AndroidView(
        factory = { DaFeiYuHostView(context, client, settings).also { host = it } },
        update = { it.updateSettings(settings) },
        modifier = modifier.fillMaxSize(),
    )
    LaunchedEffect(eventBus) {
        eventBus.events.collectLatest { event ->
            if (event is AppEvent.ChatGenerationEnded) host?.refreshBalance()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class DaFeiYuHostView(
    context: android.content.Context,
    private val client: OkHttpClient,
    settings: Settings,
) : FrameLayout(context) {
    private val webView = DaFeiYuWebView(context)
    @Volatile private var currentSettings = settings
    @Volatile private var cachedBalance: BalanceResult? = null

    @Volatile private var widgetLeft = Float.POSITIVE_INFINITY
    @Volatile private var widgetTop = Float.POSITIVE_INFINITY
    @Volatile private var widgetRight = Float.NEGATIVE_INFINITY
    @Volatile private var widgetBottom = Float.NEGATIVE_INFINITY
    private var gestureInside = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false
        webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.setSupportZoom(false)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webView.addJavascriptInterface(DaFeiYuBridge(this), "RikkaDaFeiYu")
        webView.webViewClient = DaFeiYuWebViewClient(this)
        addView(webView)
        val whale = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
        val debug = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
        val html = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
            <style>html,body,#root{margin:0;width:100%;height:100%;overflow:visible;background:transparent}</style>
            </head><body><div id="root"></div>
            <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;window.__RIKKAHUB_DAFEIYU_SETTINGS=${DaFeiYuSettingsStore.json(context)};</script>
            <script>$whale</script><script>$debug</script></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
    }

    fun updateWidgetRect(left: Float, top: Float, right: Float, bottom: Float) {
        widgetLeft = left; widgetTop = top; widgetRight = right; widgetBottom = bottom
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                if (!(x >= widgetLeft && x <= widgetRight && y >= widgetTop && y <= widgetBottom)) { gestureInside = false; return false }
                gestureInside = true
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> if (!gestureInside) return false
            else -> if (!gestureInside) return false
        }
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) gestureInside = false
        return handled || gestureInside
    }
    fun updateSettings(value: Settings) { currentSettings = value }

    fun refreshBalance() {
        post {
            cachedBalance = null
            webView.evaluateJavascript("try{typeof refresh==='function'&&refresh(true)}catch(e){}", null)
        }
    }
    fun consumePanelAction(): String = DaFeiYuSettingsStore.consumePanel(context)
    fun saveUsageSettings(json: String) = DaFeiYuSettingsStore.saveUsagePatch(context, json)

    fun balanceJson(): String {
        val now = System.currentTimeMillis()
        cachedBalance?.let { if (now - it.at < BALANCE_CACHE_MS) return it.json }
        val result = fetchCurrentBalance().copy(at = now)
        cachedBalance = result
        return result.json
    }

    fun apiModelsJson(): String {
        val pair = currentModelForWhale() ?: return JSONObject().put("ok", true).put("models", JSONArray()).put("templates", JSONArray()).toString()
        val model = pair.first
        val provider = pair.second
        val balance = JSONObject(balanceJson())
        val item = JSONObject().apply {
            put("id", "rikkahub:${model.id}")
            put("name", model.displayName)
            put("provider", provider.name)
            put("builtin", false)
            put("currency", balance.optString("currency", "CNY"))
            if (balance.optBoolean("ok", false)) {
                put("balance", balance.optDouble("totalBalance"))
                put("balanceMode", "balance")
            } else {
                put("balance", JSONObject.NULL)
                put("balanceMode", "events")
                put("error", balance.optString("error", "余额暂不可用"))
            }
            put("todayUsage", JSONObject.NULL)
        }
        return JSONObject().put("ok", true).put("models", JSONArray().put(item)).put("templates", JSONArray()).toString()
    }

    private fun currentModelForWhale(): Pair<Model, ProviderSetting>? {
        val provider = currentSettings.providers.firstOrNull { provider ->
            provider.models.any { model -> model.id == currentSettings.chatModelId }
        } ?: return null
        val model = provider.models.firstOrNull { it.id == currentSettings.chatModelId } ?: return null
        return model to provider
    }

    private fun fetchCurrentBalance(): BalanceResult {
        val pair = currentModelForWhale() ?: return BalanceResult.error("NO_MODEL", "RikkaHub 当前没有选择聊天模型")
        val provider = pair.second
        val spec = when (provider) {
            is ProviderSetting.OpenAI -> openAiBalanceSpec(provider)
            is ProviderSetting.Google -> null
            is ProviderSetting.Claude -> null
        } ?: return BalanceResult.error("NO_BALANCE_API", "当前提供商没有余额接口")
        if (spec.key.isBlank()) return BalanceResult.error("NO_API_KEY", "RikkaHub 当前提供商没有 API Key")
        return try {
            val request = Request.Builder().url(spec.url)
                .header("Authorization", spec.auth.replace("{key}", spec.key))
                .header("Accept", "application/json").get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return BalanceResult.error("HTTP_${response.code}", "余额接口返回 HTTP ${response.code}")
                val value = readPath(JSONObject(body), spec.path) ?: return BalanceResult.error("BAD_RESPONSE", "余额接口返回中找不到余额字段")
                val amount = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                } ?: return BalanceResult.error("BAD_BALANCE", "余额字段不是数字")
                BalanceResult.success(amount * spec.scale, spec.currency)
            }
        } catch (e: Exception) {
            BalanceResult.error("NETWORK", e.message ?: "余额接口请求失败")
        }
    }

    private fun openAiBalanceSpec(provider: ProviderSetting.OpenAI): BalanceSpec? {
        val base = provider.baseUrl.trimEnd('/')
        val host = runCatching { Uri.parse(base).host.orEmpty().lowercase() }.getOrDefault("")
        if (host == "api.deepseek.com" || host.endsWith(".deepseek.com")) {
            return BalanceSpec("https://api.deepseek.com/user/balance", "balance_infos[0].total_balance", "CNY", provider.apiKey)
        }
        if (provider.balanceOption.enabled) {
            val path = provider.balanceOption.apiPath.trimStart('/')
            return BalanceSpec("$base/$path", provider.balanceOption.resultPath, "CNY", provider.apiKey)
        }
        return null
    }

    private fun readPath(root: JSONObject, path: String): Any? {
        if (path.isBlank()) return null
        var current: Any = root
        val regex = Regex("([^./\\[\\]]+)|\\[(\\d+)\\]")
        for (m in regex.findAll(path)) {
            current = if (m.groupValues[1].isNotEmpty()) {
                if (current !is JSONObject || !current.has(m.groupValues[1])) return null
                current.get(m.groupValues[1])
            } else {
                val index = m.groupValues[2].toIntOrNull() ?: return null
                if (current !is JSONArray || index !in 0 until current.length()) return null
                current.get(index)
            }
        }
        return current
    }
}

private data class BalanceSpec(val url: String, val path: String, val currency: String, val key: String, val auth: String = "Bearer {key}", val scale: Double = 1.0)
private data class BalanceResult(val json: String, val at: Long = System.currentTimeMillis()) {
    companion object {
        fun success(balance: Double, currency: String) = BalanceResult(JSONObject().apply {
            put("ok", true); put("totalBalance", balance); put("currency", currency); put("updatedAt", System.currentTimeMillis())
        }.toString())
        fun error(code: String, message: String) = BalanceResult(JSONObject().apply {
            put("ok", false); put("code", code); put("error", message); put("transient", true)
        }.toString())
    }
}

private class DaFeiYuBridge(private val host: DaFeiYuHostView) {
    @JavascriptInterface fun moveWidgetBy(@Suppress("UNUSED_PARAMETER") dx: Float, @Suppress("UNUSED_PARAMETER") dy: Float) = Unit
    @JavascriptInterface fun setPanelOpen(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
    @JavascriptInterface fun setInteractive(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
    @JavascriptInterface fun setWidgetRect(left: Float, top: Float, right: Float, bottom: Float, @Suppress("UNUSED_PARAMETER") viewportWidth: Float, @Suppress("UNUSED_PARAMETER") viewportHeight: Float) = host.updateWidgetRect(left, top, right, bottom)
    @JavascriptInterface fun saveSizeConfig(@Suppress("UNUSED_PARAMETER") json: String) = Unit
    @JavascriptInterface fun saveUsageSettings(json: String) = host.saveUsageSettings(json)
    @JavascriptInterface fun consumePanelAction(): String = host.consumePanelAction()
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context)
private class DaFeiYuWebViewClient(private val host: DaFeiYuHostView) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? = when (request.url.path) {
        "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png")
        "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif")
        "/dsh-whale/sound/press.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3", "audio/mpeg")
        "/dsh-whale/sound/release.mp3" -> asset(if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3", "audio/mpeg")
        "/dsh-whale/balance.json" -> json(host.balanceJson())
        "/dsh-whale/api-models.json" -> json(host.apiModelsJson())
        "/dsh-whale/size.json" -> json(DaFeiYuSettingsStore.json(host.context))
        "/dsh-whale/usage-settings.json" -> json(DaFeiYuSettingsStore.usageJson(host.context))
        "/dsh-whale/last-turn.json" -> json("{\"ok\":true,\"seq\":0,\"turn\":null,\"amount\":0,\"tokens\":0,\"ts\":0}")
        else -> super.shouldInterceptRequest(view, request)
    }
    private fun asset(path: String, mime: String) = WebResourceResponse(mime, null, host.context.assets.open(path))
    private fun json(body: String) = WebResourceResponse("application/json", "UTF-8", body.byteInputStream())
}