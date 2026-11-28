package moe.megurine.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.LocalRailContentInset
import moe.megurine.music.music.LocalRailObstructionStart
import moe.megurine.music.music.NetCover
import moe.megurine.music.music.Song
import moe.megurine.music.music.WallpaperBackground
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.components.EmptyHint
import moe.megurine.music.ui.components.RefreshIndicator
import moe.megurine.music.ui.components.SongPairRow
import moe.megurine.music.ui.components.TrackRow
import moe.megurine.music.ui.glass.GlassContextBridge
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.utils.songColumns
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
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val ArtistHeaderGap = 12.dp
private val ArtistAvatarSize = 61.dp

private val ArtistAvatarBlockExtent = ArtistAvatarSize + 14.dp
private val ArtistAlbumColumns = 3
private val ArtistBottomReserve = 76.dp

@Composable
fun ArtistScreen(
    artistName: String,
    artistId: String = "",
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
) {
    val columns = songColumns()
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var artistPic by remember { mutableStateOf("") }
    var resolvedId by remember { mutableStateOf(artistId) }
    var songOffset by remember { mutableIntStateOf(0) }
    var songTotal by remember { mutableIntStateOf(0) }
    var loadingMoreSongs by remember { mutableStateOf(false) }
    var hasMoreSongs by remember { mutableStateOf(true) }

    var albums by remember { mutableStateOf<List<StreamApi.ArtistAlbum>>(emptyList()) }
    var albumOffset by remember { mutableIntStateOf(0) }
    var albumLoading by remember { mutableStateOf(true) }
    var loadingMoreAlbums by remember { mutableStateOf(false) }
    var hasMoreAlbums by remember { mutableStateOf(true) }


    var albumTotal by remember { mutableIntStateOf(0) }
    var infoBrief by remember { mutableStateOf("") }
    var infoSections by remember { mutableStateOf<List<StreamApi.ArtistInfoSection>>(emptyList()) }
    var infoLoading by remember { mutableStateOf(false) }
    var infoLoaded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val songListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val infoListState = rememberLazyListState()
    val pageStates = remember(songListState, albumListState, infoListState) {
        listOf(songListState, albumListState, infoListState)
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
        albumLoading = true
        songs = emptyList()
        albums = emptyList()
        songOffset = 0
        albumOffset = 0
        hasMoreSongs = true
        hasMoreAlbums = true

        val info = runCatching { StreamApi.artistInfo(artistId, artistName) }.getOrNull()
        val id = info?.id.orEmpty().ifBlank { artistId }
        resolvedId = id
        artistPic = info?.avatar.orEmpty()
        val hot = info?.hotSongs.orEmpty()
        songs = hot
        songOffset = hot.size
        songTotal = info?.songCount ?: hot.size
        albumTotal = info?.albumCount ?: 0
        if (infoBrief.isBlank()) infoBrief = info?.brief.orEmpty()

        hasMoreSongs = if (songTotal > 0) songTotal > hot.size else hot.size >= 40
        loading = false

        if (id.isBlank()) {
            albumLoading = false
            hasMoreAlbums = false
            return@LaunchedEffect
        }
        val (firstAlbums, albumMore) = runCatching {
            StreamApi.artistAlbums(id, 0, 30)
        }.getOrDefault(emptyList<StreamApi.ArtistAlbum>() to false)
        albums = firstAlbums
        albumOffset = firstAlbums.size
        hasMoreAlbums = albumMore
        albumLoading = false
    }

    fun loadMoreSongs() {
        if (loadingMoreSongs || !hasMoreSongs || resolvedId.isBlank()) return
        scope.launch {
            loadingMoreSongs = true
            val (more, next) = runCatching {
                StreamApi.artistSongs(resolvedId, songOffset, 100)
            }.getOrDefault(emptyList<Song>() to false)
            val fresh = more.filter { s -> songs.none { it.id == s.id } }
            if (fresh.isNotEmpty()) {
                songs = songs + fresh
                songOffset += more.size
            }
            hasMoreSongs = next && fresh.isNotEmpty()
            loadingMoreSongs = false
        }
    }

    fun loadMoreAlbums() {
        if (loadingMoreAlbums || !hasMoreAlbums || resolvedId.isBlank()) return
        scope.launch {
            loadingMoreAlbums = true
            val (more, next) = runCatching {
                StreamApi.artistAlbums(resolvedId, albumOffset, 30)
            }.getOrDefault(emptyList<StreamApi.ArtistAlbum>() to false)
            val fresh = more.filter { a -> albums.none { it.id == a.id } }
            if (fresh.isNotEmpty()) {
                albums = albums + fresh
                albumOffset += more.size
            }
            hasMoreAlbums = next && fresh.isNotEmpty()
            loadingMoreAlbums = false
        }
    }


    LaunchedEffect(pagerState.currentPage, resolvedId) {
        if (pagerState.currentPage != 2) return@LaunchedEffect
        if (infoLoaded || resolvedId.isBlank()) return@LaunchedEffect
        infoLoading = true
        val intro = runCatching { StreamApi.artistIntro(resolvedId) }.getOrNull()
        if (intro != null) {
            if (intro.brief.isNotBlank()) infoBrief = intro.brief
            infoSections = intro.sections
        }
        infoLoaded = true
        infoLoading = false
    }


    LaunchedEffect(songListState, songs.size, hasMoreSongs, loadingMoreSongs) {
        snapshotFlow { songListState.layoutInfo }
            .collect { info ->
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                if (songs.isNotEmpty() && hasMoreSongs && !loadingMoreSongs &&
                    last >= info.totalItemsCount - 3
                ) {
                    loadMoreSongs()
                }
            }
    }
    LaunchedEffect(albumListState, albums.size, hasMoreAlbums, loadingMoreAlbums) {
        snapshotFlow { albumListState.layoutInfo }
            .collect { info ->
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                if (hasMoreAlbums && !loadingMoreAlbums && last >= info.totalItemsCount - 3) {
                    loadMoreAlbums()
                }
            }
    }

    val cover = remember(artistPic, songs) {
        when {
            artistPic.isNotBlank() -> artistPic
            else -> songs.firstOrNull()?.picId
                ?.let { StreamApi.picUrl(it, CoverSize.MEDIUM.request) } ?: ""
        }.ifBlank { null }
    }


    var barHeightPx by remember { mutableIntStateOf(0) }
    val barHeight = with(density) { barHeightPx.toDp() }

    val collapseDistancePx = with(density) { ArtistAvatarBlockExtent.toPx() }

    val scrolled by remember(pagerState, pageStates) {
        derivedStateOf {
            val state = pageStates.getOrNull(pagerState.currentPage) ?: return@derivedStateOf 0f
            val first = state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
            (-first.offset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    BackHandler(onBack = onBack)


    val railInset = LocalRailContentInset.current
    val contentStart = railInset + WindowInsets.displayCutout.asPaddingValues()
        .calculateStartPadding(LayoutDirection.Ltr)
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
                modifier = Modifier.fillMaxSize().padding(start = contentStart),
                beyondViewportPageCount = 1,
            ) { tab ->
                LazyColumn(
                    state = pageStates[tab],
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                    contentPadding = PaddingValues(
                        top = barHeight + ArtistHeaderGap,
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues().calculateBottomPadding() + ArtistBottomReserve,
                    ),
                ) {
                    if (!loading) {
                        if (tab == 0) {
                            songsContent(
                                songs = songs,
                                columns = columns,
                                alpha = { index -> itemAlpha(index) },
                                onPlay = onPlay,
                                onMore = onMore,
                                loadingMore = loadingMoreSongs,
                            )
                        } else if (tab == 1) {
                            albumsContent(
                                albums = albums,
                                loading = albumLoading,
                                columns = ArtistAlbumColumns,
                                alpha = { index -> itemAlpha(index) },
                                onOpenAlbum = onOpenAlbum,
                                loadingMore = loadingMoreAlbums,
                            )
                        } else {
                            infoContent(
                                songCount = songTotal,
                                albumCount = albumTotal,
                                brief = infoBrief,
                                sections = infoSections,
                                loading = infoLoading,
                            )
                        }
                    }
                }
            }
        }

        ArtistTopBar(
            artistName = artistName,
            songCount = songTotal,
            cover = cover,
            selectedTab = pagerState.currentPage,
            onSelectTab = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            onBack = onBack,
            backdrop = backdrop,
            collapse = scrolled,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onSizeChanged { if (it.height > barHeightPx) barHeightPx = it.height },
        )

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(240)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = barHeight),
        ) {
            RefreshIndicator(text = "正在刷新")
        }
    }
}

@Composable
private fun ArtistTopBar(
    artistName: String,
    songCount: Int,
    cover: String?,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    collapse: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val advanced = LocalAdvancedMaterialEnabled.current
    val progress = collapse.coerceIn(0f, 1f)
    val scrolled = progress > 0.001f

    val avatarAlpha = (1f - progress / 0.5f).coerceIn(0f, 1f)

    GlassTopAppBar(
        title = artistName,
        modifier = modifier,
        contentModifier = Modifier.padding(start = LocalRailContentInset.current),
        isContentScrolled = scrolled,
        largeTitle = "",
        expandable = false,
        progressiveBlur = advanced,
        backdrop = backdrop,
        titleAlpha = { progress },

        centerObstructionStart = LocalRailObstructionStart.current() ?: Dp.Unspecified,
        navigationIcon = {
            GlassIconButton(
                onClick = onBack,

                shadow = if (scrolled) GlassShadows.Regular else null,
            ) {
                Icon(
                    imageVector = MiuixIcons.Glass.ChevronBackward,
                    contentDescription = "返回",
                    modifier = Modifier.size(25.dp),
                    tint = scheme.onSurface,
                )
            }
        },
        bottomContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ArtistAvatarBlockExtent * (1f - progress))
                        .clipToBounds(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(ArtistAvatarSize)
                            .graphicsLayer { alpha = avatarAlpha }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artistName,
                                color = scheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (songCount > 0) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${songCount}首歌曲",
                                    color = scheme.onSurfaceSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }


                @Suppress("UNCHECKED_CAST")
                val barContext = GlassContextBridge.localGlassTopAppBarContext()
                        as ProvidableCompositionLocal<Any?>
                CompositionLocalProvider(barContext provides null) {
                    GlassTabRow(
                        tabs = listOf("歌曲", "专辑", "百科"),
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
        },
    )
}

private fun LazyListScope.songsContent(
    songs: List<Song>,
    columns: Int,
    alpha: (Int) -> Float,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    loadingMore: Boolean,
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

                showSourceTag = false,
                contentPadding = if (columns == 2) {
                    PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                } else {
                    PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                },
                modifier = Modifier.graphicsLayer { this.alpha = a },
            )
        }
    }
    if (loadingMore) {
        item(key = "loading-more-songs") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                RefreshIndicator(text = "正在加载", size = 20.dp, strokeWidth = 2.dp)
            }
        }
    }
}

private fun LazyListScope.albumsContent(
    albums: List<StreamApi.ArtistAlbum>,
    loading: Boolean,
    columns: Int,
    alpha: (Int) -> Float,
    onOpenAlbum: (String, String) -> Unit,
    loadingMore: Boolean,
) {
    if (albums.isEmpty()) {
        if (!loading) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { this.alpha = alpha(0) },
                ) {
                    EmptyHint("没有专辑", icon = MiuixIcons.Album)
                }
            }
        }
        return
    }
    itemsIndexed(
        albums.chunked(columns),
        key = { _, row -> "album-row-${row.first().id}" },
    ) { rowIndex, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = if (rowIndex == 0) 4.dp else 18.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEachIndexed { columnIndex, album ->
                val index = rowIndex * columns + columnIndex
                AlbumGridCell(
                    album = album,
                    onClick = { onOpenAlbum(album.name, album.id) },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { this.alpha = alpha(index) },
                )
            }
            repeat(columns - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
    if (loadingMore) {
        item(key = "loading-more-albums") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                RefreshIndicator(text = "正在加载", size = 20.dp, strokeWidth = 2.dp)
            }
        }
    }
}

private fun LazyListScope.infoContent(
    songCount: Int,
    albumCount: Int,
    brief: String,
    sections: List<StreamApi.ArtistInfoSection>,
    loading: Boolean,
) {

    if (brief.isBlank() && sections.isEmpty()) {
        if (!loading) {
            item(key = "artist-info-empty") {
                EmptyHint(
                    text = "无歌手信息",
                    icon = MiuixIcons.ContactsCircle,
                )
            }
        }
        return
    }
    item(key = "artist-info-stats") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ArtistStat(label = "歌曲", value = if (songCount > 0) "$songCount" else "—")
            ArtistStat(label = "专辑", value = if (albumCount > 0) "$albumCount" else "—")
        }
    }
    if (brief.isNotBlank()) {
        item(key = "artist-info-brief") {
            Text(
                text = brief,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp),
            )
        }
    }

    sections.forEachIndexed { index, section ->
        item(key = "artist-info-section-$index") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (index == 0 && brief.isBlank()) 18.dp else 22.dp,
                    ),
            ) {
                if (section.title.isNotBlank()) {
                    Text(
                        text = section.title,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = section.text,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun ArtistStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AlbumGridCell(
    album: StreamApi.ArtistAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        NetCover(
            url = StreamApi.sizedCover(album.cover, CoverSize.SMALL.request).ifBlank { null },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            corner = 22.dp,
            maxSize = CoverSize.SMALL.decode,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
