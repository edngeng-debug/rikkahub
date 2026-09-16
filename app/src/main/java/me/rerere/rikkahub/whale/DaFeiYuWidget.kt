package me.rerere.rikkahub.whale

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest
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
import kotlin.math.roundToInt

private const val COMPACT_DP = 260
private const val BALANCE_CACHE_MS = 30_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = LocalSettings.current
    val client: OkHttpClient = koinInject()
    val eventBus: AppEventBus = koinInject()
    var panelOpen by remember { mutableStateOf(false) }
    var x by remember { mutableFloatStateOf(Float.NaN) }
    var y by remember { mutableFloatStateOf(Float.NaN) }
    var host by remember { mutableStateOf<DaFeiYuHostView?>(null) }

    BoxWithConstraints(
        modifier = modifier.imePadding(),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val compactPx = with(density) { COMPACT_DP.dp.toPx() }
        val maxXPx = with(density) { maxWidth.toPx() } - compactPx
        val maxYPx = with(density) { maxHeight.toPx() } - compactPx

        LaunchedEffect(maxWidth, maxHeight, panelOpen) {
            if (panelOpen) return@LaunchedEffect
            if (x.isNaN()) x = maxXPx.coerceAtLeast(0f)
            else x = x.coerceIn(0f, maxXPx.coerceAtLeast(0f))
            if (y.isNaN()) y = maxYPx.coerceAtLeast(0f)
            else y = y.coerceIn(0f, maxYPx.coerceAtLeast(0f))
        }

        AndroidView(
            factory = {
                DaFeiYuHostView(
                    context = context,
                    client = client,
                    settings = settings,
                    onMove = { dx, dy ->
                        if (!panelOpen) {
                            x = (if (x.isNaN()) maxXPx.coerceAtLeast(0f) else x + dx)
                                .coerceIn(0f, maxXPx.coerceAtLeast(0f))
                            y = (if (y.isNaN()) maxYPx.coerceAtLeast(0f) else y + dy)
                                .coerceIn(0f, maxYPx.coerceAtLeast(0f))
                        }
                    },
                    onPanelOpen = { open -> panelOpen = open },
                ).also { host = it }
            },
            update = {
                it.updateSettings(settings)
                if (!panelOpen) it.layoutCompact()
            },
            modifier = if (panelOpen) {
                Modifier.fillMaxSizeSafe()
            } else {
                Modifier
                    .size(COMPACT_DP.dp)
                    .offset { IntOffset(x.coerceAtLeast(0f).roundToInt(), y.coerceAtLeast(0f).roundToInt()) }
            },
        )
    }

    LaunchedEffect(eventBus) {
        eventBus.events.collectLatest { event ->
            if (event is AppEvent.ChatGenerationEnded && event.contentPreview != null) {
                host?.refreshBalance()
            }
        }
    }
}

private fun Modifier.fillMaxSizeSafe(): Modifier = fillMaxSize()

@SuppressLint("SetJavaScriptEnabled")
private class DaFeiYuHostView(
    context: android.content.Context,
    private val client: OkHttpClient,
    settings: Settings,
    private val onMove: (Float, Float) -> Unit,
    private val onPanelOpen: (Boolean) -> Unit,
) : FrameLayout(context) {
    private val webView = DaFeiYuWebView(context)
    @Volatile
    private var currentSettings: Settings = settings
    @Volatile
    private var panelMode = false
    @Volatile
    private var cachedBalance: BalanceResult? = null

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

        val script = context.assets.open("dafeiyu/whale-widget.js").bufferedReader().use { it.readText() }
        val debugScript = context.assets.open("dafeiyu/debug.js").bufferedReader().use { it.readText() }
        val html = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
            </head><body style="margin:0;background:transparent;overflow:visible;width:100%;height:100%">
            <div id="root"></div>
            <script>window.__RIKKAHUB_DAFEIYU_EMBEDDED=true;window.__RIKKAHUB_DAFEIYU_SETTINGS=${DaFeiYuSettingsStore.json(context)};</script>
            <script>$script</script><script>$debugScript</script></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL("https://rikkahub.local/", html, "text/html", "UTF-8", null)
    }

    fun updateSettings(settings: Settings) {
        currentSettings = settings
    }

    fun layoutCompact() {
        if (panelMode) return
        post {
            val lp = webView.layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            lp.width = LayoutParams.MATCH_PARENT
            lp.height = LayoutParams.MATCH_PARENT
            webView.layoutParams = lp
        }
    }

    fun setPanelOpen(open: Boolean) {
        if (panelMode == open) return
        panelMode = open
        post {
            webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            onPanelOpen(open)
        }
    }

    fun moveWidgetBy(dx: Float, dy: Float) {
        if (panelMode) return
        post { onMove(dx, dy) }
    }

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
        val cached = cachedBalance
        if (cached != null && now - cached.at < BALANCE_CACHE_MS) return cached.json

        val result = fetchCurrentBalance()
        cachedBalance = result.copy(at = now)
        return result.json
    }

    private fun fetchCurrentBalance(): BalanceResult {
        val model = currentSettings.getCurrentChatModel()
            ?: return BalanceResult.error("NO_MODEL", "RikkaHub 当前没有选择聊天模型")
        val provider = model.findProvider(currentSettings.providers)
            ?: return BalanceResult.error("NO_PROVIDER", "找不到当前模型对应的提供商")

        val spec = when (provider) {
            is ProviderSetting.OpenAI -> openAiBalanceSpec(provider)
            is ProviderSetting.Google -> null
            is ProviderSetting.Claude -> null
        } ?: return BalanceResult.error("NO_BALANCE_API", "当前提供商没有可用的余额接口")

        if (spec.key.isBlank()) return BalanceResult.error("NO_API_KEY", "RikkaHub 当前提供商没有 API Key")

        return try {
            val request = Request.Builder()
                .url(spec.url)
                .header("Authorization", "Bearer ${spec.key}")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return BalanceResult.error("HTTP_${response.code}", "余额接口返回 HTTP ${response.code}")
                }
                val root = JSONObject(body)
                val value = readPath(root, spec.path)
                    ?: return BalanceResult.error("BAD_RESPONSE", "余额接口返回中找不到余额字段")
                val balance = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                } ?: return BalanceResult.error("BAD_BALANCE", "余额字段不是数字")
                BalanceResult.success(balance, spec.currency)
            }
        } catch (e: Exception) {
            BalanceResult.error("NETWORK", e.message ?: "余额接口请求失败")
        }
    }

    private fun openAiBalanceSpec(provider: ProviderSetting.OpenAI): BalanceSpec? {
        val base = provider.baseUrl.trimEnd('/')
        val host = runCatching { Uri.parse(base).host.orEmpty().lowercase() }.getOrDefault("")
        if (host.contains("deepseek.com")) {
            return BalanceSpec(
                url = "https://api.deepseek.com/user/balance",
                path = "balance_infos[0].total_balance",
                currency = "CNY",
                key = provider.apiKey,
            )
        }
        if (provider.balanceOption.enabled) {
            return BalanceSpec(
                url = base + "/" + provider.balanceOption.apiPath.trimStart('/'),
                path = provider.balanceOption.resultPath,
                currency = "CNY",
                key = provider.apiKey,
            )
        }
        return null
    }

    private fun readPath(root: JSONObject, path: String): Any? {
        if (path.isBlank()) return null
        var current: Any = root
        val token = Regex("([^./\\[\\]]+)|\\[(\\d+)\\]")
        for (match in token.findAll(path)) {
            current = if (match.groupValues[1].isNotEmpty()) {
                if (current !is JSONObject || !current.has(match.groupValues[1])) return null
                current.get(match.groupValues[1])
            } else {
                val index = match.groupValues[2].toIntOrNull() ?: return null
                if (current !is JSONArray || index !in 0 until current.length()) return null
                current.get(index)
            }
        }
        return current
    }
}

private data class BalanceSpec(
    val url: String,
    val path: String,
    val currency: String,
    val key: String,
)

private data class BalanceResult(
    val json: String,
    val at: Long = System.currentTimeMillis(),
) {
    companion object {
        fun success(balance: Double, currency: String) = BalanceResult(
            JSONObject().apply {
                put("ok", true)
                put("totalBalance", balance)
                put("currency", currency)
                put("updatedAt", System.currentTimeMillis())
            }.toString()
        )

        fun error(code: String, message: String) = BalanceResult(
            JSONObject().apply {
                put("ok", false)
                put("code", code)
                put("error", message)
                put("transient", true)
            }.toString()
        )
    }
}

private class DaFeiYuBridge(private val host: DaFeiYuHostView) {
    @JavascriptInterface fun setPanelOpen(value: Boolean) = host.setPanelOpen(value)
    @JavascriptInterface fun moveWidgetBy(dx: Float, dy: Float) = host.moveWidgetBy(dx, dy)
    @JavascriptInterface fun setInteractive(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
    @JavascriptInterface fun setWidgetRect(@Suppress("UNUSED_PARAMETER") left: Float, @Suppress("UNUSED_PARAMETER") top: Float, @Suppress("UNUSED_PARAMETER") right: Float, @Suppress("UNUSED_PARAMETER") bottom: Float, @Suppress("UNUSED_PARAMETER") viewportWidth: Float, @Suppress("UNUSED_PARAMETER") viewportHeight: Float) = Unit
    @JavascriptInterface fun saveSizeConfig(@Suppress("UNUSED_PARAMETER") json: String) = Unit
    @JavascriptInterface fun saveUsageSettings(json: String) = host.saveUsageSettings(json)
    @JavascriptInterface fun consumePanelAction(): String = host.consumePanelAction()
}

private class DaFeiYuWebView(context: android.content.Context) : WebView(context)

private class DaFeiYuWebViewClient(private val host: DaFeiYuHostView) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        return when (request.url.path) {
            "/dsh-whale/image.png" -> asset("dafeiyu/DSniang1.png", "image/png")
            "/dsh-whale/rua.gif" -> asset("dafeiyu/rua.gif", "image/gif")
            "/dsh-whale/sound/press.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D1.mp3" else "dafeiyu/Ya1.mp3",
                "audio/mpeg"
            )
            "/dsh-whale/sound/release.mp3" -> asset(
                if (request.url.getQueryParameter("set") == "fx1") "dafeiyu/D2.mp3" else "dafeiyu/Ya2.mp3",
                "audio/mpeg"
            )
            "/dsh-whale/balance.json" -> json(host.balanceJson())
            "/dsh-whale/api-models.json" -> json(host.apiModelsJson())
            "/dsh-whale/size.json" -> json(DaFeiYuSettingsStore.json(host.context))
            "/dsh-whale/usage-settings.json" -> json(DaFeiYuSettingsStore.usageJson(host.context))
            "/dsh-whale/last-turn.json" -> json("{\"ok\":true,\"seq\":0,\"turn\":null,\"amount\":0,\"tokens\":0,\"ts\":0}")
            else -> super.shouldInterceptRequest(view, request)
        }
    }

    private fun asset(path: String, mime: String) =
        WebResourceResponse(mime, null, host.context.assets.open(path))

    private fun json(body: String) =
        WebResourceResponse("application/json", "UTF-8", body.byteInputStream())
}

private fun DaFeiYuHostView.apiModelsJson(): String {
    val model = currentModelForWhale() ?: return JSONObject().put("ok", true).put("models", JSONArray()).put("templates", JSONArray()).toString()
    val balance = balanceJson()
    val b = runCatching { JSONObject(balance) }.getOrDefault(JSONObject())
    val item = JSONObject().apply {
        put("id", "rikkahub:${model.first.id}")
        put("name", model.first.name)
        put("provider", model.second.name)
        put("builtin", false)
        put("currency", b.optString("currency", "CNY"))
        if (b.optBoolean("ok", false)) {
            put("balance", b.optDouble("totalBalance", Double.NaN))
            put("balanceMode", "balance")
        } else {
            put("balance", JSONObject.NULL)
            put("balanceMode", "events")
        }
        put("todayUsage", JSONObject.NULL)
    }
    return JSONObject().put("ok", true).put("models", JSONArray().put(item)).put("templates", JSONArray()).toString()
}

private fun DaFeiYuHostView.currentModelForWhale(): Pair<me.rerere.ai.provider.Model, ProviderSetting>? {
    val model = currentSettings.getCurrentChatModel() ?: return null
    val provider = model.findProvider(currentSettings.providers) ?: return null
    return model to provider
}
