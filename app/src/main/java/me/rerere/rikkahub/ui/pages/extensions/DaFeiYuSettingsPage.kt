package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.whale.DaFeiYuSettings
import me.rerere.rikkahub.whale.DaFeiYuSettingsStore

@Composable
fun DaFeiYuSettingsPage() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var settings by remember { mutableStateOf(DaFeiYuSettingsStore.load(context)) }

    fun update(transform: (DaFeiYuSettings) -> DaFeiYuSettings) {
        val next = transform(settings)
        settings = next
        DaFeiYuSettingsStore.save(context, next)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("大肥鱼设置") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    "这里调整大肥鱼本体、气泡和文字大小。返回聊天后会自动使用新的设置。",
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item {
                ScaleSetting("大肥鱼", settings.char) { v -> update { it.copy(char = v) } }
            }
            item {
                ScaleSetting("气泡", settings.bubble) { v -> update { it.copy(bubble = v) } }
            }
            item {
                ScaleSetting("标题", settings.label) { v -> update { it.copy(label = v) } }
            }
            item {
                ScaleSetting("余额数字", settings.amount) { v -> update { it.copy(amount = v) } }
            }
            item {
                ScaleSetting("提示文字", settings.hint) { v -> update { it.copy(hint = v) } }
            }
            item {
                Button(
                    onClick = {
                        DaFeiYuSettingsStore.reset(context)
                        settings = DaFeiYuSettings()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("恢复默认")
                }
            }
        }
    }
}

@Composable
private fun ScaleSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$title：${"%.2f".format(value)}×")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.5f..2f,
            steps = 29,
        )
    }
}
