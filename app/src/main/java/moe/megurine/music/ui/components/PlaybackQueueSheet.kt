package moe.megurine.music.ui.components

import android.widget.Toast
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.ListenMoeController
import moe.megurine.music.music.NetCover
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.music.Song
import moe.megurine.music.music.StreamApi
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Redo
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.glass.OpenWithOtherApp
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val QueueArtworkSize = 48.dp
private val QueueRowHorizontalEndPadding = 12.dp
private val QueueRowIconSize = 24.dp

@Composable
fun PlaybackQueueSheet(
    visible: Boolean,
    onDismiss: () -> Unit,

    backdrop: Backdrop? = null,
) {
    val state by PlaybackController.state.collectAsState()
    val radio by ListenMoeController.state.collectAsState()

    val uiState = state
    val radioState = radio
    val queueIds = uiState.queue.map { it.id }
    val radioIds = radioState.queue.map { it.id }
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val radioListState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }
    var switchingId by remember { mutableStateOf<String?>(null) }

    val listMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.75f

    LaunchedEffect(visible, radioState.active, uiState.queue.size) {
        if (visible && !radioState.active && uiState.index in uiState.queue.indices) {
            listState.scrollToItem(uiState.index)
        }
    }

    val subtitle = when {
        radioState.active -> "LISTEN.moe · ${radioState.station.label}"
        uiState.queue.isNotEmpty() -> "${uiState.index + 1} / ${uiState.queue.size}"
        else -> ""
    }

    HyperSheet(
        visible = visible,
        onDismiss = onDismiss,
        backdrop = backdrop,
        insideMargin = androidx.compose.ui.unit.DpSize(0.dp, 0.dp),
        immersive = true,

        progressiveBlur = true,

        scrollOffset = {
            when {
                radioState.active -> when {
                    radioState.queue.isEmpty() -> 0f
                    radioListState.firstVisibleItemIndex > 0 -> Float.MAX_VALUE
                    else -> radioListState.firstVisibleItemScrollOffset.toFloat()
                }
                uiState.queue.isEmpty() -> 0f
                listState.firstVisibleItemIndex > 0 -> Float.MAX_VALUE
                else -> listState.firstVisibleItemScrollOffset.toFloat()
            }
        },

        title = "播放队列",
        startAction = {
            QueueGlassAction(
                icon = MiuixIcons.Close,
                description = "关闭",
                onClick = onDismiss,
                modifier = Modifier.padding(start = 12.dp),
            )
        },
        endAction = {
            if (!radioState.active) {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QueueGlassAction(
                        icon = MiuixIcons.Location,
                        description = "定位当前歌曲",
                        enabled = uiState.index in uiState.queue.indices,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch { listState.animateScrollToItem(uiState.index) }
                        },
                    )
                    QueueGlassAction(
                        icon = MiuixIcons.Delete,
                        description = "清空队列",
                        enabled = uiState.queue.isNotEmpty(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            confirmClear = true
                        },
                    )
                }
            }
        },
        headerBottomContent = {
            if (subtitle.isNotEmpty()) {
                Box(

                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-17).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subtitle,
                        color = scheme.onSurfaceSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { innerPadding ->

        val listTopPadding = innerPadding.calculateTopPadding()
        Column(modifier = Modifier.fillMaxWidth()) {
            if (radioState.active) {
                if (radioState.queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = listTopPadding)
                            .height(listMaxHeight),
                    ) {
                        EmptyHint(
                            text = if (radioState.loading) "正在获取播放队列" else "队列还是空的",
                            icon = MiuixIcons.Playlist,
                            centered = true,
                        )
                    }
                } else {
                    LazyColumn(
                        state = radioListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(listMaxHeight + listTopPadding),

                        contentPadding = PaddingValues(
                            top = listTopPadding + QueueListVerticalPadding,
                            bottom = QueueListVerticalPadding,
                        ),
                    ) {
                        itemsIndexed(
                            radioState.queue,
                            key = { index, item -> queueSlotKeyAt(radioIds, index, item.id) },
                        ) { _, item ->
                            val isCurrent = item.id == radioState.song?.id
                            QueueRow(
                                song = item.toSong(),
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(200),
                                    placementSpec = spring(dampingRatio = 1f, stiffness = 320f),
                                    fadeOutSpec = tween(260),
                                ),
                                isCurrent = isCurrent,
                                moveAction = null,
                                showRemove = false,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onDismiss()
                                },
                                onRemove = {},
                                onSwitchOnline = if (isCurrent) null else {
                                    {
                                        if (switchingId == null) {
                                            switchingId = item.id
                                            scope.launch {
                                                val match = runCatching {
                                                    ListenMoeController.findNeteaseMatch(item)
                                                }.getOrNull()
                                                switchingId = null
                                                if (match != null) {
                                                    onDismiss()
                                                    PlaybackController.playSongs(listOf(match), 0, 0L)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "未找到同名歌曲",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            } else if (uiState.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = listTopPadding)
                        .height(listMaxHeight),
                ) {
                    EmptyHint(
                        text = "队列还是空的",
                        icon = MiuixIcons.Playlist,
                        centered = true,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listMaxHeight + listTopPadding),
                    contentPadding = PaddingValues(
                        top = listTopPadding + QueueListVerticalPadding,
                        bottom = QueueListVerticalPadding,
                    ),
                ) {
                    itemsIndexed(
                        uiState.queue,
                        key = { index, song -> queueSlotKeyAt(queueIds, index, song.id) },
                    ) { index, song ->
                        val isCurrent = index == uiState.index
                        QueueRow(
                            song = song,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(200),
                                placementSpec = spring(dampingRatio = 1f, stiffness = 320f),
                                fadeOutSpec = tween(260),
                            ),
                            isCurrent = isCurrent,
                            moveAction = when {
                                isCurrent -> null
                                index > uiState.index -> QueueMoveAction.Up
                                else -> QueueMoveAction.Down
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                PlaybackController.playAt(index)
                            },
                            onMove = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val target = if (index > uiState.index) index - 1 else index + 1
                                PlaybackController.moveInQueue(index, target)
                            },
                            onRemove = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                PlaybackController.removeFromQueue(song.id)
                            },
                        )
                    }
                }
            }
        }
    }

    ConfirmDialog(
        show = confirmClear,
        title = "清空播放列表",
        summary = "确定要清空播放列表吗？此操作会终止当前播放的歌曲",
        confirmText = "清空",
        onConfirm = {
            confirmClear = false
            PlaybackController.clearQueue()
            onDismiss()
        },
        onDismiss = { confirmClear = false },
    )
}

@Composable
private fun QueueGlassAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassIconButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) {
                MiuixTheme.colorScheme.onSurface
            } else {
                MiuixTheme.colorScheme.disabledOnSecondaryVariant
            },
        )
    }
}

private val QueueListVerticalPadding = 4.dp

private enum class QueueMoveAction { Up, Down }

private fun queueSlotKeyAt(ids: List<String>, index: Int, id: String): String {
    var seen = 0
    for (i in 0 until index.coerceAtMost(ids.size)) {
        if (ids[i] == id) seen++
    }
    return "$id#$seen"
}

@Composable
private fun QueueRow(
    song: Song,
    modifier: Modifier = Modifier,
    isCurrent: Boolean,
    moveAction: QueueMoveAction?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMove: () -> Unit = {},
    showRemove: Boolean = true,
    onSwitchOnline: (() -> Unit)? = null,
) {
    val scheme = MiuixTheme.colorScheme

    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        holdDownState = isCurrent,
        startAction = {
            Box(modifier = Modifier.padding(end = 12.dp)) {
                NetCover(
                    url = StreamApi.coverUrl(song, CoverSize.SMALL.request).ifBlank { null },
                    modifier = Modifier.size(QueueArtworkSize),
                    corner = 100.dp,
                    maxSize = CoverSize.SMALL.decode,
                    contentDescription = song.name,
                )
            }
        },
        endActions = {
            if (onSwitchOnline != null) {
                QueueRowIcon(
                    icon = MiuixIcons.Glass.OpenWithOtherApp,
                    description = "切换至内置音源",
                    onClick = onSwitchOnline,
                )
            }
            if (moveAction != null) {
                QueueRowIcon(
                    icon = if (moveAction == QueueMoveAction.Up) MiuixIcons.Undo else MiuixIcons.Redo,
                    description = if (moveAction == QueueMoveAction.Up) {
                        "上移播放位置"
                    } else {
                        "下移播放位置"
                    },
                    onClick = onMove,
                )
            }
            if (showRemove) {
                QueueRowIcon(
                    icon = MiuixIcons.Close,
                    description = "从播放列表移除",
                    onClick = onRemove,
                )
            }
        },
        insideMargin = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = QueueRowHorizontalEndPadding,
            bottom = 8.dp,
        ),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = song.name,
                color = scheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artistText,
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QueueRowIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(QueueRowIconSize),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.85f)),
        )
    }
}
