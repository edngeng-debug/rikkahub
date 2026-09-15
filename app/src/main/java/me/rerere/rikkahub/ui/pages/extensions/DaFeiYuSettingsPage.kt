package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rere.rikkahub.ui.components.nav.BackButton
import me.rere.rikkahub.ui.theme.CustomColors
import me.rere.rikkahub.whale.DaFeiYuSettings
import me.rere.rikkahub.whale.DaFeiYuSettingsStore

@Composable
fun DaFeiYuSettingsPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var settings by remember { mutableStateOf(DaFeiYuSettingsStore.load(context)) }
    fun save(next: DaFeiYuSettings) { settings = next; DaFeiYuSettingsStore.save(context, next) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("大肥鱼") }, navigationIcon = { BackButton() }, colors = CustomColors.topBarColors) },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("显示与交互", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item {
                SettingSwitch("气泡", "显示大肥鱼的对话气泡", settings.bubbleOn) { save(settings.copy(bubbleOn = it)) }
            }
            item {
                SettingSwitch("菜单按钮", "聊天界面不显示菜单按钮", !settings.menuBtnHide) { save(settings.copy(menuBtnHide = !it)) }
            }
            item { Text("音效", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("启用音效", "拖动和松开时播放音效", settings.sound) { save(settings.copy(sound = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("音量：${(settings.vol * 100).toInt()}%")
                    Slider(value = settings.vol, onValueChange = { save(settings.copy(vol = it)) }, valueRange = 0f..1f)
                }
            }
            item { Text("用量显示", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item {
                SettingChoice("用量模式", settings.usageMode, listOf("ledger" to "记账", "daily" to "每日", "off" to "关闭")) { save(settings.copy(usageMode = it)) }
            }
            item {
                SettingChoice("峰值模式", settings.peakMode, listOf("default" to "默认", "auto" to "自动", "off" to "关闭")) { save(settings.copy(peakMode = it)) }
            }
            item { Text("回合扣费", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("显示回合扣费", "在气泡中显示本回合费用", settings.turnCostOn) { save(settings.copy(turnCostOn = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("扣费提示关闭时间：${settings.turnCostCloseMs / 1000} 秒")
                    Slider(value = settings.turnCostCloseMs.toFloat(), onValueChange = { save(settings.copy(turnCostCloseMs = it.toLong())) }, valueRange = 1000f..15000f)
                }
            }
            item { Text("滚动间距", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
            item { SettingSwitch("启用滚动间距", "调整挂件与输入区域的间距", settings.scrollGapOn) { save(settings.copy(scrollGapOn = it)) } }
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("间距：${settings.scrollGapPx}px")
                    Slider(value = settings.scrollGapPx.toFloat(), onValueChange = { save(settings.copy(scrollGapPx = it.toInt())) }, valueRange = 0f..60f)
                }
            }
            item {
                Button(onClick = { save(DaFeiYuSettings()) }, modifier = Modifier.fillMaxWidth()) { Text("恢复默认设置") }
            }
        }
    }
}

@Composable private fun SettingSwitch(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) { Text(title); Text(summary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable private fun SettingChoice(title: String, value: String, choices: List<Pair<String,String>>, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { (key,label) ->
                Button(onClick = { onChange(key) }, modifier = Modifier.weight(1f)) { Text(if (value == key) "✓ $label" else label) }
            }
        }
    }
}
