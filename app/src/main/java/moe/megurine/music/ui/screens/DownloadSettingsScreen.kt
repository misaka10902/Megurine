package moe.megurine.music.ui.screens

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
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
import moe.megurine.music.music.SystemDirectoryPicker
import moe.megurine.music.music.displayPathOfTreeUri
import moe.megurine.music.ui.basic.OverlayDropdownMenu
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun DownloadSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    val dropdownBackdrop = backdrop.takeIf { LocalAdvancedMaterialEnabled.current }

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

    DetailPage(
        title = "下载设置",
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
                    ArrowPreference(
                        title = "下载位置",
                        summary = settings.downloadPath,
                        onClick = { folderLauncher.launch(Unit) },
                    )
                    OverlayDropdownMenu(
                        title = "下载音质",
                        summary = "实际音质以音源为准",
                        entry = qualityDropdownEntry(settings, isDownload = true),
                        backdrop = dropdownBackdrop,
                    )
                    val nameEntry = remember(settings.downloadNamePattern) {
                        DropdownEntry(
                            items = MusicSettings.DOWNLOAD_NAME_PATTERNS.mapIndexed { index, pattern ->
                                DropdownItem(
                                    text = MusicSettings.DOWNLOAD_NAME_PATTERN_LABELS[index],
                                    selected = settings.downloadNamePattern == pattern,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        MusicLibrary.setDownloadNamePattern(pattern)
                                    },
                                )
                            },
                        )
                    }
                    OverlayDropdownMenu(
                        title = "下载文件名称",
                        entry = nameEntry,
                        backdrop = dropdownBackdrop,
                    )
                    SwitchPreference(
                        title = "下载时写入元数据",
                        checked = settings.downloadMetadata,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setDownloadMetadata(it)
                        },
                    )
                }
            }
        }
    }
}
