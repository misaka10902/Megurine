package com.hyper.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.MusicSettings
import com.hyper.music.music.PlaybackController
import com.hyper.music.ui.basic.OverlayDropdownMenu
import com.hyper.music.ui.basic.rememberDropdownColors
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val haptic = LocalHapticFeedback.current
    val musicSettings by MusicLibrary.settings.collectAsState()
    val dropdownColors = rememberDropdownColors()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val dropdownBackdrop = if (advancedMaterial) backdrop else null

    DetailPage(
        title = "播放设置",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(RoundedCornerShape(20.dp)),
                    insideMargin = PaddingValues(0.dp),
                    colors = cardGlassColors(),
                ) {
                    BasicComponent(
                        title = "倍速播放",
                        summary = "调节歌曲播放的速率",
                        bottomAction = {
                            TabRowWithContour(
                                tabs = MusicSettings.PLAYBACK_SPEED_LABELS,
                                selectedTabIndex = MusicSettings.PLAYBACK_SPEEDS.indexOf(musicSettings.playbackSpeed)
                                    .coerceAtLeast(0),
                                onTabSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    val speed = MusicSettings.PLAYBACK_SPEEDS[index]
                                    MusicLibrary.setPlaybackSpeed(speed)
                                    PlaybackController.setPlaybackSpeed(speed)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                    OverlayDropdownMenu(
                        title = "播放音质",
                        entry = qualityDropdownEntry(musicSettings, isDownload = false),
                        collapseOnSelection = true,
                        hyperBackdrop = dropdownBackdrop,
                        dropdownColors = dropdownColors,
                    )
                    SwitchPreference(
                        title = "自动播放上次歌曲",
                        summary = "启动应用时自动继续播放",
                        checked = musicSettings.autoResume,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setAutoResume(it)
                        },
                    )
                    SwitchPreference(
                        title = "音频淡入淡出效果",
                        checked = musicSettings.audioFade,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setAudioFade(it)
                        },
                    )
                    SwitchPreference(
                        title = "音频焦点",
                        summary = "当其他应用播放音频时暂停播放，系统的多声音设置会影响此功能",
                        checked = musicSettings.audioFocus,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setAudioFocus(it)
                            PlaybackController.applyAudioFocus()
                        },
                    )
                }
            }
        }
    }
}
