package com.hyper.music.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hyper.music.music.AppCache
import com.hyper.music.music.BackupStore
import com.hyper.music.music.MusicLibrary
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import com.hyper.music.ui.player.PlayerArtwork
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupStore.Payload?>(null) }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) { AppCache.usageBytes(context) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val payload = withContext(Dispatchers.IO) { BackupStore.read(context, uri) }
            if (payload == null) {
                Toast.makeText(context, "备份文件无法读取", Toast.LENGTH_SHORT).show()
            } else {
                pendingRestore = payload
            }
        }
    }

    DetailPage(
        title = "数据设置",
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
                    ArrowPreference(
                        title = "导出数据",
                        summary = if (exporting) "正在导出…" else "支持备份歌单、收藏与播放历史",
                        onClick = {
                            if (exporting) return@ArrowPreference
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            exporting = true
                            scope.launch {
                                val name = withContext(Dispatchers.IO) {
                                    BackupStore.exportToDownloads(context)
                                }
                                exporting = false
                                Toast.makeText(
                                    context,
                                    if (name != null) "已导出到 Download/$name" else "导出失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                    ArrowPreference(
                        title = "恢复数据",
                        summary = "从备份的 JSON 文件恢复",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            restoreLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream"),
                            )
                        },
                    )
                    ArrowPreference(
                        title = "WebDAV 配置",
                        summary = "配置 WebDAV 服务器以收听云端歌曲",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            Toast.makeText(context, "开发中，敬请期待", Toast.LENGTH_SHORT).show()
                        },
                    )
                    ArrowPreference(
                        title = "清理缓存",
                        titleColor = BasicComponentColors(
                            color = MiuixTheme.colorScheme.error,
                            disabledColor = MiuixTheme.colorScheme.error.copy(alpha = 0.5f),
                        ),
                        endActions = {
                            Text(
                                text = formatCacheSize(cacheBytes),
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showClearCacheDialog = true
                        },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        show = showClearCacheDialog,
        title = "清理缓存",
        summary = "是否清理本地缓存？",
        confirmText = "清理",
        onConfirm = {
            showClearCacheDialog = false
            scope.launch {
                val left = withContext(Dispatchers.IO) {
                    AppCache.clear(context)
                    AppCache.usageBytes(context)
                }
                PlayerArtwork.clear()
                cacheBytes = left
                Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { showClearCacheDialog = false },
        backdrop = backdrop,
    )

    ConfirmDialog(
        show = pendingRestore != null,
        title = "恢复数据",
        summary = "将覆盖当前所有歌单、收藏的歌曲与播放历史，确定要恢复吗？",
        confirmText = "恢复",
        onConfirm = {
            val payload = pendingRestore
            pendingRestore = null
            if (payload != null) {
                MusicLibrary.replaceAll(payload.playlists, payload.favorites, payload.history)
                Toast.makeText(context, "恢复成功", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { pendingRestore = null },
        backdrop = backdrop,
    )
}

private fun formatCacheSize(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    val mb = bytes / 1024.0 / 1024.0
    return if (gb >= 0.1) {
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        String.format(Locale.US, "%.2f MB", mb)
    }
}
