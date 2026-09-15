package me.rerere.rikkahub.whale

import android.content.Context

data class DaFeiYuSettings(
    val char: Float = 1f,
    val bubble: Float = 1f,
    val label: Float = 1f,
    val amount: Float = 1f,
    val hint: Float = 1f,
)

object DaFeiYuSettingsStore {
    private const val PREFS = "dafeiyu_settings"
    private const val CHAR = "char"
    private const val BUBBLE = "bubble"
    private const val LABEL = "label"
    private const val AMOUNT = "amount"
    private const val HINT = "hint"

    fun load(context: Context): DaFeiYuSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DaFeiYuSettings(
            char = p.getFloat(CHAR, 1f),
            bubble = p.getFloat(BUBBLE, 1f),
            label = p.getFloat(LABEL, 1f),
            amount = p.getFloat(AMOUNT, 1f),
            hint = p.getFloat(HINT, 1f),
        )
    }

    fun save(context: Context, settings: DaFeiYuSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(CHAR, settings.char)
            .putFloat(BUBBLE, settings.bubble)
            .putFloat(LABEL, settings.label)
            .putFloat(AMOUNT, settings.amount)
            .putFloat(HINT, settings.hint)
            .apply()
    }

    fun reset(context: Context) {
        save(context, DaFeiYuSettings())
    }

    fun json(context: Context): String {
        val s = load(context)
        return "{\"char\":${s.char},\"bubble\":${s.bubble},\"label\":${s.label},\"amount\":${s.amount},\"hint\":${s.hint}}"
    }
}
