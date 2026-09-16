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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import kotlin.math.roundToInt

private const val COMPACT_DP = 250
private const val BALANCE_CACHE_MS = 30_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DaFeiYuWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val client: OkHttpClient = koinInject()
    val eventBus: AppEventBus = koinInject()
    // Never use NaN as a layout offset: Compose's offset { } modifier converts
    // the values to Int during placement, where NaN.roundToInt() throws.
    // Start at a safe value and move to the bottom-right once constraints exist.
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var positioned by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf<DaFeiYuHostView?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val sizePx = with(density) { COMPACT_DP.dp.toPx() }
        val maxX = (maxWidth.value * density.density - sizePx).coerceAtLeast(0f)
        val maxY = (maxHeight.value * density.density - sizePx).coerceAtLeast(0f)
        LaunchedEffect(maxWidth, maxHeight) {
            if (!positioned) {
                x = maxX
                y = maxY
                positioned = true
            } else {
                x = x.coerceIn(0f, maxX)
                y = y.coerceIn(0f, maxY)
            }
        }
        AndroidView(
            factory = {
                DaFeiYuHostView(context, client, settings) { dx, dy ->
                    x = (x + dx).coerceIn(0f, maxX)
                    y = (y + dy).coerceIn(0f, maxY)
                }.also { host = it }
            },
            update = { it.updateSettings(settings) },
            modifier = Modifier.size(COMPACT_DP.dp).offset {
                IntOffset(
                    x.coerceIn(0f, maxX).roundToInt(),
                    y.coerceIn(0f, maxY).roundToInt(),
                )
            },
        )
    }
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
    private val onMove: (Float, Float) -> Unit,
) : FrameLayout(context) {
    private val webView = DaFeiYuWebView(context)
    @Volatile private var currentSettings = settings
    @Volatile private var cachedBalance: BalanceResult? = null

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

    fun updateSettings(value: Settings) { currentSettings = value }
    fun moveWidgetBy(dx: Float, dy: Float) { post { onMove(dx, dy) } }
    fun refreshBalance() {