package com.hyper.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.NetCover
import com.hyper.music.music.Song
import com.hyper.music.music.WallpaperBackground
import com.hyper.music.ui.components.EmptyHint
import com.hyper.music.ui.components.GlassHeaderScrim
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.components.SongPairRow
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.songColumns
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val ArtistHeaderGap = 12.dp
private val ArtistAvatarSize = 67.dp
private val ArtistBottomReserve = 76.dp

@Composable
fun ArtistScreen(
    artistName: String,
    artistId: String = "",
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val columns = songColumns()
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val advanced = LocalAdvancedMaterialEnabled.current

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var artistPic by remember { mutableStateOf("") }
    var resolvedId by remember { mutableStateOf(artistId) }
    var page by remember { mutableIntStateOf(1) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val songListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val pageStates = remember(songListState, albumListState) {
        listOf(songListState, albumListState)
    }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(loading) {
        if (!loading) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    fun itemAlpha(index: Int): Float =
        ((reveal.value - (index * 0.015f).coerceAtMost(0.12f)) / 0.7f).coerceIn(0f, 1f)

    LaunchedEffect(artistName, artistId) {
        loading = true
        page = 1
        hasMore = true
        val id = if (artistId.isNotBlank()) artistId else GdApi.resolveArtistId(artistName)
        resolvedId = id
        launch {
            artistPic = runCatching { GdApi.artistAvatar(artistName, id) }.getOrDefault("")
        }
        songs = GdApi.artistSongsById(id, artistName, 30, 1)
        loading = false
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val next = page + 1
            val more = runCatching { GdApi.artistSongsById(resolvedId, artistName, 30, next) }
                .getOrDefault(emptyList())
            val fresh = more.filter { s -> songs.none { it.id == s.id } }
            if (fresh.isNotEmpty()) {
                songs = songs + fresh
                page = next
            }
            hasMore = fresh.isNotEmpty()
            loadingMore = false
        }
    }

    val albums = remember(songs) {
        songs.groupBy { it.album }.filterKeys { it.isNotBlank() }
    }
    val cover = remember(artistPic, songs) {
        when {
            artistPic.isNotBlank() -> artistPic
            else -> songs.firstOrNull()?.picId
                ?.let { GdApi.picUrl(it, CoverSize.MEDIUM.request) } ?: ""
        }.ifBlank { null }
    }

    val statusBarPx = WindowInsets.statusBars.getTop(density)
    var headerHeightPx by remember {
        mutableIntStateOf(statusBarPx + with(density) { 188.dp.toPx() }.toInt())
    }
    val headerHeight = with(density) { headerHeightPx.toDp() }
    val rampPx = with(density) { 48.dp.toPx() }

    val scrolled by remember(pagerState, pageStates) {
        derivedStateOf {
            val state = pageStates.getOrNull(pagerState.currentPage)
            when {
                state == null -> 0f
                state.firstVisibleItemIndex > 0 -> 1f
                else -> (state.firstVisibleItemScrollOffset / rampPx).coerceIn(0f, 1f)
            }
        }
    }
    val blurStrength by animateFloatAsState(
        targetValue = scrolled,
        animationSpec = tween(200),
        label = "artistHeaderBlur",
    )

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scheme.surface),
            )
            WallpaperBackground()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { tab ->
                LazyColumn(
                    state = pageStates[tab],
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                    contentPadding = PaddingValues(
                        top = headerHeight + ArtistHeaderGap,
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues().calculateBottomPadding() + ArtistBottomReserve,
                    ),
                ) {
                    if (!loading) {
                        if (tab == 0) {
                            songsContent(
                                songs = songs,
                                columns = columns,
                                hasMore = hasMore,
                                loadingMore = loadingMore,
                                alpha = { index -> itemAlpha(index) },
                                onPlay = onPlay,
                                onMore = onMore,
                                onLoadMore = { loadMore() },
                            )
                        } else {
                            albumsContent(
                                albums = albums,
                                alpha = { index -> itemAlpha(index) },
                                onOpenAlbum = onOpenAlbum,
                                hasMore = hasMore,
                                loadingMore = loadingMore,
                                onLoadMore = { loadMore() },
                            )
                        }
                    }
                }
            }
        }

        GlassHeaderScrim(
            backdrop = backdrop,
            advanced = advanced,
            strength = blurStrength,
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight + 16.dp)
                .align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                    }
                },
        )

        ArtistFixedHeader(
            artistName = artistName,
            cover = cover,
            selectedTab = pagerState.currentPage,
            onSelectTab = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            onBack = onBack,
            backdrop = backdrop,
            scrolled = scrolled > 0.001f,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
        )

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(240)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = headerHeight),
        ) {
            RefreshIndicator(text = "正在刷新")
        }
    }
}

@Composable
private fun ArtistFixedHeader(
    artistName: String,
    cover: String?,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val advanced = LocalAdvancedMaterialEnabled.current
    Column(modifier = modifier) {
        GlassTopAppBar(
            title = "",
            isContentScrolled = scrolled,
            largeTitle = "",
            expandable = false,
            backdrop = backdrop,
            bandBrush = SolidColor(Color.Transparent),
            titleAlpha = { 0f },
            navigationIcon = {
                GlassIconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Glass.ChevronBackward,
                        contentDescription = "返回",
                        modifier = Modifier.size(25.dp),
                        tint = scheme.onSurface,
                    )
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetCover(
                url = cover,
                modifier = Modifier
                    .size(ArtistAvatarSize)
                    .clip(CircleShape),
                corner = 100.dp,
                maxSize = CoverSize.MEDIUM.decode,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = artistName,
                color = scheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(14.dp))

        GlassTabRow(
            tabs = listOf("歌曲", "专辑"),
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            backdrop = if (advanced) backdrop else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding),
            colors = GlassTabRowDefaults.primaryColors(),
        )
    }
}

private fun LazyListScope.songsContent(
    songs: List<Song>,
    columns: Int,
    hasMore: Boolean,
    loadingMore: Boolean,
    alpha: (Int) -> Float,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onLoadMore: () -> Unit,
) {
    if (songs.isEmpty()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.alpha = alpha(0) },
            ) {
                EmptyHint("暂无歌曲")
            }
        }
    }
    itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
        SongPairRow(
            pair = group,
            startIndex = groupIndex * columns,
            columns = columns,
        ) { index, song ->
            val a = alpha(index)
            TrackRow(
                song = song,
                onClick = { onPlay(songs, index) },
                onMore = { onMore(song) },
                contentPadding = if (columns == 2) {
                    PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                } else {
                    PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                },
                modifier = Modifier.graphicsLayer { this.alpha = a },
            )
        }
    }
    if (hasMore) {
        item {
            LoadMoreButton(
                loading = loadingMore,
                onClick = onLoadMore,
                modifier = Modifier.graphicsLayer { this.alpha = alpha(songs.size) },
            )
        }
    }
}

private fun LazyListScope.albumsContent(
    albums: Map<String, List<Song>>,
    alpha: (Int) -> Float,
    onOpenAlbum: (String) -> Unit,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    if (albums.isEmpty()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.alpha = alpha(0) },
            ) {
                EmptyHint("暂无专辑")
            }
        }
    }
    albums.entries.forEachIndexed { index, entry ->
        val album = entry.key
        val albumSongs = entry.value
        item(key = "album-$album") {
            val a = alpha(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenAlbum(album) }
                    .padding(start = 28.dp, top = 8.dp, end = 52.dp, bottom = 8.dp)
                    .graphicsLayer { this.alpha = a },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NetCover(
                    url = GdApi.picUrl(
                        albumSongs.first().picId,
                        CoverSize.SMALL.request,
                    ).ifBlank { null },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    corner = 22.dp,
                    maxSize = CoverSize.SMALL.decode,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${albumSongs.size} 首",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
    if (hasMore) {
        item {
            LoadMoreButton(
                loading = loadingMore,
                onClick = onLoadMore,
                modifier = Modifier.graphicsLayer { this.alpha = alpha(albums.size) },
            )
        }
    }
}
