package moe.megurine.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.music.AudioQuality
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.cardGlass
import moe.megurine.music.ui.effects.miuix.cardGlassColors
import moe.megurine.music.ui.player.BottomBarAreaHeight
import moe.megurine.music.ui.player.MiniPlayerHeight
import moe.megurine.music.ui.player.MiniPlayerToNavBarSpacing
import moe.megurine.music.ui.utils.horizontalContentPadding
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import moe.megurine.music.ui.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import moe.megurine.music.ui.basic.rememberNativeTextFieldHandle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsScreen(
    topInset: Dp = 0.dp,
    scrollBehavior: ScrollBehavior? = null,
    hyperBackdrop: Backdrop? = null,
    onOpenWallpaper: () -> Unit = {},
    onOpenPlayerSettings: () -> Unit = {},
    onOpenPlaybackSettings: () -> Unit = {},
    onOpenSourceSettings: () -> Unit = {},
    onOpenDownloadSettings: () -> Unit = {},
    onOpenDataSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val musicSettings by MusicLibrary.settings.collectAsState()

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
                BottomBarAreaHeight
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(title = "基础设置") {
                ArrowPreference(title = "播放与音频", onClick = onOpenPlaybackSettings)
                ArrowPreference(title = "音源设置", onClick = onOpenSourceSettings)
                ArrowPreference(title = "下载设置", onClick = onOpenDownloadSettings)
                ArrowPreference(title = "数据管理", onClick = onOpenDataSettings)
            }
        }
        item {
            SettingsSection(title = "个性化") {
                SwitchPreference(
                    title = "高级材质",
                    summary = "开启后会同时启用歌词模糊",
                    checked = musicSettings.advancedMaterial,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        MusicLibrary.setAdvancedMaterial(it)
                    },
                )
                ArrowPreference(title = "更换背景", onClick = onOpenWallpaper)
                ArrowPreference(title = "界面设置", onClick = onOpenPlayerSettings)
            }
        }
        item {
            SettingsSection(title = "关于") {
                ArrowPreference(title = "关于应用", onClick = onOpenAbout)
            }
        }
    }
}

@Composable
internal fun SettingsItemIcon(icon: ImageVector) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(end = 12.dp).size(24.dp),
        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
    )
}

@Composable
internal fun SettingsSection(
    title: String? = null,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            SmallTitle(text = title, modifier = Modifier.offset(x = (-15).dp))
        }
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
