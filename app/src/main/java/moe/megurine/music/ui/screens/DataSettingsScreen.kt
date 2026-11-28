package moe.megurine.music.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import moe.megurine.music.music.AppCache
import moe.megurine.music.music.BackupStore
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.music.WebDavApi
import moe.megurine.music.ui.components.AppTooltip
import moe.megurine.music.ui.components.ConfirmDialog
import moe.megurine.music.ui.components.HyperSheet
import moe.megurine.music.ui.components.SheetCard
import moe.megurine.music.ui.components.SheetHeader
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.player.PlayerArtwork
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import moe.megurine.music.ui.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import moe.megurine.music.ui.basic.rememberNativeTextFieldHandle
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.glass.Backup
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val settings by MusicLibrary.settings.collectAsState()
    val webdavReady = settings.webdavUrl.isNotBlank()

    var cacheBytes by remember { mutableLongStateOf(0L) }
    var cloudCacheBytes by remember { mutableLongStateOf(0L) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearCloudCacheDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupStore.Payload?>(null) }
    var backupFiles by remember { mutableStateOf<List<WebDavApi.CloudBackup>>(emptyList()) }
    var showBackupSheet by remember { mutableStateOf(false) }
    val infoTooltip = rememberTooltipState()

    fun refreshSizes() {
        scope.launch {
            cacheBytes = withContext(Dispatchers.IO) { AppCache.usageBytes(context) }
            cloudCacheBytes = withContext(Dispatchers.IO) { WebDavApi.offlineCacheSize(context) }
        }
    }

    LaunchedEffect(Unit) { refreshSizes() }

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
        title = "数据管理",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
        endAction = {

            val density = LocalDensity.current
            val tooltipProvider = remember(density) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        val marginPx = with(density) { 6.dp.roundToPx() }
                        val spacingPx = with(density) { 8.dp.roundToPx() }
                        val x = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(0)
                        val y = anchorBounds.bottom + spacingPx
                        return IntOffset(x, y.coerceAtLeast(0))
                    }
                }
            }
            TooltipBox(
                positionProvider = tooltipProvider,
                tooltip = {
                    AppTooltip(
                        lines = listOf("配置服务前，建议先在服务器根目录创建 Megurine 文件夹，以正常与云端同步"),
                        maxWidth = 300.dp,
                    )
                },
                state = infoTooltip,
            ) {
                GlassIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch { infoTooltip.show() }
                    },
                ) {
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = "说明",
                        modifier = Modifier.size(22.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSection(title = "备份和恢复") {
                    ArrowPreference(
                        title = "导出数据",
                        summary = if (exporting) "正在导出…" else "支持备份歌单、收藏、历史与应用设置",
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
                }

                SettingsSection(title = "云端服务") {
                    ArrowPreference(
                        title = "WebDAV 服务器配置",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showWebDavDialog = true
                        },
                    )
                    ArrowPreference(
                        title = "连通性检验",
                        summary = "点击检查 WebDAV 服务器的连通性",
                        enabled = webdavReady,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                val result = WebDavApi.checkConnection()
                                val message = result.getOrElse {
                                    "连接失败：${it.message ?: "请检查服务器配置"}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    ArrowPreference(
                        title = "云端备份",
                        summary = "将歌单、收藏等数据备份到云端",
                        enabled = webdavReady,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                val result = WebDavApi.uploadBackup()
                                val message = result.fold(
                                    onSuccess = { name -> "已备份到云端：$name" },
                                    onFailure = { "云端备份失败：${it.message ?: "请稍后重试"}" },
                                )
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    ArrowPreference(
                        title = "云端恢复",
                        summary = if (restoring) "正在读取云端备份…" else "选择云端保存的备份以恢复",
                        enabled = webdavReady && !restoring,
                        onClick = {
                            if (restoring) return@ArrowPreference
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            restoring = true
                            scope.launch {
                                val result = WebDavApi.listBackups()
                                restoring = false
                                result.onFailure {
                                    Toast.makeText(
                                        context,
                                        it.message ?: "云端备份读取失败",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                val list = result.getOrNull() ?: return@launch
                                if (list.isEmpty()) {
                                    Toast.makeText(context, "云端还没有备份文件", Toast.LENGTH_SHORT).show()
                                } else {
                                    backupFiles = list
                                    showBackupSheet = true
                                }
                            }
                        },
                    )
                    SwitchPreference(
                        title = "边听边存",
                        summary = "收听云端歌曲时，将歌曲缓存到本地",
                        checked = settings.webdavCacheWhileListening,
                        enabled = webdavReady,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setWebDavCacheWhileListening(enabled)
                            if (!enabled) {
                                scope.launch {
                                    withContext(Dispatchers.IO) { WebDavApi.clearOfflineCache(context) }
                                    cloudCacheBytes = 0L
                                    Toast.makeText(
                                        context,
                                        "已关闭边听边存，云端歌曲缓存已清理",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                    )
                }

                SettingsSection {
                    ArrowPreference(
                        title = "清理云端歌曲缓存",
                        titleColor = errorTitleColors(),
                        endActions = {
                            Text(
                                text = formatCacheSize(cloudCacheBytes),
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showClearCloudCacheDialog = true
                        },
                    )
                    ArrowPreference(
                        title = "清理缓存",
                        titleColor = errorTitleColors(),
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

    WebDavConfigDialog(
        show = showWebDavDialog,
        current = settings,
        backdrop = backdrop,
        onSave = { url, username, password ->
            MusicLibrary.setWebDavConfig(url, username, password)
            refreshSizes()
        },
        onRemove = {
            MusicLibrary.setWebDavConfig("", "", "")
            refreshSizes()
        },
        onDismiss = { showWebDavDialog = false },
    )

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
                cloudCacheBytes = withContext(Dispatchers.IO) { WebDavApi.offlineCacheSize(context) }
                Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { showClearCacheDialog = false },
        backdrop = backdrop,
    )

    ConfirmDialog(
        show = showClearCloudCacheDialog,
        title = "清理云端歌曲缓存",
        summary = "是否清理已缓存到本机的云端歌曲？再次播放时需要重新下载",
        confirmText = "清理",
        onConfirm = {
            showClearCloudCacheDialog = false
            scope.launch {
                withContext(Dispatchers.IO) { WebDavApi.clearOfflineCache(context) }
                cloudCacheBytes = withContext(Dispatchers.IO) { WebDavApi.offlineCacheSize(context) }
                Toast.makeText(context, "云端歌曲缓存已清理", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { showClearCloudCacheDialog = false },
        backdrop = backdrop,
    )

    ConfirmDialog(
        show = pendingRestore != null,
        title = "恢复数据",
        summary = "将覆盖当前所有歌单、收藏、播放历史与应用设置，确定要恢复吗？",
        confirmText = "恢复",
        onConfirm = {
            val payload = pendingRestore
            pendingRestore = null
            if (payload != null) {
                MusicLibrary.replaceAll(payload.playlists, payload.favorites, payload.history)
                payload.settings?.let { json ->
                    MusicLibrary.importSettingsJson(json)
                    val restored = MusicLibrary.settings.value
                    PlaybackController.setVolume(restored.volume)
                    PlaybackController.setPlaybackSpeed(restored.playbackSpeed)
                    PlaybackController.applyAudioFocus()
                }
                Toast.makeText(context, "恢复成功", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { pendingRestore = null },
        backdrop = backdrop,
    )

    HyperSheet(visible = showBackupSheet, onDismiss = { showBackupSheet = false }) {
        SheetHeader("选择备份", standardTitle = true)
        SheetCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                backupFiles.forEach { backup ->
                    BackupEntry(backup = backup) {
                        showBackupSheet = false
                        scope.launch {
                            val result = WebDavApi.downloadBackup(backup.name)
                            result.onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "云端恢复失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            val raw = result.getOrNull() ?: return@launch
                            val payload = withContext(Dispatchers.IO) { BackupStore.parse(raw) }
                            if (payload == null) {
                                Toast.makeText(context, "云端备份文件无法读取", Toast.LENGTH_SHORT).show()
                            } else {
                                pendingRestore = payload
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupEntry(
    backup: WebDavApi.CloudBackup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Image(
                imageVector = MiuixIcons.Glass.Backup,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = backup.timeLabel,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = backup.name,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun errorTitleColors() = BasicComponentColors(
    color = MiuixTheme.colorScheme.error,
    disabledColor = MiuixTheme.colorScheme.error.copy(alpha = 0.5f),
)

@Composable
private fun WebDavConfigDialog(
    show: Boolean,
    current: MusicSettings,
    backdrop: Backdrop?,
    onSave: (String, String, String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember(show) { mutableStateOf(if (show) current.webdavUrl else "") }
    var username by remember(show) { mutableStateOf(if (show) current.webdavUsername else "") }
    var password by remember(show) { mutableStateOf(if (show) current.webdavPassword else "") }
    
    var removeArmed by remember(show) { mutableStateOf(false) }
    var connecting by remember(show) { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val urlHandle = rememberNativeTextFieldHandle()
    val userHandle = rememberNativeTextFieldHandle()
    val passHandle = rememberNativeTextFieldHandle()
    val scheme = MiuixTheme.colorScheme

    fun hideIme() {
        urlHandle.hideIme()
        userHandle.hideIme()
        passHandle.hideIme()
        keyboard?.hide()
    }

    fun close() {
        hideIme()
        onDismiss()
    }

    GlassDialog(
        show = show,
        onDismissRequest = { if (!connecting) close() },
        title = "WebDAV 服务器",
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FieldLabel(text = "服务器地址")
            InputBox {
                NativeTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        removeArmed = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "https://example.com/dav",
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    ),
                    handle = urlHandle,
                    onDone = { close() },
                )
            }
            FieldLabel(text = "用户名")
            InputBox {
                NativeTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        removeArmed = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "用户名由服务商指定",
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    ),
                    handle = userHandle,
                    onDone = { close() },
                )
            }
            FieldLabel(text = "密码")
            InputBox {
                NativeTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        removeArmed = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "请填写授权密码",
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    ),
                    handle = passHandle,
                    onDone = { close() },
                    password = true,
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
                    enabled = !connecting,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        close()
                    },
                    modifier = Modifier.weight(1f),
                )
                if (current.webdavUrl.isNotBlank() || url.isNotBlank()) {
                    TextButton(
                        text = "移除",
                        enabled = !connecting,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (!removeArmed) {
                                removeArmed = true
                                Toast.makeText(context, "再次点击清空内容", Toast.LENGTH_SHORT).show()
                            } else {
                                removeArmed = false
                                hideIme()
                                url = ""
                                username = ""
                                password = ""

                                onRemove()
                                Toast.makeText(context, "已清空服务器信息", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            color = scheme.secondaryVariant,
                            disabledColor = scheme.disabledSecondaryVariant,
                            textColor = scheme.error,
                            disabledTextColor = scheme.error.copy(alpha = 0.5f),
                        ),
                    )
                }
                TextButton(
                    text = if (connecting) "连接中" else "确定",
                    enabled = !connecting,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        if (url.isBlank()) {
                            Toast.makeText(context, "请先填写服务器地址", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        removeArmed = false
                        hideIme()
                        connecting = true
                        scope.launch {
                            val result = WebDavApi.checkConnection(url, username, password)
                            connecting = false
                            result.fold(
                                onSuccess = {
                                    onSave(url.trim(), username, password)
                                    Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "连接失败：${error.message ?: "请检查服务器信息"}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
    )
}

@Composable
private fun InputBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) { content() }
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
