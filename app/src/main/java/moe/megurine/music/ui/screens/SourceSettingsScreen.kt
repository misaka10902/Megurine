package moe.megurine.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.ui.basic.OverlayDropdownMenu
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun SourceSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    var showKeywordDialog by remember { mutableStateOf(false) }
    val dropdownBackdrop = backdrop.takeIf { LocalAdvancedMaterialEnabled.current }
    val searchSourceEntry = remember(settings.searchSource) {
        DropdownEntry(
            items = MusicSettings.SEARCH_SOURCES.mapIndexed { index, source ->
                DropdownItem(
                    text = MusicSettings.SEARCH_SOURCE_LABELS[index],
                    selected = settings.searchSource == source,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        MusicLibrary.setSearchSource(source)
                    },
                )
            },
        )
    }

    DetailPage(
        title = "音源设置",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSection(title = "聚合音源") {
                    OverlayDropdownMenu(
                        title = "搜索音源",
                        summary = "JOOX音源只支持歌曲搜索，且会拖慢搜索速度，建议仅临时开启",
                        entry = searchSourceEntry,
                        backdrop = dropdownBackdrop,
                    )
                    SwitchPreference(
                        title = "音源标签",
                        checked = settings.showSourceTags,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setShowSourceTags(it)
                        }
                    )
                }
                SettingsSection(title = "B站音源") {
                    SwitchPreference(
                        title = "启用",
                        checked = settings.biliEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setBiliEnabled(it)
                        }
                    )
                    SwitchPreference(
                        title = "显示歌词",
                        summary = "可能会出现进度不一致的情况",
                        checked = settings.biliLyrics,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setBiliLyrics(it)
                        }
                    )
                    ArrowPreference(
                        title = "搜索关键词",
                        summary = settings.biliKeywords,
                        enabled = settings.biliEnabled,
                        onClick = { showKeywordDialog = true },
                    )
                }
            }
        }
    }

    BiliKeywordDialog(
        show = showKeywordDialog,
        current = settings.biliKeywords,
        backdrop = backdrop,
        onConfirm = { MusicLibrary.setBiliKeywords(it) },
        onDismiss = { showKeywordDialog = false },
    )
}
