package moe.megurine.music.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.ui.basic.OverlayDropdownMenu
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.AccessControl
import top.yukonga.miuix.kmp.icon.glass.SoundVibration
import top.yukonga.miuix.kmp.icon.glass.SoundWave
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val musicSettings by MusicLibrary.settings.collectAsState()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val dropdownBackdrop = if (advancedMaterial) backdrop else null

    val miSoundIntent = remember {
        Intent()
            .setClassName("com.miui.misound", "com.miui.misound.HeadsetSettingsActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val hasMiSound = remember(context) {
        miSoundIntent.resolveActivityInfo(context.packageManager, 0)
            ?.let { it.enabled && it.exported } == true
    }


    val effectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }

    fun openActivity(intent: Intent, unavailable: String) {
        runCatching { context.startActivity(intent) }.onFailure { error ->
            if (error is ActivityNotFoundException || error is SecurityException) {
                Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DetailPage(
        title = "播放与音频",
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
                    SliderPreference(
                        title = "应用音量调节",
                        value = musicSettings.volume,
                        onValueChange = { v ->
                            MusicLibrary.setVolume(v)
                            PlaybackController.setVolume(v)
                        },
                        valueText = "${(musicSettings.volume * 100).toInt()}%",
                        valueRange = 0f..1f,
                        steps = 99,
                        startAction = { SettingsItemIcon(MiuixIcons.Glass.SoundVibration) },
                    )
                }

                SettingsSection(title = "播放") {
                    val speedOptions = MusicSettings.PLAYBACK_SPEEDS
                    SliderPreference(
                        title = "倍速播放",
                        value = speedOptions.indexOf(musicSettings.playbackSpeed)
                            .coerceAtLeast(0)
                            .toFloat(),
                        onValueChange = { index ->
                            val target = speedOptions[index.toInt().coerceIn(speedOptions.indices)]
                            if (target != musicSettings.playbackSpeed) {
                                MusicLibrary.setPlaybackSpeed(target)
                                PlaybackController.setPlaybackSpeed(target)
                            }
                        },
                        valueText = MusicSettings.PLAYBACK_SPEED_LABELS[
                            speedOptions.indexOf(musicSettings.playbackSpeed).coerceAtLeast(0)
                        ],
                        valueRange = 0f..speedOptions.lastIndex.toFloat(),
                        steps = (speedOptions.size - 2).coerceAtLeast(0),
                        keyPoints = speedOptions.indices.map { it.toFloat() },
                        showKeyPoints = true,
                    )
                    val waitOptions = MusicSettings.BUFFER_WAIT_OPTIONS
                    SliderPreference(
                        title = "缓冲等待时间",
                        summary = "缓冲超过设定时长时，自动播放下一首",
                        value = waitOptions.indexOf(musicSettings.bufferWaitSeconds)
                            .coerceAtLeast(0)
                            .toFloat(),
                        onValueChange = { index ->
                            val target = waitOptions[index.toInt().coerceIn(waitOptions.indices)]
                            if (target != musicSettings.bufferWaitSeconds) {
                                MusicLibrary.setBufferWaitSeconds(target)
                            }
                        },
                        valueText = "${musicSettings.bufferWaitSeconds} 秒",
                        valueRange = 0f..(waitOptions.lastIndex).toFloat(),
                        steps = (waitOptions.size - 2).coerceAtLeast(0),
                        keyPoints = waitOptions.indices.map { it.toFloat() },
                        showKeyPoints = true,
                    )
                    SwitchPreference(
                        title = "自动起播",
                        summary = "启动应用时自动恢复播放",
                        checked = musicSettings.autoResume,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setAutoResume(it)
                        },
                    )
                    OverlayDropdownMenu(
                        title = "播放音质",
                        summary = "实际音质以音源为准",
                        entry = qualityDropdownEntry(musicSettings, isDownload = false),
                        backdrop = dropdownBackdrop,
                    )
                }

                SettingsSection(title = "音频输出") {
                    ArrowPreference(
                        title = "系统均衡器",
                        startAction = { SettingsItemIcon(MiuixIcons.Glass.AccessControl) },
                        onClick = {
                            openSystemEqualizer(
                                context = context,
                                launcher = effectLauncher,
                                audioSessionId = PlaybackController.audioSessionId(),
                            )
                        },
                    )
                    ArrowPreference(
                        title = "小米音质音效设置",
                        startAction = { SettingsItemIcon(MiuixIcons.Glass.SoundWave) },
                        enabled = hasMiSound,
                        onClick = { openActivity(miSoundIntent, "当前设备不支持小米音质音效") },
                    )
                    SwitchPreference(
                        title = "高精度输出",
                        summary = "启用32位浮点音频输出，保留低位深信息，切换时播放会短暂中断，且该功能会导致手机功耗增加，请酌情使用",
                        checked = musicSettings.highPrecisionOutput,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setHighPrecisionOutput(it)
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



private fun openSystemEqualizer(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    audioSessionId: Int,
) {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        .apply { if (audioSessionId > 0) putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId) }
    try {
        launcher.launch(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "当前系统没有可用的均衡器", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "当前系统没有可用的均衡器", Toast.LENGTH_SHORT).show()
    }
}
