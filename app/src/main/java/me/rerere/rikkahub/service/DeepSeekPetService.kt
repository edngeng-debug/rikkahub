package me.rerere.rikkahub.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/** Mobile floating pet based on the interaction model of the upstream DeepSeek whale widget. */
class DeepSeekPetService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    private var key: String = ""
    private var windowManager: WindowManager? = null
    private var root: FrameLayout? = null
    private var pet: ImageView? = null
    private var bubble: TextView? = null
    private var balanceText: TextView? = null
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                key = intent.getStringExtra(EXTRA_KEY).orEmpty()
                if (Settings.canDrawOverlays(this)) {
                    showPet()
                    startRefreshing()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun showPet() {
        if (root != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            contentDescription = "DeepSeek 娘"
            setOnTouchListener(::onPetTouch)
        }
        val bubbleView = TextView(this).apply {
            setTextColor(Color.rgb(67, 86, 145))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(24, 14, 24, 14)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            isVisible = false
        }
        val balanceView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(10, 4, 10, 4)
            setBackgroundColor(Color.argb(190, 32, 49, 112))
            isVisible = false
        }
        container.addView(image, FrameLayout.LayoutParams(190, 190, Gravity.BOTTOM or Gravity.END))
        container.addView(bubbleView, FrameLayout.LayoutParams(190, 100, Gravity.TOP or Gravity.START))
        container.addView(balanceView, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = 8 })

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            210,
            250,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 280
        }
        windowManager?.addView(container, params)
        root = container
        pet = image
        bubble = bubbleView
        balanceText = balanceView
        loadPetImage()
        showBubble("你好呀～点我一下试试！", 2800)
    }

    private fun onPetTouch(view: View, event: MotionEvent): Boolean {
        val params = root?.layoutParams as? WindowManager.LayoutParams ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                moved = false
                view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downX).toInt()
                val dy = (event.rawY - downY).toInt()
                if (abs(dx) > 8 || abs(dy) > 8) moved = true
                if (moved) {
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager?.updateViewLayout(root, params)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(130)
                    .setInterpolator(OvershootInterpolator(2f)).withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                    }.start()
                if (!moved) {
                    showBubble(randomPhrase(), 3200)
                    refreshBalance()
                }
                return true
            }
        }
        return true
    }

    private fun randomPhrase(): String = listOf(
        "欸！你戳我干嘛～", "轻一点啦……", "捏一下就跑？",
        "今天也要好好聊天哦！", "DeepSeek 娘在线中～", "余额帮你看着呢！"
    ).random()

    private fun showBubble(text: String, duration: Long) {
        val view = bubble ?: return
        view.text = text
        view.alpha = 0f
        view.isVisible = true
        view.animate().alpha(1f).setDuration(140).start()
        view.postDelayed({
            view.animate().alpha(0f).setDuration(180).withEndAction { view.isVisible = false }.start()
        }, duration)
    }

    private fun loadPetImage() {
        scope.launch(Dispatchers.IO) {
            runCatching { URL(IMAGE_URL).openStream().use { BitmapFactory.decodeStream(it) } }
                .getOrNull()?.let { bitmap ->
                    launch(Dispatchers.Main) { pet?.setImageBitmap(bitmap) }
                }
        }
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

    private fun refreshBalance() {
        if (key.isBlank()) {
            balanceText?.isVisible = false
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val connection = (URL(BALANCE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    setRequestProperty("Authorization", "Bearer $key")
                }
                connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
            }.getOrNull() ?: return@launch
            val balance = runCatching {
                val json = JSONObject(result)
                val infos = json.optJSONArray("balance_infos") ?: return@runCatching null
                var selected: Double? = null
                for (i in 0 until infos.length()) {
                    val item = infos.optJSONObject(i) ?: continue
                    val currency = item.optString("currency")
                    val value = item.optDouble("total_balance", Double.NaN)
                    if (!value.isNaN() && currency.equals("CNY", true) && value > 0) {
                        selected = value
                        break
                    }
                    if (selected == null && !value.isNaN()) selected = value
                }
                selected
            }.getOrNull() ?: return@launch
            launch(Dispatchers.Main) {
                balanceText?.text = "¥ %.2f".format(balance)
                balanceText?.isVisible = true
            }
        }
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        scope.cancel()
        root?.let { runCatching { windowManager?.removeView(it) } }
        root = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "me.rerere.rikkahub.action.DEEPSEEK_PET_START"
        const val ACTION_STOP = "me.rerere.rikkahub.action.DEEPSEEK_PET_STOP"
        const val EXTRA_KEY = "deepseek_api_key"
        private const val BALANCE_URL = "https://api.deepseek.com/user/balance"
        private const val IMAGE_URL = "https://raw.githubusercontent.com/MeteorNOX/DeepSeek-Balance-Whale-Widget/40cebc2937aea674247a0d0e03e16c154f7b9864/assets/DSniang1.png"

        fun start(context: Context, apiKey: String) {
            if (!Settings.canDrawOverlays(context)) return
            context.startService(Intent(context, DeepSeekPetService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_KEY, apiKey)
            })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DeepSeekPetService::class.java).apply { action = ACTION_STOP })
        }
    }
}
