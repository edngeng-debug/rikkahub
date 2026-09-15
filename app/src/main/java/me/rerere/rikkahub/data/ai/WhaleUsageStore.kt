package me.rerere.rikkahub.data.ai

import android.content.Context
import me.rerere.ai.core.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/** Stores DeepSeek usage and exposes the last completed turn in the format expected by the original whale widget. */
object WhaleUsageStore {
    private const val PREFS = "rikkahub_whale_usage"
    private const val DAY = "day"
    private const val INPUT = "input_tokens"
    private const val OUTPUT = "output_tokens"
    private const val TOTAL = "total_tokens"
    private const val CACHED = "cached_tokens"
    private const val CALLS = "calls"
    private const val HISTORY = "history"
    private const val LAST_SEQ = "last_seq"
    private const val LAST_TURN = "last_turn"
    private const val LAST_AMOUNT = "last_amount"
    private const val LAST_TOKENS = "last_tokens"
    private const val LAST_TS = "last_ts"

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    @Synchronized
    fun record(context: Context, providerName: String?, modelId: String, usage: TokenUsage) {
        if (!providerName.equals("DeepSeek", ignoreCase = true)) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = today()
        if (prefs.getString(DAY, null) != day) {
            prefs.edit()
                .putString(DAY, day)
                .putInt(INPUT, 0)
                .putInt(OUTPUT, 0)
                .putInt(TOTAL, 0)
                .putInt(CACHED, 0)
                .putInt(CALLS, 0)
                .putString(HISTORY, "[]")
                .apply()
        }

        val history = runCatching { JSONArray(prefs.getString(HISTORY, "[]")) }.getOrElse { JSONArray() }
        history.put(
            JSONObject()
                .put("time", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
                .put("model", modelId)
                .put("inputTokens", usage.promptTokens)
                .put("outputTokens", usage.completionTokens)
                .put("totalTokens", usage.totalTokens)
                .put("cachedTokens", usage.cachedTokens)
        )
        while (history.length() > 200) history.remove(0)

        val amount = calculateCost(modelId, usage)
        val seq = prefs.getLong(LAST_SEQ, 0L) + 1L
        prefs.edit()
            .putInt(INPUT, prefs.getInt(INPUT, 0) + usage.promptTokens)
            .putInt(OUTPUT, prefs.getInt(OUTPUT, 0) + usage.completionTokens)
            .putInt(TOTAL, prefs.getInt(TOTAL, 0) + usage.totalTokens)
            .putInt(CACHED, prefs.getInt(CACHED, 0) + usage.cachedTokens)
            .putInt(CALLS, prefs.getInt(CALLS, 0) + 1)
            .putString(HISTORY, history.toString())
            .putLong(LAST_SEQ, seq)
            .putString(LAST_TURN, "rikkahub-${prefs.getInt(CALLS, 0) + 1}")
            .putFloat(LAST_AMOUNT, amount.toFloat())
            .putInt(LAST_TOKENS, usage.totalTokens)
            .putLong(LAST_TS, System.currentTimeMillis())
            .apply()
    }

    private fun calculateCost(modelId: String, usage: TokenUsage): Double {
        val peak = isPeakHour()
        val multiplier = if (modelId.lowercase(Locale.US).contains("pro")) 3.0 else 1.0
        val hitPrice = if (peak) 0.04 else 0.02
        val missPrice = if (peak) 2.0 else 1.0
        val outputPrice = if (peak) 8.0 else 4.0
        val cached = usage.cachedTokens.toDouble()
        val miss = max(0.0, usage.promptTokens.toDouble() - cached)
        return multiplier * ((cached * hitPrice + miss * missPrice + usage.completionTokens * outputPrice) / 1_000_000.0)
    }

    private fun isPeakHour(): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return false
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 9..11 || hour in 14..17
    }

    fun lastTurnSnapshot(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return JSONObject()
            .put("ok", true)
            .put("seq", prefs.getLong(LAST_SEQ, 0L))
            .put("turn", prefs.getString(LAST_TURN, null))
            .put("amount", prefs.getFloat(LAST_AMOUNT, 0f).toDouble())
            .put("tokens", prefs.getInt(LAST_TOKENS, 0))
            .put("ts", prefs.getLong(LAST_TS, 0L))
            .toString()
    }

    fun snapshot(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = today()
        if (prefs.getString(DAY, null) != day) {
            return JSONObject()
                .put("day", day)
                .put("inputTokens", 0)
                .put("outputTokens", 0)
                .put("totalTokens", 0)
                .put("cachedTokens", 0)
                .put("calls", 0)
                .put("history", JSONArray())
                .toString()
        }
        return JSONObject()
            .put("day", day)
            .put("inputTokens", prefs.getInt(INPUT, 0))
            .put("outputTokens", prefs.getInt(OUTPUT, 0))
            .put("totalTokens", prefs.getInt(TOTAL, 0))
            .put("cachedTokens", prefs.getInt(CACHED, 0))
            .put("calls", prefs.getInt(CALLS, 0))
            .put("history", runCatching { JSONArray(prefs.getString(HISTORY, "[]")) }.getOrElse { JSONArray() })
            .toString()
    }
}
