package com.hyper.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.PlaybackController
import com.hyper.music.ui.basic.OverlayDropdownMenu
import com.hyper.music.ui.basic.rememberDropdownColors
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    val dropdownColors = rememberDropdownColors()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    val dropdownBackdrop = if (advancedMaterial) backdrop else null
    val cardShape = RoundedCornerShape(20.dp)

    DetailPage(
        title = "界面设置",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    colors = cardGlassColors(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "操作提示",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        TipText("长按播放界面的专辑图片可将图片保存到相册")
                        TipText("长按更多选项顶部的歌曲信息可复制歌曲名称")
                        TipText("电台模式下，以下部分设置可能无效")
                        TipText("如果长时间未播放，可能是API问题，稍等一下试试，另外不建议短时间内频繁切换歌曲")
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SmallTitle(text = "播放界面", modifier = Modifier.offset(x = (-15).dp))

                    Card(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                        insideMargin = PaddingValues(0.dp),
                        colors = cardGlassColors(),
                    ) {
                        OverlayDropdownMenu(
                            title = "标题位置",
                            entry = remember(settings.titleCentered) {
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            text = "居中",
                                            selected = settings.titleCentered,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setTitleCentered(true)
                                            }
                                        ),
                                        DropdownItem(
                                            text = "左对齐",
                                            selected = !settings.titleCentered,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setTitleCentered(false)
                                            }
                                        ),
                                    )
                                )
                            },
                            collapseOnSelection = true,
                            hyperBackdrop = dropdownBackdrop,
                            dropdownColors = dropdownColors,
                        )
                        OverlayDropdownMenu(
                            title = "专辑封面形状",
                            entry = remember(settings.coverCircular) {
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            text = "圆形",
                                            selected = settings.coverCircular,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setCoverCircular(true)
                                            }
                                        ),
                                        DropdownItem(
                                            text = "圆角矩形",
                                            selected = !settings.coverCircular,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setCoverCircular(false)
                                            }
                                        ),
                                    )
                                )
                            },
                            collapseOnSelection = true,
                            hyperBackdrop = dropdownBackdrop,
                            dropdownColors = dropdownColors,
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
                                            }
                                        ),
                                        DropdownItem(
                                            text = "点击",
                                            selected = settings.lyricSwitchOnTap,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setLyricSwitchOnTap(true)
                                            }
                                        ),
                                    )
                                )
                            },
                            collapseOnSelection = true,
                            hyperBackdrop = dropdownBackdrop,
                            dropdownColors = dropdownColors,
                        )
                        OverlayDropdownMenu(
                            title = "播放页背景",
                            entry = remember(settings.playerBackground) {
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(
                                            text = "封面模糊",
                                            selected = settings.playerBackground == "blur",
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setPlayerBackground("blur")
                                            }
                                        ),
                                        DropdownItem(
                                            text = "动态流光",
                                            selected = settings.playerBackground == "flow",
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setPlayerBackground("flow")
                                            }
                                        ),
                                    )
                                )
                            },
                            collapseOnSelection = true,
                            hyperBackdrop = dropdownBackdrop,
                            dropdownColors = dropdownColors,
                        )
                        AnimatedVisibility(
                            visible = settings.playerBackground == "flow",
                            modifier = Modifier.fillMaxWidth(),
                            enter = fadeIn(tween(200)) +
                                expandVertically(tween(240), expandFrom = Alignment.Top),
                            exit = fadeOut(tween(140)) +
                                shrinkVertically(tween(200), shrinkTowards = Alignment.Top),
                        ) {
                            OverlayDropdownMenu(
                                title = "流光样式",
                                entry = remember(settings.flowStyle) {
                                    DropdownEntry(
                                        items = listOf(
                                            DropdownItem(
                                                text = "跟随系统",
                                                selected = settings.flowStyle == "system",
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    MusicLibrary.setFlowStyle("system")
                                                }
                                            ),
                                            DropdownItem(
                                                text = "深色流光",
                                                selected = settings.flowStyle == "dark",
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    MusicLibrary.setFlowStyle("dark")
                                                }
                                            ),
                                            DropdownItem(
                                                text = "浅色流光",
                                                selected = settings.flowStyle == "light",
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    MusicLibrary.setFlowStyle("light")
                                                }
                                            ),
                                        )
                                    )
                                },
                                collapseOnSelection = true,
                                hyperBackdrop = dropdownBackdrop,
                                dropdownColors = dropdownColors,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SmallTitle(text = "歌词", modifier = Modifier.offset(x = (-15).dp))

                    Card(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                        insideMargin = PaddingValues(0.dp),
                        colors = cardGlassColors(),
                    ) {
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
                                            }
                                        ),
                                        DropdownItem(
                                            text = "左对齐",
                                            selected = !settings.lyricCentered,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setLyricCentered(false)
                                            }
                                        ),
                                    )
                                )
                            },
                            collapseOnSelection = true,
                            hyperBackdrop = dropdownBackdrop,
                            dropdownColors = dropdownColors,
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
                        SwitchPreference(
                            title = "歌词翻译",
                            checked = settings.showLyricsTranslation,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setShowLyricsTranslation(it)
                            }
                        )
                        SwitchPreference(
                            title = "显示罗马音和粤语拼音",
                            summary = "此设置对本地歌曲无效",
                            checked = settings.showLyricRoman,
                            enabled = settings.showLyricsTranslation,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setShowLyricRoman(it)
                            }
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    SmallTitle(text = "其他界面", modifier = Modifier.offset(x = (-15).dp))

                    Card(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                        insideMargin = PaddingValues(0.dp),
                        colors = cardGlassColors(),
                    ) {
                        SwitchPreference(
                            title = "实时歌词",
                            summary = "在系统媒体控制器实时显示歌词",
                            checked = settings.liveLyric,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setLiveLyric(it)
                                PlaybackController.refreshLiveLyric()
                            }
                        )
                        SwitchPreference(
                            title = "播放栏歌词",
                            checked = settings.playBarLyrics,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                MusicLibrary.setPlayBarLyrics(it)
                            }
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun TipText(text: String) {
    Text(
        text = "•  $text",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MiuixTheme.colorScheme.onSurface,
    )
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
