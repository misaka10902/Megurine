package moe.megurine.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.ui.basic.OverlayDropdownMenu
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun LyricsSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    DetailPage(
        title = "歌词设置",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSection {
                    LyricsSettingsRows(includeTranslation = true, backdrop = backdrop)
                }
            }
        }
    }
}



@Composable
internal fun LyricsSettingsRows(
    includeTranslation: Boolean = true,
    backdrop: LayerBackdrop? = null,
) {
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    Column(modifier = Modifier.fillMaxWidth()) {
        OverlayDropdownMenu(
            title = "歌词位置",
            entry = remember(settings.lyricCentered) {
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = "居中",
                            selected = settings.lyricCentered,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setLyricCentered(true)
                            },
                        ),
                        DropdownItem(
                            text = "左对齐",
                            selected = !settings.lyricCentered,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setLyricCentered(false)
                            },
                        ),
                    ),
                )
            },
            backdrop = backdrop?.takeIf { advancedMaterial },
        )
        OverlayDropdownMenu(
            title = "歌词切换方式",
            entry = remember(settings.lyricSwitchOnTap) {
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = "滑动",
                            selected = !settings.lyricSwitchOnTap,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setLyricSwitchOnTap(false)
                            },
                        ),
                        DropdownItem(
                            text = "点击",
                            selected = settings.lyricSwitchOnTap,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setLyricSwitchOnTap(true)
                            },
                        ),
                    ),
                )
            },
            backdrop = backdrop?.takeIf { advancedMaterial },
        )
        SliderPreference(
            value = settings.lyricFontSize,
            onValueChange = { MusicLibrary.setLyricFontSize(it) },
            title = "歌词字体大小",
            valueText = "${settings.lyricFontSize.toInt()}sp",
            valueRange = 14f..36f,
            steps = 21,
        )
        SliderPreference(
            value = settings.lyricFontWeight.toFloat(),
            onValueChange = { raw ->
                val snapped = (raw / 100f).roundToInt() * 100
                MusicLibrary.setLyricFontWeight(snapped.coerceIn(300, 900))
            },
            title = "歌词字重",
            valueText = LyricWeights
                .minByOrNull { abs(it.first - settings.lyricFontWeight) }?.second,
            valueRange = 300f..900f,
            steps = LyricWeights.size - 2,
            showKeyPoints = true,
        )
        if (includeTranslation) {
            SwitchPreference(
                title = "歌词翻译",
                checked = settings.showLyricsTranslation,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    MusicLibrary.setShowLyricsTranslation(it)
                },
            )
        }
        SwitchPreference(
            title = "显示罗马音和粤语拼音",
            summary = "此设置对本地歌曲无效",
            checked = settings.showLyricRoman,
            enabled = settings.showLyricsTranslation,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                MusicLibrary.setShowLyricRoman(it)
            },
        )
    }
}

private val LyricWeights = listOf(
    300 to "细",
    400 to "常规",
    500 to "中等",
    600 to "半粗",
    700 to "粗",
    800 to "特粗",
    900 to "极粗",
)
