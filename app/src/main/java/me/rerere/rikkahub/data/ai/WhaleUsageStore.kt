package me.rerere.rikkahub.data.ai

import android.content.Context
import me.rerere.ai.core.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores exact token usage reported by the provider for the bundled DeepSeek whale widget. */
object WhaleUsageStore {
    private const val PREFS = "rikkahub_whale_usage"
    private const val DAY = "day"
    private const val INPUT = "input_tokens"
    private const val OUTPUT = "output_tokens"
    private const val TOTAL = "total_tokens"
    private const val CACHED = "cached_tokens"
    private const val CALLS = "calls"
    private const val HISTORY = "history"

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

        prefs.edit()
            .putInt(INPUT, prefs.getInt(INPUT, 0) + usage.promptTokens)
            .putInt(OUTPUT, prefs.getInt(OUTPUT, 0) + usage.completionTokens)
            .putInt(TOTAL, prefs.getInt(TOTAL, 0) + usage.totalTokens)
            .putInt(CACHED, prefs.getInt(CACHED, 0) + usage.cachedTokens)
            .putInt(CALLS, prefs.getInt(CALLS, 0) + 1)
            .putString(HISTORY, history.toString())
            .apply()
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
