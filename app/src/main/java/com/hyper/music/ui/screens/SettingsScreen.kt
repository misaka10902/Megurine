package com.hyper.music.ui.screens

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.AudioQuality
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.MusicSettings
import com.hyper.music.music.PlaybackController
import com.hyper.music.music.SystemDirectoryPicker
import com.hyper.music.music.displayPathOfTreeUri
import com.hyper.music.ui.basic.OverlayDropdownMenu
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.hyper.music.ui.basic.rememberDropdownColors
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import com.hyper.music.ui.player.BottomBarAreaHeight
import com.hyper.music.ui.player.MiniPlayerHeight
import com.hyper.music.ui.player.MiniPlayerToNavBarSpacing
import com.hyper.music.ui.utils.horizontalContentPadding
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import android.content.SharedPreferences
import com.hyper.music.ui.utils.applyThemeAwareSystemBars
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.rememberNativeTextFieldHandle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.glass.GlassDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

private const val THEME_PREFS = "app_theme_prefs"
private const val THEME_MODE = "theme_mode"
private const val THEME_COLOR = "theme_color"

@Composable
fun SettingsScreen(
    topInset: Dp = 0.dp,
    scrollBehavior: ScrollBehavior? = null,
    hyperBackdrop: Backdrop? = null,
    onOpenWallpaper: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenPlaybackSettings: () -> Unit = {},
    onOpenBiliSourceSettings: () -> Unit = {},
    onOpenDataSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val musicSettings by MusicLibrary.settings.collectAsState()
    val dropdownColors = rememberDropdownColors()

    val themePrefs = remember { context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(themePrefs.getString(THEME_MODE, "system") ?: "system")
    }
    var themeColor by remember {
        mutableStateOf(themePrefs.getString(THEME_COLOR, "miku") ?: "miku")
    }
    DisposableEffect(themePrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == THEME_MODE) {
                themeMode = themePrefs.getString(THEME_MODE, "system") ?: "system"
            } else if (key == THEME_COLOR) {
                themeColor = themePrefs.getString(THEME_COLOR, "miku") ?: "miku"
            }
        }
        themePrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { themePrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(themeMode) {
        (context as? Activity)?.applyThemeAwareSystemBars()
    }

    val folderLauncher = rememberLauncherForActivityResult(SystemDirectoryPicker()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        val display = displayPathOfTreeUri(uri)?.let { if (it.endsWith('/')) it else "$it/" }
            ?: MusicSettings.DEFAULT_DOWNLOAD_PATH
        MusicLibrary.setDownloadPath(display, uri.toString())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove)
            .then(
                scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier
            ),
        contentPadding = PaddingValues(
            start = horizontalContentPadding(),
            end = horizontalContentPadding(),
            top = topInset,
            bottom = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding() + MiniPlayerHeight + MiniPlayerToNavBarSpacing +
                BottomBarAreaHeight + 30.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PlaybackSection(
                musicSettings = musicSettings,
                onOpenBiliSourceSettings = onOpenBiliSourceSettings,
                onOpenPlaybackSettings = onOpenPlaybackSettings,
            )
        }
        item {
            InterfaceSection(
                musicSettings = musicSettings,
                dropdownColors = dropdownColors,
                hyperBackdrop = hyperBackdrop,
                onOpenWallpaper = onOpenWallpaper,
                onOpenPlayerSettings = onOpenPlayerSettings,
                themeMode = themeMode,
                onPickTheme = { mode ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    themeMode = mode
                    themePrefs.edit().putString(THEME_MODE, mode).apply()
                },
                themeColor = themeColor,
                onPickThemeColor = { color ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    themeColor = color
                    themePrefs.edit().putString(THEME_COLOR, color).apply()
                },
            )
        }
        item {
            DownloadSection(
                musicSettings = musicSettings,
                dropdownColors = dropdownColors,
                hyperBackdrop = hyperBackdrop,
                onPickFolder = { folderLauncher.launch(Unit) },
                onOpenDataSettings = onOpenDataSettings,
            )
        }
        item {
            AboutSection(
                onOpenAbout = onOpenAbout,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(text = title, modifier = Modifier.offset(x = (-15).dp))
        Card(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth().cardGlass(RoundedCornerShape(20.dp)),
            insideMargin = PaddingValues(0.dp),
            colors = cardGlassColors(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
        if (footnote != null) {
            Text(
                text = footnote,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PlaybackSection(
    musicSettings: MusicSettings,
    onOpenBiliSourceSettings: () -> Unit,
    onOpenPlaybackSettings: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsSection(title = "基础") {
        SliderPreference(
            value = musicSettings.volume,
            onValueChange = { v ->
                MusicLibrary.setVolume(v)
                PlaybackController.setVolume(v)
            },
            title = "应用音量调节",
            valueText = "${(musicSettings.volume * 100).toInt()}%",
            valueRange = 0f..1f,
            steps = 99,
        )
        ArrowPreference(
            title = "B站音源设置",
            onClick = onOpenBiliSourceSettings,
        )
        ArrowPreference(
            title = "播放设置",
            onClick = onOpenPlaybackSettings,
        )
    }
}

@Composable
private fun InterfaceSection(
    musicSettings: MusicSettings,
    dropdownColors: DropdownColors,
    hyperBackdrop: Backdrop?,
    onOpenWallpaper: () -> Unit,
    onOpenPlayerSettings: () -> Unit,
    themeMode: String,
    onPickTheme: (String) -> Unit,
    themeColor: String,
    onPickThemeColor: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsSection(title = "视觉效果") {
        OverlayDropdownMenu(
            title = "显示模式",
            entry = remember(themeMode) {
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = "浅色模式",
                            selected = themeMode == "light",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPickTheme("light")
                            }
                        ),
                        DropdownItem(
                            text = "深色模式",
                            selected = themeMode == "dark",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPickTheme("dark")
                            }
                        ),
                        DropdownItem(
                            text = "跟随系统",
                            selected = themeMode == "system",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPickTheme("system")
                            }
                        ),
                    )
                )
            },
            collapseOnSelection = true,
            hyperBackdrop = materialBackdrop(hyperBackdrop),
            dropdownColors = dropdownColors,
        )
        OverlayDropdownMenu(
            title = "主题颜色",
            entry = remember(themeColor) {
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = "初音绿",
                            selected = themeColor == "miku",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPickThemeColor("miku")
                            }
                        ),
                        DropdownItem(
                            text = "澎湃蓝",
                            selected = themeColor == "hyperos",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPickThemeColor("hyperos")
                            }
                        ),
                    )
                )
            },
            collapseOnSelection = true,
            hyperBackdrop = materialBackdrop(hyperBackdrop),
            dropdownColors = dropdownColors,
        )
        SwitchPreference(
            title = "高级材质",
            summary = "开启后会同时启用歌词模糊",
            checked = musicSettings.advancedMaterial,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                MusicLibrary.setAdvancedMaterial(it)
            }
        )
        ArrowPreference(
            title = "更换背景",
            onClick = onOpenWallpaper,
        )

        ArrowPreference(
            title = "界面设置",
            onClick = onOpenPlayerSettings,
        )
    }
}

@Composable
private fun DownloadSection(
    musicSettings: MusicSettings,
    dropdownColors: DropdownColors,
    hyperBackdrop: Backdrop?,
    onPickFolder: () -> Unit,
    onOpenDataSettings: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsSection(
        title = "下载与数据",
    ) {
        ArrowPreference(
            title = "下载位置",
            summary = musicSettings.downloadPath,
            onClick = onPickFolder,
        )
        OverlayDropdownMenu(
            title = "下载音质",
            entry = qualityDropdownEntry(musicSettings, isDownload = true),
            collapseOnSelection = true,
            hyperBackdrop = materialBackdrop(hyperBackdrop),
            dropdownColors = dropdownColors,
        )
        SwitchPreference(
            title = "下载时写入元数据",
            checked = musicSettings.downloadMetadata,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                MusicLibrary.setDownloadMetadata(it)
            }
        )
        ArrowPreference(
            title = "数据设置",
            onClick = onOpenDataSettings,
        )
    }
}

@Composable
private fun AboutSection(
    onOpenAbout: () -> Unit,
) {
    SettingsSection(title = "关于") {
        ArrowPreference(
            title = "关于应用",
            onClick = onOpenAbout,
        )
    }
}

@Composable
internal fun qualityDropdownEntry(
    musicSettings: MusicSettings,
    isDownload: Boolean,
): DropdownEntry {
    val haptic = LocalHapticFeedback.current
    val selected = if (isDownload) musicSettings.downloadQuality else musicSettings.quality
    return remember(selected, isDownload) {
        DropdownEntry(
            items = AudioQuality.entries.map { quality ->
                DropdownItem(
                    text = quality.label,
                    summary = quality.bitrateText,
                    selected = selected == quality,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        if (isDownload) {
                            MusicLibrary.setDownloadQuality(quality)
                        } else {
                            MusicLibrary.setQuality(quality)
                            PlaybackController.reloadCurrent()
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun materialBackdrop(backdrop: Backdrop?): Backdrop? =
    backdrop?.takeIf { LocalAdvancedMaterialEnabled.current }

@Composable
internal fun BiliKeywordDialog(
    show: Boolean,
    current: String,
    backdrop: Backdrop?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(show) { mutableStateOf(current) }
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldHandle = rememberNativeTextFieldHandle()
    val scheme = MiuixTheme.colorScheme
    GlassDialog(
        show = show,
        onDismissRequest = {
            fieldHandle.hideIme()
            keyboard?.hide()
            onDismiss()
        },
        title = "设置关键词",
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "用空格将关键词分隔开",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                NativeTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "输入关键词",
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    ),
                    singleLine = true,
                    handle = fieldHandle,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        fieldHandle.hideIme()
                        keyboard?.hide()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确定",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        fieldHandle.hideIme()
                        keyboard?.hide()
                        onConfirm(text.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
