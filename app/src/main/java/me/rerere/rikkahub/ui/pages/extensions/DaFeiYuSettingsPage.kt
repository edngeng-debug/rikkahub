package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.whale.DaFeiYuSettings
import me.rerere.rikkahub.whale.DaFeiYuSettingsStore
import org.json.JSONObject
import java.util.Locale

@Composable
fun DaFeiYuSettingsPage() {
    val context = LocalContext.current
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

    fun saveAdvanced(key: String, value: Any) {
        saveUsage(JSONObject().put("advanced", copyJson(usage.value.advanced).put(key, value)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大肥鱼") },
                navigationIcon = { BackButton() },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("显示", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("显示气泡", "显示余额、今日已用和提示气泡", settings.bubbleOn) { save(settings.copy(bubbleOn = it)) } }

            item { Text("音效", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("启用音效", "拖动、松开等操作播放音效", settings.sound) { save(settings.copy(sound = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("音量：${(settings.vol * 100).toInt()}%")
                    Slider(value = settings.vol, onValueChange = { save(settings.copy(vol = it)) }, valueRange = 0f..1f)
                }
            }
            item { SettingChoice("音效套装", settings.soundSet, listOf("duck" to "小黄鸭", "fx1" to "音效 1")) { save(settings.copy(soundSet = it)) } }

            item { Text("用量与计费", style = MaterialTheme.typography.titleMedium) }
            item { SettingChoice("用量模式", settings.usageMode, listOf("ledger" to "记账", "token" to "实时·令牌", "off" to "关闭")) { save(settings.copy(usageMode = it)) } }
            item { SettingChoice("峰谷文案", settings.peakMode, listOf("default" to "默认", "auto" to "自动", "off" to "关闭")) { save(settings.copy(peakMode = it)) } }
            item { SettingSwitch("显示回合扣费", "在气泡中显示本回合费用", settings.turnCostOn) { save(settings.copy(turnCostOn = it)) } }
            item {
                NumberField("扣费提示关闭时间（秒）", (settings.turnCostCloseMs / 1000).toString(), "0～15", KeyboardType.Number) { v ->
                    v.toLongOrNull()?.coerceIn(0L, 15L)?.let { save(settings.copy(turnCostCloseMs = it * 1000L)) }
                }
            }
            item {
                OutlinedTextField(
                    value = usage.value.advanced.optString("turnCostText", "本回合扣费 {amount} 元"),
                    onValueChange = { saveAdvanced("turnCostText", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("每轮消耗提示文案") },
                    supportingText = { Text("支持 {amount}、{tokens} 占位符") },
                    singleLine = true,
                )
            }
            item { SettingSwitch("任务结束音效", "任务完成时播放选定的结束音效", usage.value.advanced.optBoolean("taskEndOn", false)) { saveAdvanced("taskEndOn", it) } }
            item {
                SettingChoice("任务结束音效", usage.value.advanced.optString("taskEndSound", "exp_orb"), listOf("exp_orb" to "Minecraft·经验球", "duck" to "小黄鸭")) { saveAdvanced("taskEndSound", it) }
            }

            item { Text("提醒", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("余额预警", "余额低于阈值时显示提醒", usage.value.alertOn) { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("on", it))) } }
            item {
                NumberField("余额预警阈值（元）", moneyText(usage.value.alertBelow), "例如 5 或 5.50", KeyboardType.Decimal) { v ->
                    v.toFloatOrNull()?.coerceAtLeast(0f)?.let { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("below", it.toDouble()))) }
                }
            }
            item { SettingSwitch("今日预算", "今日已用达到预算金额时提醒", usage.value.budgetOn) { saveUsage(JSONObject().put("budget", copyJson(usage.value.budget).put("on", it))) } }
            item {
                NumberField("今日预算（元）", moneyText(usage.value.budgetAmount), "例如 10 或 20.50", KeyboardType.Decimal) { v ->
                    v.toFloatOrNull()?.coerceAtLeast(0f)?.let { saveUsage(JSONObject().put("budget", copyJson(usage.value.budget).put("amount", it.toDouble()))) }
                }
            }
            item { SettingSwitch("提醒自动关闭", "余额预警和今日预算提示自动收起", usage.value.autoClose) { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("autoClose", it)).put("budget", copyJson(usage.value.budget).put("autoClose", it))) } }
            item {
                NumberField("提醒显示时间（秒）", usage.value.ttlSec.toString(), "1～30", KeyboardType.Number) { v ->
                    v.toIntOrNull()?.coerceIn(1, 30)?.let { saveUsage(JSONObject().put("alert", copyJson(usage.value.alert).put("ttlSec", it)).put("budget", copyJson(usage.value.budget).put("ttlSec", it))) }
                }
            }

            item { Text("滚动", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("滚动间隔", "启用后为滚动相关操作增加间隔", settings.scrollGapOn) { save(settings.copy(scrollGapOn = it)) } }
            item {
                NumberField("滚动间隔（px）", settings.scrollGapPx.toString(), "0～50", KeyboardType.Number) { v ->
                    v.toIntOrNull()?.coerceIn(0, 50)?.let { save(settings.copy(scrollGapPx = it)) }
                }
            }

            item { Text("吸附与镜像", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("启用边缘吸附", "拖动结束后自动吸附到最近边缘", usage.value.advanced.optBoolean("snapOn", true)) { saveAdvanced("snapOn", it) } }
            item {
                NumberField("吸附距离（px）", usage.value.advanced.optInt("snapPx", 18).toString(), "0～100", KeyboardType.Number) { v ->
                    v.toIntOrNull()?.coerceIn(0, 100)?.let { saveAdvanced("snapPx", it) }
                }
            }
            item { SettingSwitch("左侧吸附时镜像", "贴左边时水平翻转大肥鱼和文字", usage.value.advanced.optBoolean("mirror", true)) { saveAdvanced("mirror", it) } }

            item { Text("泡泡与角色", style = MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("自定义泡泡", "启用自定义提示文本作为泡泡内容", usage.value.advanced.optBoolean("customBubbleOn", false)) { saveAdvanced("customBubbleOn", it) } }
            item {
                OutlinedTextField(
                    value = usage.value.advanced.optString("customBubbleText", ""),
                    onValueChange = { saveAdvanced("customBubbleText", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义泡泡文本") },
                    supportingText = { Text("留空时继续使用默认泡泡序列") },
                    minLines = 2,
                )
            }
            item {
                OutlinedTextField(
                    value = usage.value.advanced.optString("roleName", "大肥鱼"),
                    onValueChange = { saveAdvanced("roleName", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("角色名称") },
                    supportingText = { Text("用于设置页和提示文案；图片资源仍使用内置资源") },
                    singleLine = true,
                )
            }
            item { SettingChoice("角色资源", usage.value.advanced.optString("roleAsset", "default"), listOf("default" to "默认角色", "duck" to "小黄鸭")) { saveAdvanced("roleAsset", it) } }

            item { Text("高级功能", style = MaterialTheme.typography.titleMedium) }
            item {
                InfoCard("拖动与位置", "大肥鱼现在直接嵌入 RikkaHub 聊天页，不使用 PopupWindow；空白区域会把触摸事件交还给聊天界面，键盘区域不会被整块 250dp 容器挡住。")
            }
            item {
                InfoCard("大小设置", "已按要求彻底移除。大肥鱼保持固定尺寸，不再提供大小滑块或尺寸持久化。")
            }
            item {
                InfoCard("角色与资源管理", "可设置角色名称和当前内置角色资源。自定义文件导入入口保留为后续资源管理面板，当前不会再跳回失效的旧网页二级菜单。")
            }
            item {
                InfoCard("自定义泡泡", "现在可以直接编辑自定义泡泡文本并控制开关，不需要进入原网页三级菜单。")
            }
            item {
                InfoCard("吸附与镜像", "吸附开关、吸附距离和左侧镜像已经迁移到本页。")
            }
            item {
                InfoCard("音效管理", "音效开关、音量、套装和任务结束音效均已迁移到本页。")
            }
            item {
                InfoCard("每轮消耗", "显示开关、自动关闭时间和提示文案已经迁移到本页。")
            }

            item { Text("当前版本重点保证：聊天输入、拖动、点击气泡、余额读取和所有设置入口不再依赖失效的网页三级菜单。", style = MaterialTheme.typography.bodySmall) }

            item {
                Button(onClick = {
                    DaFeiYuSettingsStore.reset(context)
                    DaFeiYuSettingsStore.saveUsagePatch(context, "{\"taskEnd\":{\"on\":false,\"sel\":\"frag:exp_orb\"},\"alert\":{\"on\":true,\"below\":5,\"autoClose\":true,\"ttlSec\":6},\"budget\":{\"on\":true,\"amount\":10,\"autoClose\":true,\"ttlSec\":6},\"advanced\":{\"snapOn\":true,\"snapPx\":18,\"mirror\":true,\"customBubbleOn\":false,\"customBubbleText\":\"\",\"roleName\":\"大肥鱼\",\"roleAsset\":\"default\",\"taskEndOn\":false,\"taskEndSound\":\"exp_orb\",\"turnCostText\":\"本回合扣费 {amount} 元\"}}")
                    settings = DaFeiYuSettingsStore.load(context)
                    usage.value = readUsage(DaFeiYuSettingsStore.usageJson(context))
                }, modifier = Modifier.fillMaxWidth()) { Text("恢复默认设置") }
            }
        }
    }
}

private fun moneyText(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.2f", v)

@Composable
private fun NumberField(label: String, value: String, supporting: String, keyboardType: KeyboardType, onValueChange: (String) -> Unit) {
    var text by rememberSaveable(label) { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (text.toFloatOrNull() == null && text.toLongOrNull() == null) text = value
    }
    OutlinedTextField(value = text, onValueChange = { text = it; onValueChange(it) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, supportingText = { Text(supporting) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
}

private data class UsageUi(
    val alert: JSONObject,
    val budget: JSONObject,
    val advanced: JSONObject,
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
    val advanced = s.optJSONObject("advanced") ?: JSONObject()
    return UsageUi(
        alert = alert,
        budget = budget,
        advanced = advanced,
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
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) { Text(title); Text(summary, style = MaterialTheme.typography.bodySmall) }
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
private fun InfoCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
