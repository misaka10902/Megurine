package moe.megurine.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import moe.megurine.music.music.LocalSongs
import moe.megurine.music.music.Song
import moe.megurine.music.music.WebDavApi
import moe.megurine.music.ui.components.ConfirmDialog
import moe.megurine.music.ui.components.SelectionAction
import moe.megurine.music.ui.components.SelectionBarState
import moe.megurine.music.ui.components.TrackRow
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.rememberPopupBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import moe.megurine.music.ui.glass.GlassContextBridge
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme


private val TabRowBottomGap = 6.dp

private val TopBarTitleGap = 12.dp

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onBatchAddToPlaylist: (List<Song>) -> Unit = {},
    onBatchAddToQueue: (List<Song>) -> Unit = {},
    onRequestDelete: (List<Song>) -> Unit = {},
    onCloudDelete: (List<Song>) -> Unit = {},
    refreshSignal: Int = 0,
) {
    val context = LocalContext.current
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val advanced = LocalAdvancedMaterialEnabled.current

    var tab by remember { mutableIntStateOf(0) }
    var localSongs by remember { mutableStateOf(emptyList<Song>()) }
    var cloudSongs by remember { mutableStateOf(emptyList<Song>()) }
    var localLoading by remember { mutableStateOf(true) }
    var cloudLoading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var cloudSignal by remember { mutableIntStateOf(0) }
    val localRevision by LocalSongs.revision.collectAsState()


    val pagerState = rememberPagerState(pageCount = { 2 })
    val localListState = rememberLazyListState()
    val cloudListState = rememberLazyListState()
    val pageStates = remember(localListState, cloudListState) {
        listOf(localListState, cloudListState)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (tab != page) tab = page
        }
    }

    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, readPermission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(readPermission)
        }
    }

    LaunchedEffect(refreshKey, refreshSignal, localRevision) {
        localLoading = true
        localSongs = LocalSongs.scan(context)
        localLoading = false
        if (tab == 0) refreshing = false
    }

    LaunchedEffect(cloudSignal, WebDavApi.configured()) {
        cloudLoading = true
        val result = WebDavApi.listSongs()
        val loaded = result.getOrNull()
        if (loaded != null) {
            cloudSongs = loaded
        } else {
            cloudSongs = emptyList()
            if (cloudSignal > 0) {
                Toast.makeText(
                    context,
                    result.exceptionOrNull()?.message ?: "云端歌曲读取失败",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        cloudLoading = false
        if (tab == 1) refreshing = false

        if (loaded != null) WebDavApi.enrichSongs(loaded) { cloudSongs = it }
    }

    val songs = if (tab == 0) localSongs else cloudSongs

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIds, songs, tab) {
        val picked = songs.filter { it.id in selectedIds }
        if (picked.isEmpty()) {
            onSelectionChange(null)
        } else {
            onSelectionChange(
                SelectionBarState(
                    count = picked.size,
                    actions = listOf(
                        SelectionAction(
                            label = "加入歌单",
                            icon = MiuixIcons.Playlist,
                        ) { onBatchAddToPlaylist(picked) },
                        SelectionAction(
                            label = "加入队列",
                            icon = MiuixIcons.ListView,
                        ) {
                            onBatchAddToQueue(picked)
                            selectedIds = emptySet()
                        },
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                        ) { confirmDelete = true },
                    ),
                    onExit = { selectedIds = emptySet() },
                ),
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    ConfirmDialog(
        show = confirmDelete,
        title = "删除歌曲",
        summary = if (tab == 0) {
            "确认从下载文件夹中删除 ${selectedIds.size} 首歌曲吗？"
        } else {
            "确认从云端删除 ${selectedIds.size} 首歌曲吗？"
        },
        confirmText = "删除",
        onConfirm = {
            val picked = songs.filter { it.id in selectedIds }
            selectedIds = emptySet()
            confirmDelete = false
            if (tab == 0) onRequestDelete(picked) else onCloudDelete(picked)
        },
        onDismiss = { confirmDelete = false },
        backdrop = rememberPopupBackdrop(),
    )

    DetailPage(
        title = "音乐库",
        onBack = onBack,
        backdrop = backdrop,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { selectedIds = emptySet() },
        listState = localListState,
        pagerState = pagerState,
        pagerStates = pageStates,
        header = {},
        topBarBottomContent = {

            @Suppress("UNCHECKED_CAST")
            val barContext = GlassContextBridge.localGlassTopAppBarContext()
                    as ProvidableCompositionLocal<Any?>
            CompositionLocalProvider(barContext provides null) {
                GlassTabRow(
                    tabs = listOf("本地", "云端"),
                    selectedIndex = tab,
                    onSelect = { index ->
                        if (index != tab) {
                            tab = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    },
                    backdrop = if (advanced) backdrop else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                        .padding(top = TopBarTitleGap)
                        .padding(bottom = TabRowBottomGap),
                    colors = GlassTabRowDefaults.primaryColors(),
                )
            }
        },
        pullRefresh = PullRefreshSpec(
            isRefreshing = refreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                refreshing = true
                if (tab == 0) refreshKey++ else cloudSignal++
            },

            topInset = TabRowBottomGap,
        ),
        pagerList = { page ->
            libraryPage(
                page = page,
                songs = if (page == 0) localSongs else cloudSongs,
                loading = if (page == 0) localLoading else cloudLoading,
                multiSelect = multiSelect,
                selectedIds = selectedIds,
                onToggleSelect = { song ->
                    selectedIds = if (song.id in selectedIds) {
                        selectedIds - song.id
                    } else {
                        selectedIds + song.id
                    }
                },
                onPlay = onPlay,
                onMore = onMore,
                onLongPress = { song ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedIds = selectedIds + song.id
                },
            )
        },
        overlay = {},
        pagerPageOverlay = { page ->
            val pageSongs = if (page == 0) localSongs else cloudSongs
            val pageLoading = if (page == 0) localLoading else cloudLoading

            if (pageSongs.isEmpty() && !pageLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = if (page == 0) MiuixIcons.FileDownloads else MiuixIcons.CloudFill,
                        text = if (page == 0) "没有本地歌曲" else "云端还没有歌曲",
                    )
                }
            }
        },
    )
}

private fun LazyListScope.libraryPage(
    page: Int,
    songs: List<Song>,
    loading: Boolean,
    multiSelect: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (Song) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onLongPress: (Song) -> Unit,
) {
    item(key = "library-tabs-space-$page") {
        Box(Modifier.fillMaxWidth().padding(top = TabRowBottomGap))
    }
    if (songs.isNotEmpty()) {
        itemsIndexed(songs, key = { _, song -> "$page-${song.id}" }) { index, song ->
            TrackRow(
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(200),
                    placementSpec = spring(dampingRatio = 1f, stiffness = 320f),
                    fadeOutSpec = tween(260),
                ),
                song = song,
                onClick = {
                    if (multiSelect) onToggleSelect(song) else onPlay(songs, index)
                },
                onMore = { onMore(song) },
                selectionMode = multiSelect,
                showLocalBadge = false,
                showCloudBadge = false,
                selected = song.id in selectedIds,
                onLongPress = { onLongPress(song) },
            )
        }
    }
}
