package me.rerere.rikkahub.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.ai.WhaleUsageStore
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Android host for MeteorNOX/DeepSeek-Balance-Whale-Widget.
 *
 * The upstream widget JavaScript and all artwork/audio are bundled from the
 * repository submodule. This service only supplies the host routes that the
 * original DSH plugin normally exposes through its Node webServer.
 */
class DeepSeekPetService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    private var apiKey = ""
    private var windowManager: WindowManager? = null
    private var window: View? = null
    private var webView: WebView? = null
    private var params: WindowManager.LayoutParams? = null
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var cancelledSyntheticPointer = false
    private var lastBalance: Double? = null
    private var balanceCurrency = "CNY"
    private var todayUsage = 0.0
    private var updatedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                apiKey = intent.getStringExtra(EXTRA_KEY).orEmpty()
                if (Settings.canDrawOverlays(this)) {
                    ensureForegroundNotification()
                    showPet()
                    startRefreshing()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "DeepSeek 大肥鱼", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("DeepSeek 大肥鱼正在陪你")
            .setContentText("点击、按住或拖动大肥鱼即可互动")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showPet() {
        if (window != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val size = (320 * resources.displayMetrics.density).toInt()
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val wmParams = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - size).coerceAtLeast(0)
            y = (screenH - size - (72 * resources.displayMetrics.density).toInt()).coerceAtLeast(0)
        }

        val view = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = UpstreamWidgetWebViewClient()
            addJavascriptInterface(WhaleBridge(), "RikkaHubWhaleBridge")
        }

        view.setOnTouchListener { _, event -> onOverlayTouch(event) }
        windowManager?.addView(view, wmParams)
        window = view
        webView = view
        params = wmParams
        loadHostPage()
    }

    private fun loadHostPage() {
        val html = assets.open("deepseek_whale_host.html").bufferedReader().use { it.readText() }
        webView?.loadDataWithBaseURL(
            "https://rikkahub.local/",
            html,
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun onOverlayTouch(event: MotionEvent): Boolean {
        val p = params ?: return true
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = p.x
                startY = p.y
                moved = false
                cancelledSyntheticPointer = false
                dispatchSyntheticPointer("pointerdown", x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downX).toInt()
                val dy = (event.rawY - downY).toInt()
                if (!moved && (abs(dx) > 8 || abs(dy) > 8)) {
                    moved = true
                    dispatchSyntheticPointer("pointercancel", x, y)
                    cancelledSyntheticPointer = true
                }
                if (moved) {
                    p.x = startX + dx
                    p.y = startY + dy
                    clampWindow(p)
                    windowManager?.updateViewLayout(window, p)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!moved && !cancelledSyntheticPointer) {
                    dispatchSyntheticPointer("pointerup", x, y)
                    dispatchSyntheticClick(x, y)
                } else if (!cancelledSyntheticPointer) {
                    dispatchSyntheticPointer("pointercancel", x, y)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!cancelledSyntheticPointer) dispatchSyntheticPointer("pointercancel", x, y)
            }
        }
        return true
    }

    private fun clampWindow(p: WindowManager.LayoutParams) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val width = p.width
        val height = p.height
        p.x = p.x.coerceIn(-width / 2, screenW - width / 2)
        p.y = p.y.coerceIn(0, screenH - height)
    }

    private fun dispatchSyntheticPointer(type: String, x: Float, y: Float) {
        val buttons = if (type == "pointerup" || type == "pointercancel") 0 else 1
        val js = """
            (function(){
              var t=document.elementFromPoint($x,$y)||document.querySelector('.dshwv-root')||document.body;
              var e=new PointerEvent('$type',{bubbles:true,cancelable:true,pointerId:1,pointerType:'touch',isPrimary:true,clientX:$x,clientY:$y,buttons:$buttons});
              t.dispatchEvent(e);
            })();
        """.trimIndent()
        webView?.post { webView?.evaluateJavascript(js, null) }
    }

    private fun dispatchSyntheticClick(x: Float, y: Float) {
        val js = """
            (function(){
              var t=document.elementFromPoint($x,$y)||document.querySelector('.dshwv-root')||document.body;
              t.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,clientX:$x,clientY:$y}));
            })();
        """.trimIndent()
        webView?.post { webView?.evaluateJavascript(js, null) }
    }

    private fun startRefreshing() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            refreshBalance()
            while (isActive) {
                delay(60_000)
                refreshBalance()
            }
        }
    }

    private suspend fun refreshBalance() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext
        runCatching {
            val connection = (URL(BALANCE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) return@runCatching

            val json = JSONObject(text)
            val infos = json.optJSONArray("balance_infos") ?: return@runCatching
            var selected: JSONObject? = null
            for (i in 0 until infos.length()) {
                val item = infos.optJSONObject(i) ?: continue
                if (item.optString("currency") == "CNY" && item.optDouble("total_balance", 0.0) > 0) {
                    selected = item
                    break
                }
            }
            if (selected == null) {
                for (i in 0 until infos.length()) {
                    val item = infos.optJSONObject(i) ?: continue
                    if (item.optDouble("total_balance", 0.0) != 0.0) {
                        selected = item
                        break
                    }
                }
            }
            if (selected == null && infos.length() > 0) selected = infos.optJSONObject(0)
            val item = selected ?: return@runCatching
            val balance = item.optDouble("total_balance", Double.NaN)
            if (balance.isNaN()) return@runCatching
            val currency = item.optString("currency", "CNY")
            updateLedger(balance, currency)
        }
    }

    @Synchronized
    private fun updateLedger(balance: Double, currency: String) {
        val prefs = getSharedPreferences(LEDGER_PREFS, MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val oldDay = prefs.getString("day", null)
        val oldCurrency = prefs.getString("currency", null)
        var usage = if (oldDay == today && oldCurrency == currency) {
            prefs.getString("today_usage", "0")?.toDoubleOrNull() ?: 0.0
        } else 0.0
        val old = if (oldDay == today && oldCurrency == currency) {
            prefs.getString("last_balance", null)?.toDoubleOrNull()
        } else null
        if (old != null && balance < old) usage += old - balance

        prefs.edit()
            .putString("day", today)
            .putString("currency", currency)
            .putString("last_balance", balance.toString())
            .putString("today_usage", usage.toString())
            .putLong("updated_at", System.currentTimeMillis())
            .apply()

        lastBalance = balance
        balanceCurrency = currency
        todayUsage = usage
        updatedAt = System.currentTimeMillis()
    }

    private fun balanceJson(): String {
        val prefs = getSharedPreferences(LEDGER_PREFS, MODE_PRIVATE)
        if (lastBalance == null) {
            lastBalance = prefs.getString("last_balance", null)?.toDoubleOrNull()
            balanceCurrency = prefs.getString("currency", "CNY") ?: "CNY"
            todayUsage = prefs.getString("today_usage", "0")?.toDoubleOrNull() ?: 0.0
            updatedAt = prefs.getLong("updated_at", 0L)
        }
        return if (lastBalance != null) {
            JSONObject()
                .put("ok", true)
                .put("totalBalance", lastBalance)
                .put("currency", balanceCurrency)
                .put("updatedAt", updatedAt)
                .put("todayUsage", todayUsage)
                .put("isPeak", isPeakHour())
                .put("usageMode", "ledger")
                .toString()
        } else {
            JSONObject().put("ok", false).put("code", "NO_BALANCE").put("error", "尚未获取到余额").toString()
        }
    }

    private fun isPeakHour(): Boolean {
        val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
        val day = now.get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return false
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return hour in 9..11 || hour in 14..17
    }

    private fun sizeJson(): String {
        val p = getSharedPreferences(SIZE_PREFS, MODE_PRIVATE)
        return JSONObject()
            .put("scale", p.getFloat("scale", 1f).toDouble())
            .put("sound", p.getBoolean("sound", true))
            .put("vol", p.getFloat("vol", 1f).toDouble())
            .put("soundSet", p.getString("soundSet", "duck"))
            .put("usageMode", p.getString("usageMode", "ledger"))
            .put("peakMode", p.getString("peakMode", "default"))
            .put("bubbleOn", p.getBoolean("bubbleOn", true))
            .put("turnCostOn", p.getBoolean("turnCostOn", false))
            .put("turnCostCloseMs", p.getLong("turnCostCloseMs", 5000L))
            .toString()
    }

    private fun saveSize(json: String): String {
        runCatching {
            val o = JSONObject(json)
            getSharedPreferences(SIZE_PREFS, MODE_PRIVATE).edit()
                .putFloat("scale", o.optDouble("scale", 1.0).toFloat())
                .putBoolean("sound", o.optBoolean("sound", true))
                .putFloat("vol", o.optDouble("vol", 1.0).toFloat())
                .putString("soundSet", o.optString("soundSet", "duck"))
                .putString("usageMode", o.optString("usageMode", "ledger"))
                .putString("peakMode", o.optString("peakMode", "default"))
                .putBoolean("bubbleOn", o.optBoolean("bubbleOn", true))
                .putBoolean("turnCostOn", o.optBoolean("turnCostOn", false))
                .putLong("turnCostCloseMs", o.optLong("turnCostCloseMs", 5000L))
                .apply()
        }
        return sizeJson()
    }

    private inner class WhaleBridge {
        @JavascriptInterface
        fun getJson(path: String): String = when {
            path.startsWith("/dsh-whale/balance.json") -> balanceJson()
            path.startsWith("/dsh-whale/last-turn.json") -> WhaleUsageStore.lastTurnSnapshot(this@DeepSeekPetService)
            path.startsWith("/dsh-whale/size.json") -> sizeJson()
            path.startsWith("/dsh-whale/audio.json") -> JSONObject().put("groups", org.json.JSONArray()).put("fragments", org.json.JSONArray()).toString()
            path.startsWith("/dsh-whale/bubble.json") -> "{}"
            path.startsWith("/dsh-whale/roles.json") -> "{}"
            else -> "{}"
        }

        @JavascriptInterface
        fun putJson(path: String, body: String): String = when {
            path.startsWith("/dsh-whale/size.json") -> saveSize(body)
            path.startsWith("/dsh-whale/bubble.json") -> body.ifBlank { "{}" }
            path.startsWith("/dsh-whale/roles.json") -> body.ifBlank { "{}" }
            else -> "{}"
        }
    }

    private inner class UpstreamWidgetWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
            if (uri.host != "rikkahub.local") return super.shouldInterceptRequest(view, request)
            val path = uri.path ?: return super.shouldInterceptRequest(view, request)
            if (!path.startsWith("/dsh-whale/")) return super.shouldInterceptRequest(view, request)

            val (mime, asset) = when {
                path == "/dsh-whale/widget.js" -> "application/javascript" to "dsh-whale/widget.js"
                path == "/dsh-whale/image.png" -> "image/png" to "dsh-whale/DSniang1.png"
                path == "/dsh-whale/rua.gif" -> "image/gif" to "dsh-whale/rua.gif"
                path == "/dsh-whale/sound/press.mp3" -> "audio/mpeg" to if (uri.getQueryParameter("set") == "fx1") "dsh-whale/D1.mp3" else "dsh-whale/Ya1.mp3"
                path == "/dsh-whale/sound/release.mp3" -> "audio/mpeg" to if (uri.getQueryParameter("set") == "fx1") "dsh-whale/D2.mp3" else "dsh-whale/Ya2.mp3"
                path == "/dsh-whale/audio-fragment.wav" && uri.getQueryParameter("id") == "exp_orb" -> "audio/wav" to "dsh-whale/minecraft-exp-orb.wav"
                path == "/dsh-whale/audio-fragment.wav" && uri.getQueryParameter("id") == "end_a" -> "audio/wav" to "dsh-whale/task-end-a.wav"
                path == "/dsh-whale/bubble-img.png" -> "image/png" to "dsh-whale/DSH2.png"
                else -> return super.shouldInterceptRequest(view, request)
            }

            return runCatching {
                val input = assets.open(asset)
                WebResourceResponse(mime, null, input)
            }.getOrNull()
        }
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        scope.cancel()
        webView?.let { runCatching { it.stopLoading(); it.destroy() } }
        webView = null
        window?.let { runCatching { windowManager?.removeView(it) } }
        window = null
        windowManager = null
        params = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "deepseek_big_fish_pet"
        private const val NOTIFICATION_ID = 18601
        private const val EXTRA_KEY = "deepseek_api_key"
        private const val ACTION_START = "me.rerere.rikkahub.action.BIG_FISH_START"
        private const val ACTION_STOP = "me.rerere.rikkahub.action.BIG_FISH_STOP"
        private const val BALANCE_URL = "https://api.deepseek.com/user/balance"
        private const val LEDGER_PREFS = "rikkahub_big_fish_ledger"
        private const val SIZE_PREFS = "rikkahub_big_fish_size"

        fun start(context: Context, apiKey: String) {
            if (!Settings.canDrawOverlays(context) || apiKey.isBlank()) return
            val intent = Intent(context, DeepSeekPetService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_KEY, apiKey)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DeepSeekPetService::class.java).setAction(ACTION_STOP))
        }
    }
}
