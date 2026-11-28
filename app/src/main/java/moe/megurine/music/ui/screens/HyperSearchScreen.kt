package moe.megurine.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import moe.megurine.music.music.StreamApi.SearchAlbum
import moe.megurine.music.music.StreamApi.SearchArtist
import moe.megurine.music.music.StreamApi.SearchPlaylist
import moe.megurine.music.music.SearchHistoryStore
import moe.megurine.music.music.Song
import moe.megurine.music.music.WallpaperBackground
import moe.megurine.music.music.WallpaperManager
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.components.ConfirmDialog
import moe.megurine.music.ui.components.GlassHeaderScrim
import moe.megurine.music.ui.components.RefreshIndicator
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.rememberPopupBackdrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun ColumnScope.HyperSearchScreen(
    query: String,
    onQueryText: (String) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenArtist: (String, String) -> Unit,
    onOpenAlbum: (String, String, String) -> Unit,
    onOpenPlaylist: (SearchPlaylist) -> Unit,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchedKeyword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var pendingKeyword by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var artists by remember { mutableStateOf<List<SearchArtist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<SearchAlbum>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<SearchPlaylist>>(emptyList()) }
    var addingPlaylistIds by remember { mutableStateOf(setOf<String>()) }
    var category by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var noMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var playlistPage by remember { mutableIntStateOf(1) }
    var playlistsNoMore by remember { mutableStateOf(false) }
    var loadingMorePlaylists by remember { mutableStateOf(false) }
    val searchSeq = remember { AtomicInteger(0) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val songListState = rememberLazyListState()
    val artistListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()
    val listStates = remember(songListState, artistListState, albumListState, playlistListState) {
        listOf(songListState, artistListState, albumListState, playlistListState)
    }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val searched = searchedKeyword.isNotBlank()
    val searchHistory by SearchHistoryStore.items.collectAsState()
    val wallpaperState by WallpaperManager.state.collectAsState()

    val density = LocalDensity.current
    val searchBarBottom = with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 52.dp

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var clearingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { SearchHistoryStore.init(context) }

    suspend fun runThreeWaySearch(keyword: String) {
        val kw = keyword.trim()
        if (kw.isBlank()) return
        val my = searchSeq.incrementAndGet()
        pendingKeyword = kw
        loading = true
        try {
            val sJob = scope.async {
                runCatching {

                    StreamApi.searchSongs(kw, 30, 1) { partial ->
                        if (my == searchSeq.get()) songs = partial
                    }
                }.getOrElse { emptyList() }
            }
            val aJob = scope.async { runCatching { StreamApi.searchArtists(kw, 30) }.getOrElse { emptyList() } }
            val alJob = scope.async { runCatching { StreamApi.searchAlbums(kw, 30) }.getOrElse { emptyList() } }
            val pJob = scope.async { runCatching { StreamApi.searchPlaylists(kw, 30) }.getOrElse { emptyList() } }
            val sList = sJob.await()
            if (my != searchSeq.get()) return

            songs = sList
            page = 1
            noMore = sList.size < 30
            if (kw != searchedKeyword) category = 0
            searchedKeyword = kw
            SearchHistoryStore.add(kw)

            val aList = aJob.await()
            if (my != searchSeq.get()) return
            artists = aList
            val alList = alJob.await()
            if (my != searchSeq.get()) return
            albums = alList
            val pList = pJob.await()
            if (my != searchSeq.get()) return
            playlists = pList
            playlistPage = 1
            playlistsNoMore = pList.size < 30
            loadingMorePlaylists = false
        } finally {

            if (my == searchSeq.get()) {
                loading = false
                pendingKeyword = ""
            }
        }
    }

    fun loadMoreSongs() {
        if (loadingMore || loading || noMore || searchedKeyword.isBlank()) return
        loadingMore = true
        scope.launch {
            val next = runCatching { StreamApi.searchSongs(searchedKeyword, 30, page + 1) }
                .getOrElse { emptyList() }
            val fresh = next.filter { n -> songs.none { it.id == n.id } }
            if (next.isEmpty() || fresh.isEmpty()) {
                noMore = true
            } else {
                songs = songs + fresh
                page += 1
            }
            loadingMore = false
        }
    }

    fun loadMorePlaylists() {
        if (loadingMorePlaylists || loading || playlistsNoMore || searchedKeyword.isBlank()) return
        loadingMorePlaylists = true
        scope.launch {
            val next = runCatching {
                StreamApi.searchPlaylists(searchedKeyword, 30, playlistPage * 30)
            }.getOrElse { emptyList() }
            val fresh = next.filter { n -> playlists.none { it.id == n.id } }
            if (fresh.isEmpty()) {
                playlistsNoMore = true
            } else {
                playlists = playlists + fresh
                playlistPage += 1
            }
            loadingMorePlaylists = false
        }
    }

    LaunchedEffect(query) {
        val kw = query.trim()
        if (kw.isBlank()) {
            searchJob?.cancel()
            songs = emptyList()
            artists = emptyList()
            albums = emptyList()
            playlists = emptyList()
            searchedKeyword = ""
            pendingKeyword = ""
            loading = false
            page = 1
            noMore = false
            playlistPage = 1
            playlistsNoMore = false
            loadingMorePlaylists = false
            return@LaunchedEffect
        }
        if (kw == searchedKeyword || kw == pendingKeyword) return@LaunchedEffect
        loading = true
        delay(400)
        if (query.trim() != kw) return@LaunchedEffect
        if (kw == searchedKeyword || kw == pendingKeyword) return@LaunchedEffect
        searchJob?.cancel()
        searchJob = scope.launch { runThreeWaySearch(kw) }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { category = it }
    }

    LaunchedEffect(songListState, songs.size) {
        snapshotFlow { songListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (searched && songs.isNotEmpty() && last >= songs.size - 6) {
                    loadMoreSongs()
                }
            }
    }

    LaunchedEffect(playlistListState, playlists.size) {
        snapshotFlow { playlistListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (searched && playlists.isNotEmpty() && last >= playlists.size - 4) {
                    loadMorePlaylists()
                }
            }
    }

    LaunchedEffect(active) {
        if (!active) {
            searchJob?.cancel()
            delay(520)
            searchedKeyword = ""
            pendingKeyword = ""
            songs = emptyList()
            artists = emptyList()
            albums = emptyList()
            playlists = emptyList()
            addingPlaylistIds = emptySet()
            category = 0
            page = 1
            noMore = false
            loadingMore = false
            playlistPage = 1
            playlistsNoMore = false
            loadingMorePlaylists = false
            loading = false
        }
    }

    DisposableEffect(Unit) { onDispose { searchJob?.cancel() } }

    val addPlaylistToLibrary: (SearchPlaylist) -> Unit = { p ->
        if (p.id !in addingPlaylistIds) {
            addingPlaylistIds = addingPlaylistIds + p.id
            scope.launch {
                val ok = addOnlinePlaylistToLibrary(p.id, p.name, p.cover)
                addingPlaylistIds = addingPlaylistIds - p.id
                Toast.makeText(
                    context,
                    if (ok) "已添加该歌单" else "添加失败，请重试",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val stage = when {
        loading -> SearchStage.Loading
        searched -> SearchStage.Results
        else -> SearchStage.History
    }
    AnimatedContent(
        targetState = stage,
        modifier = modifier.fillMaxWidth().weight(1f),
        transitionSpec = {
            fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(160))
        },
        label = "searchStage",
    ) { current ->
        when (current) {
        SearchStage.History -> {

            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                SearchHistorySection(
                    history = searchHistory,
                    topPadding = searchBarBottom + 12.dp,
                    clearing = clearingHistory,
                    onClearFinished = {
                        SearchHistoryStore.clear()
                        clearingHistory = false
                    },
                    onPick = { keyword ->
                        onQueryText(keyword)
                        val kw = keyword.trim()
                        if (kw.isNotBlank() && kw != searchedKeyword) {
                            pendingKeyword = kw
                            loading = true
                            searchJob?.cancel()
                            searchJob = scope.launch { runThreeWaySearch(kw) }
                        }
                    },
                    onClear = { showClearHistoryDialog = true },
                )
            }
        }
        SearchStage.Loading -> Box(modifier = Modifier.fillMaxSize().imePadding()) { SearchLoadingView() }
        SearchStage.Results -> {
            val advancedHeader = LocalAdvancedMaterialEnabled.current

            val tabTop = searchBarBottom + 12.dp
            val listTop = tabTop + GlassTabRowDefaults.Height + 12.dp

            val scrimHeight = tabTop + GlassTabRowDefaults.Height + 24.dp
            val headerBackdrop = rememberLayerBackdrop { drawContent() }

            val rampPx = with(density) { 48.dp.toPx() }
            val scrolled by remember(pagerState, listStates) {
                derivedStateOf {
                    val state = listStates.getOrNull(pagerState.currentPage)
                    when {
                        state == null -> 0f
                        state.firstVisibleItemIndex > 0 -> 1f
                        else -> (state.firstVisibleItemScrollOffset / rampPx).coerceIn(0f, 1f)
                    }
                }
            }
            val headerStrength by animateFloatAsState(
                targetValue = scrolled,
                animationSpec = tween(200),
                label = "searchHeaderBand",
            )

            Box(modifier = Modifier.fillMaxSize()) {

                LaunchedEffect(searchedKeyword) {
                    if (searchedKeyword.isNotBlank()) {
                        if (pagerState.currentPage != 0) pagerState.scrollToPage(0)

                        listStates.forEach { it.scrollToItem(0) }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(headerBackdrop),
                ) {

                    if (wallpaperState.hasImage) {
                        WallpaperBackground()
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MiuixTheme.colorScheme.surface),
                        )
                    }
                    HorizontalPager(
                        state = pagerState,

                        modifier = Modifier.fillMaxSize().imePadding(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        SearchResults(
                            category = page,
                            songs = songs,
                            artists = artists,
                            albums = albums,
                            playlists = playlists,
                            loading = loading,
                            listStates = listStates,
                            topPadding = listTop,
                            onPlaySongs = onPlaySongs,
                            onMore = onMore,
                            onOpenArtist = onOpenArtist,
                            onOpenAlbum = onOpenAlbum,
                            onOpenPlaylist = onOpenPlaylist,
                            onAddPlaylist = addPlaylistToLibrary,
                            addingPlaylistIds = addingPlaylistIds,
                            onLoadMoreSongs = { loadMoreSongs() },
                            loadingMoreSongs = loadingMore,
                            onLoadMorePlaylists = { loadMorePlaylists() },
                            loadingMorePlaylists = loadingMorePlaylists,
                        )
                    }
                }

                GlassHeaderScrim(
                    backdrop = headerBackdrop,
                    advanced = advancedHeader,
                    strength = headerStrength,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrimHeight),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = tabTop)
                        .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                        .zIndex(2f),
                ) {
                    GlassTabRow(
                        tabs = listOf("歌曲", "歌手", "专辑", "歌单"),
                        selectedIndex = pagerState.currentPage,
                        onSelect = { index ->

                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        backdrop = if (advancedHeader) headerBackdrop else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = GlassTabRowDefaults.primaryColors(),
                    )
                }
            }
        }
        }
    }

    ConfirmDialog(
        show = showClearHistoryDialog,
        title = "清除搜索历史",
        summary = "确认清空所有搜索历史吗？",
        confirmText = "清空",
        onConfirm = {
            showClearHistoryDialog = false
            clearingHistory = true
        },
        onDismiss = { showClearHistoryDialog = false },
        backdrop = rememberPopupBackdrop(),
    )
}

private enum class SearchStage { History, Loading, Results }

@Composable
private fun SearchLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        RefreshIndicator(text = "正在搜索", size = 22.dp, strokeWidth = 2.dp)
    }
}

