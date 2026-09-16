package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.whale.DaFeiYuSettings
import me.rerere.rikkahub.whale.DaFeiYuSettingsStore
import org.json.JSONObject

@Composable
fun DaFeiYuSettingsPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember { mutableStateOf(DaFeiYuSettingsStore.load(context)) }
    val usage = remember { mutableStateOf(readUsage(DaFeiYuSettingsStore.usageJson(context))) }

    fun save(next: DaFeiYuSettings) {
        settings = next
        DaFeiYuSettingsStore.save(context, next)
    }

    fun saveUsage(patch: JSONObject) {
        DaFeiYuSettingsStore.saveUsagePatch(context, patch.toString())
        usage.value = readUsage(DaFeiYuSettingsStore.usageJson(context))
    }

    fun requestPanel(name: String) {
        DaFeiYuSettingsStore.requestPanel(context, name)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("大肥鱼") }, navigationIcon = { BackButton() }, colors = CustomColors.topBarColors) },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("显示", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("显示气泡", "显示余额、今日已用和提示气泡", settings.bubbleOn) { save(settings.copy(bubbleOn = it)) } }

            item { Text("音效", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("启用音效", "拖动、松开等操作播放音效", settings.sound) { save(settings.copy(sound = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("音量：${(settings.vol * 100).toInt()}%")
                    Slider(value = settings.vol, onValueChange = { save(settings.copy(vol = it)) }, valueRange = 0f..1f)
                }
            }
            item { SettingChoice("音效套装", settings.soundSet, listOf("duck" to "小黄鸭", "fx1" to "音效 1")) { save(settings.copy(soundSet = it)) } }

            item { Text("用量与计费", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingChoice("用量模式", settings.usageMode, listOf("ledger" to "记账", "daily" to "每日", "off" to "关闭")) { save(settings.copy(usageMode = it)) } }
            item { SettingChoice("峰谷文案", settings.peakMode, listOf("default" to "默认", "auto" to "自动", "off" to "关闭")) { save(settings.copy(peakMode = it)) } }
            item { SettingSwitch("显示回合扣费", "在气泡中显示本回合费用", settings.turnCostOn) { save(settings.copy(turnCostOn = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("扣费提示关闭时间：${settings.turnCostCloseMs / 1000} 秒")
                    Slider(value = settings.turnCostCloseMs.toFloat(), onValueChange = { save(settings.copy(turnCostCloseMs = it.toLong())) }, valueRange = 0f..15000f)
                }
            }

            item { Text("提醒", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("余额预警", "余额低于阈值时显示提醒", usage.value.alertOn) { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("on", it))) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("余额预警阈值：¥${"%.2f".format(usage.value.alertBelow)}")
                    Slider(value = usage.value.alertBelow, onValueChange = { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("below", it.toDouble()))) }, valueRange = 0f..100f)
                }
            }
            item { SettingSwitch("今日预算", "今日已用达到预算金额时提醒", usage.value.budgetOn) { saveUsage(JSONObject().put("budget", copyJson(usage.value.budget).put("on", it))) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("今日预算：¥${"%.2f".format(usage.value.budgetAmount)}")
                    Slider(value = usage.value.budgetAmount, onValueChange = { saveUsage(JSONObject().put("budget", copyJson(usage.value.budget).put("amount", it.toDouble()))) }, valueRange = 0f..200f)
                }
            }
            item { SettingSwitch("提醒自动关闭", "余额预警和今日预算提示自动收起", usage.value.autoClose) { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("autoClose", it)).put("budget", copyJson(usage.value.budget).put("autoClose", it))) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("提醒显示时间：${usage.value.ttlSec} 秒")
                    Slider(value = usage.value.ttlSec.toFloat(), onValueChange = { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("ttlSec", it.toInt())).put("budget", copyJson(usage.value.budget).put("ttlSec", it.toInt()))) }, valueRange = 1f..30f)
                }
            }

            item { Text("滚动", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("滚动间隔", "启用后为滚动相关操作增加间隔", settings.scrollGapOn) { save(settings.copy(scrollGapOn = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("滚动间隔：${settings.scrollGapPx}px")
                    Slider(value = settings.scrollGapPx.toFloat(), onValueChange = { save(settings.copy(scrollGapPx = it.toInt())) }, valueRange = 0f..50f)
                }
            }

            item { Text("高级功能", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { EditorButton("自定义泡泡", "编辑点击序列、文字、图片、随机内容和排版") { requestPanel("bubble") } }
            item { EditorButton("角色与资源", "管理角色图片、泡泡图库和音频片段") { requestPanel("role") } }
            item { EditorButton("吸附与镜像", "配置四边吸附和左侧镜像") { requestPanel("snap") } }
            item { EditorButton("每轮消耗提示", "编辑每轮提示内容、自动关闭和任务结束音效") { requestPanel("turn") } }
            item { EditorButton("音效管理", "打开原有音效资源编辑器") { requestPanel("audio") } }
            item {
                Text("说明：上面的高级编辑器会在你返回聊天页后自动打开；大肥鱼本体菜单已经关闭。大小设置不再提供。", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }

            item {
                Button(onClick = {
                    DaFeiYuSettingsStore.reset(context)
                    DaFeiYuSettingsStore.saveUsagePatch(context, "{\"taskEnd\":{\"on\":false,\"sel\":\"frag:exp_orb\"},\"alert\":{\"on\":true,\"below\":5,\"autoClose\":true,\"ttlSec\":6},\"budget\":{\"on\":true,\"amount\":10,\"autoClose\":true,\"ttlSec\":6}}")
                    settings = DaFeiYuSettingsStore.load(context)
                    usage.value = readUsage(DaFeiYuSettingsStore.usageJson(context))
                }, modifier = Modifier.fillMaxWidth()) { Text("恢复默认设置") }
            }
        }
    }
}

private data class UsageUi(
    val alert: JSONObject,
    val budget: JSONObject,
    val alertOn: Boolean,
    val alertBelow: Float,
    val budgetOn: Boolean,
    val budgetAmount: Float,
    val autoClose: Boolean,
    val ttlSec: Int,
)

private fun copyJson(value: JSONObject): JSONObject = try { JSONObject(value.toString()) } catch (_: Exception) { JSONObject() }

private fun readUsage(raw: String): UsageUi {
    val root = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    val s = root.optJSONObject("settings") ?: JSONObject()
    val alert = s.optJSONObject("alert") ?: JSONObject()
    val budget = s.optJSONObject("budget") ?: JSONObject()
    return UsageUi(
        alert = alert,
        budget = budget,
        alertOn = alert.optBoolean("on", true),
        alertBelow = alert.optDouble("below", 5.0).toFloat(),
        budgetOn = budget.optBoolean("on", true),
        budgetAmount = budget.optDouble("amount", 10.0).toFloat(),
        autoClose = alert.optBoolean("autoClose", budget.optBoolean("autoClose", true)),
        ttlSec = alert.optInt("ttlSec", budget.optInt("ttlSec", 6)),
    )
}

@Composable
private fun SettingSwitch(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title)
            Text(summary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingChoice(title: String, value: String, choices: List<Pair<String, String>>, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { (key, label) ->
                Button(onClick = { onChange(key) }, modifier = Modifier.weight(1f)) { Text(if (value == key) "✓ $label" else label) }
            }
        }
    }
}

@Composable
private fun EditorButton(title: String, summary: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(title)
            Text(summary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
