package me.rerere.rikkahub.whale

import android.content.Context
import org.json.JSONObject

data class DaFeiYuSettings(
    val scale: Float = 1f,
    val sound: Boolean = true,
    val vol: Float = 0.9f,
    val soundSet: String = "duck",
    val usageMode: String = "ledger",
    val peakMode: String = "default",
    val bubbleOn: Boolean = true,
    val turnCostOn: Boolean = true,
    val turnCostCloseMs: Long = 5000L,
    val scrollGapOn: Boolean = false,
    val scrollGapPx: Int = 17,
    val menuBtnHide: Boolean = false,
)

object DaFeiYuSettingsStore {
    private const val PREFS = "dafeiyu_settings"

    fun load(context: Context): DaFeiYuSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DaFeiYuSettings(
            scale = 1f,
            sound = p.getBoolean("sound", true), vol = p.getFloat("vol", .9f),
            soundSet = p.getString("soundSet", "duck") ?: "duck", usageMode = p.getString("usageMode", "ledger") ?: "ledger",
            peakMode = p.getString("peakMode", "default") ?: "default", bubbleOn = p.getBoolean("bubbleOn", true),
            turnCostOn = p.getBoolean("turnCostOn", true), turnCostCloseMs = p.getLong("turnCostCloseMs", 5000L),
            scrollGapOn = p.getBoolean("scrollGapOn", false), scrollGapPx = p.getInt("scrollGapPx", 17),
            menuBtnHide = false,
        )
    }

    fun save(context: Context, s: DaFeiYuSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("sound", s.sound).putFloat("vol", s.vol)
            .putString("soundSet", s.soundSet).putString("usageMode", s.usageMode).putString("peakMode", s.peakMode)
            .putBoolean("bubbleOn", s.bubbleOn).putBoolean("turnCostOn", s.turnCostOn)
            .putLong("turnCostCloseMs", s.turnCostCloseMs)
            .apply()
    }

    fun updateFromJson(context: Context, json: String) {
        val o = JSONObject(json); val old = load(context)
        save(context, old.copy(
            sound = if (o.has("sound")) o.optBoolean("sound", old.sound) else old.sound,
            vol = if (o.has("vol")) o.optDouble("vol", old.vol.toDouble()).toFloat() else old.vol,
            soundSet = if (o.has("soundSet")) o.optString("soundSet", old.soundSet) else old.soundSet,
            usageMode = if (o.has("usageMode")) o.optString("usageMode", old.usageMode) else old.usageMode,
            peakMode = if (o.has("peakMode")) o.optString("peakMode", old.peakMode) else old.peakMode,
            bubbleOn = if (o.has("bubbleOn")) o.optBoolean("bubbleOn", old.bubbleOn) else old.bubbleOn,
            turnCostOn = if (o.has("turnCostOn")) o.optBoolean("turnCostOn", old.turnCostOn) else old.turnCostOn,
            turnCostCloseMs = if (o.has("turnCostCloseMs")) o.optLong("turnCostCloseMs", old.turnCostCloseMs) else old.turnCostCloseMs,
        ))
    }

    fun json(context: Context): String {
        val s = load(context)
        return JSONObject().apply {
            put("scale", 1.0); put("sound", s.sound); put("vol", s.vol); put("soundSet", s.soundSet)
            put("usageMode", s.usageMode); put("peakMode", s.peakMode); put("bubbleOn", s.bubbleOn)
            put("turnCostOn", s.turnCostOn); put("turnCostCloseMs", s.turnCostCloseMs)
            put("menuBtnHide", false)
        }.toString()
    }

    fun usageJson(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("usageJson", "{\"ok\":true,\"settings\":{}}") ?: "{\"ok\":true,\"settings\":{}}"

    fun saveUsagePatch(context: Context, patch: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = try { JSONObject(usageJson(context)).optJSONObject("settings") ?: JSONObject() } catch (_: Exception) { JSONObject() }
        val incoming = try { JSONObject(patch) } catch (_: Exception) { JSONObject() }
        val keys = incoming.keys(); while (keys.hasNext()) { val k = keys.next(); current.put(k, incoming.get(k)) }
        prefs.edit().putString("usageJson", JSONObject().put("ok", true).put("settings", current).toString()).apply()
    }

    fun reset(context: Context) = save(context, DaFeiYuSettings())
}
