package moe.megurine.music.ui.screens

import android.app.Activity
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.megurine.music.music.LocalIsWideScreen
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.ui.basic.OverlayDropdownMenu
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.cardGlass
import moe.megurine.music.ui.effects.miuix.cardGlassColors
import moe.megurine.music.ui.utils.ThemeModeDark
import moe.megurine.music.ui.utils.ThemeModeKey
import moe.megurine.music.ui.utils.ThemeModeLight
import moe.megurine.music.ui.utils.ThemeModeSystem
import moe.megurine.music.ui.utils.applyThemeAwareSystemBars
import moe.megurine.music.ui.utils.resolveDarkTheme
import moe.megurine.music.ui.utils.savedThemeMode
import moe.megurine.music.ui.utils.themePreferences
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val dropdownBackdrop = if (advancedMaterial) backdrop else null
    val cardShape = RoundedCornerShape(20.dp)

    val themePrefs = remember { context.themePreferences() }
    var themeMode by remember {
        mutableStateOf(themePrefs.savedThemeMode())
    }
    var themeColor by remember {
        mutableStateOf(themePrefs.getString("theme_color", "hyperos") ?: "hyperos")
    }
    DisposableEffect(themePrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                ThemeModeKey -> themeMode = themePrefs.savedThemeMode()
                "theme_color" -> themeColor = themePrefs.getString("theme_color", "hyperos") ?: "hyperos"
            }
        }
        themePrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { themePrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun pickTheme(mode: String) {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        themeMode = mode
        themePrefs.edit().putString(ThemeModeKey, mode).apply()
        (context as? Activity)?.applyThemeAwareSystemBars()
    }
    fun pickThemeColor(color: String) {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        themeColor = color
        themePrefs.edit().putString("theme_color", color).apply()
    }

    val systemDark = isSystemInDarkTheme()
    val effectiveDark = resolveDarkTheme(themeMode, systemDark)

    DetailPage(
        title = "界面设置",
        subtitle = "部分设置对电台模式无效",
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
                ThemeModeSelector(
                    darkSelected = effectiveDark,
                    onPick = { pickTheme(if (it) ThemeModeDark else ThemeModeLight) },
                )
                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                    insideMargin = PaddingValues(0.dp),
                    colors = cardGlassColors(),
                ) {
                    SwitchPreference(
                        title = "跟随系统",
                        checked = themeMode == ThemeModeSystem,
                        onCheckedChange = { checked ->
                            pickTheme(if (checked) ThemeModeSystem else if (systemDark) ThemeModeDark else ThemeModeLight)
                        },
                    )
                }

                SettingsSection(title = "主界面") {
                    OverlayDropdownMenu(
                        title = "主题颜色",
                        entry = remember(themeColor) {
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                    text = "澎湃蓝",
                                    selected = themeColor == "hyperos",
                                    onClick = { pickThemeColor("hyperos") },
                                ),
                                DropdownItem(
                                    text = "初音绿",
                                    selected = themeColor == "miku",
                                    onClick = { pickThemeColor("miku") },
                                ),
                                DropdownItem(
                                    text = "巡音粉",
                                    selected = themeColor == "luka",
                                    onClick = { pickThemeColor("luka") },
                                ),
                                DropdownItem(
                                    text = "镜音橙",
                                    selected = themeColor == "kagamine",
                                    onClick = { pickThemeColor("kagamine") },
                                ),
                                ),
                            )
                        },
                        backdrop = dropdownBackdrop,
                    )
                    SwitchPreference(
                        title = "播放栏歌词",
                        checked = settings.playBarLyrics,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setPlayBarLyrics(it)
                        },
                    )
                    SwitchPreference(
                        title = "歌曲列表隐藏时长数据",
                        summary = "此设置对JOOX歌曲无效",
                        checked = settings.hideSongDuration,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setHideSongDuration(it)
                        },
                    )
                }

                SettingsSection(title = "媒体控制器") {
                    SwitchPreference(
                        title = "实时歌词",
                        summary = "将歌词实时广播到系统媒体控制器以及智能穿戴、车载蓝牙等设备上",
                        checked = settings.liveLyric,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setLiveLyric(it)
                            PlaybackController.refreshLiveLyric()
                        },
                    )
                    SwitchPreference(
                        title = "隐藏控制器图标",
                        checked = settings.hideControllerIcons,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setHideControllerIcons(it)
                        },
                    )
                    OverlayDropdownMenu(
                        title = "右侧图标",
                        enabled = !settings.hideControllerIcons,
                        entry = remember(settings.controllerRightIcon) {
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "收藏",
                                        selected = settings.controllerRightIcon == MusicSettings.CONTROLLER_ICON_FAVORITE,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setControllerRightIcon(MusicSettings.CONTROLLER_ICON_FAVORITE)
                                                        },
                                    ),
                                    DropdownItem(
                                        text = "关闭应用",
                                        selected = settings.controllerRightIcon == MusicSettings.CONTROLLER_ICON_CLOSE,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setControllerRightIcon(MusicSettings.CONTROLLER_ICON_CLOSE)
                                                        },
                                    ),
                                ),
                            )
                        },
                        backdrop = dropdownBackdrop,
                    )
                }

                SettingsSection(title = "播放界面") {
                    ArrowPreference(title = "歌词设置", onClick = onOpenLyricsSettings)
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
                                        },
                                    ),
                                    DropdownItem(
                                        text = "左对齐",
                                        selected = !settings.titleCentered,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setTitleCentered(false)
                                        },
                                    ),
                                ),
                            )
                        },
                        backdrop = dropdownBackdrop,
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
                                        },
                                    ),
                                    DropdownItem(
                                        text = "圆角矩形",
                                        selected = !settings.coverCircular,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setCoverCircular(false)
                                        },
                                    ),
                                ),
                            )
                        },
                        backdrop = dropdownBackdrop,
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
                                        },
                                    ),
                                    DropdownItem(
                                        text = "动态流光",
                                        selected = settings.playerBackground == "flow",
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setPlayerBackground("flow")
                                        },
                                    ),
                                ),
                            )
                        },
                        backdrop = dropdownBackdrop,
                    )
                    AnimatedVisibility(
                        visible = settings.playerBackground == "flow",
                        modifier = Modifier.fillMaxWidth(),
                        enter = fadeIn(tween(200)) + expandVertically(tween(240), expandFrom = Alignment.Top),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top),
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
                                            },
                                        ),
                                        DropdownItem(
                                            text = "深色流光",
                                            selected = settings.flowStyle == "dark",
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setFlowStyle("dark")
                                            },
                                        ),
                                        DropdownItem(
                                            text = "浅色流光",
                                            selected = settings.flowStyle == "light",
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                MusicLibrary.setFlowStyle("light")
                                            },
                                        ),
                                    ),
                                )
                            },
                            backdrop = dropdownBackdrop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    darkSelected: Boolean,
    onPick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeModePreview(
                dark = false,
                isSelected = !darkSelected,
                onClick = { onPick(false) },
                modifier = Modifier.weight(1f),
            )
            ThemeModePreview(
                dark = true,
                isSelected = darkSelected,
                onClick = { onPick(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeModePreview(
    dark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            selected = isSelected
        },
        cornerRadius = 26.dp,
        insideMargin = PaddingValues(vertical = 6.dp),
        colors = CardDefaults.defaultColors(color = Color.Transparent),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .squircleBorder(
                    width = 3.dp,
                    color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                    cornerRadius = 26.dp,
                )
                .padding(6.dp),
        ) {
            ThemeModeIllustration(dark = dark)
        }
        Text(
            text = if (dark) "深色模式" else "浅色模式",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = if (isSelected) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurfaceSecondary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ThemeModeIllustration(
    dark: Boolean,
    modifier: Modifier = Modifier,
) {

    val primary = MiuixTheme.colorScheme.primary

    val wide = LocalIsWideScreen.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (wide) 2f else 1f)
            .squircleClip(20.dp),
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(primary, lerp(primary, Color.White, 0.45f)),
            ),
        )
        val illustrationWidth = size.width * if (wide) 0.7f else 1f
        val start = (size.width - illustrationWidth) / 2f
        val unitX = illustrationWidth / 680f
        val unitY = size.height / 680f
        val cornerUnit = minOf(unitX, unitY)
        val panelColor = if (dark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
        val lineColor = if (dark) Color.White.copy(alpha = 0.35f)
        else lerp(primary, Color.Black, 0.48f).copy(alpha = 0.6f)
        for (top in listOf(148f, 356f)) {
            drawRoundRect(
                color = panelColor,
                topLeft = Offset(start + 48f * unitX, top * unitY),
                size = Size(584f * unitX, 176f * unitY),
                cornerRadius = CornerRadius(44f * cornerUnit),
            )
            for ((offset, width) in listOf(52f to 460f, 100f to 312f)) {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(start + 112f * unitX, (top + offset) * unitY),
                    size = Size(width * unitX, 24f * unitY),
                    cornerRadius = CornerRadius(12f * cornerUnit),
                )
            }
        }
    }
}
