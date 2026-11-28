package moe.megurine.music.ui.screens
import android.widget.Toast
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.NetCover
import moe.megurine.music.music.PlaylistCoverStore
import moe.megurine.music.music.Song
import moe.megurine.music.music.WallpaperManager
import moe.megurine.music.music.WallpaperBackground
import moe.megurine.music.music.LocalFixedWallpaperBackdrop
import moe.megurine.music.music.LocalIsWideScreen
import moe.megurine.music.music.LocalRailContentInset
import moe.megurine.music.music.LocalRailObstructionStart
import moe.megurine.music.music.fixedWallpaperRegion
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.components.ConfirmDialog
import moe.megurine.music.ui.components.GlassMenuEntry
import moe.megurine.music.ui.components.GlassMenuPopup
import moe.megurine.music.ui.effects.miuix.LocalActiveBackdrop
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.rememberPopupBackdrop
import moe.megurine.music.ui.effects.miuix.cardGlass
import moe.megurine.music.ui.effects.miuix.cardGlassColors
import moe.megurine.music.ui.effects.miuix.rememberSquircleShape
import moe.megurine.music.ui.components.EmptyHint
import moe.megurine.music.ui.components.RefreshIndicator
import moe.megurine.music.ui.components.ReorderGeom
import moe.megurine.music.ui.components.SelectionAction
import moe.megurine.music.ui.components.SelectionBarState
import moe.megurine.music.ui.components.SongPairRow
import moe.megurine.music.ui.components.TrackRow
import moe.megurine.music.ui.components.DragReorderState
import moe.megurine.music.ui.components.dragReorderItem
import moe.megurine.music.ui.components.dragReorderSurface
import moe.megurine.music.ui.components.rememberDragReorderState
import moe.megurine.music.ui.components.rememberPlaylistCoverPick
import top.yukonga.miuix.kmp.utils.overScrollVertical
import moe.megurine.music.ui.utils.songColumns
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.icon.glass.Music1
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.Cutout
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.glass.More
import top.yukonga.miuix.kmp.icon.glass.PasteEditPoint
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.glass.Play
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer

private val DetailBottomReserve = 76.dp

internal class PullRefreshSpec(
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
    val texts: List<String> = listOf("下拉刷新", "松手刷新", "正在刷新", "刷新成功"),
    
    val topInset: Dp = 0.dp,
)

private fun DragReorderState.placementSpec() =
    if (isDragging || settling || suppressPlacement) {
        null
    } else {
        spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    }

private inline fun interpolateOverPages(
    pagerState: PagerState,
    lastIndex: Int,
    valueAt: (Int) -> Float,
): Float {
    if (lastIndex <= 0) return valueAt(0)
    val p = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, lastIndex.toFloat())
    val lo = floor(p).toInt().coerceIn(0, lastIndex)
    val hi = (lo + 1).coerceAtMost(lastIndex)
    val t = (p - lo).coerceIn(0f, 1f)
    val start = valueAt(lo)
    return if (t <= 0f) start else start + (valueAt(hi) - start) * t
}

@Composable
internal fun DetailPage(
    title: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    header: @Composable () -> Unit,
    background: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    compactTitle: Boolean = false,

    autoHideTitle: Boolean = false,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),

    pagerState: PagerState? = null,
    pagerStates: List<LazyListState> = emptyList(),
    pagerList: (LazyListScope.(Int) -> Unit)? = null,
    
    pagerPageOverlay: (@Composable (Int) -> Unit)? = null,

    hideWallpaper: Boolean = false,
    activeBackdrop: LayerBackdrop? = null,

    listModifier: Modifier = Modifier,
    endAction: (@Composable () -> Unit)? = null,
    endActionKey: Any? = null,
    endActionWidth: Dp = 44.dp,
    multiSelect: Boolean = false,

    selectionCount: Int = 0,
    onExitMultiSelect: (() -> Unit)? = null,
    
    topBarBottomContent: @Composable () -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    pullRefresh: PullRefreshSpec? = null,
    list: LazyListScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current


    val isWideScreen = LocalIsWideScreen.current
    val railObstructionStart = LocalRailObstructionStart.current()
    val fixedWallpaperBackdrop = LocalFixedWallpaperBackdrop.current

    val cutoutStart = WindowInsets.displayCutout.asPaddingValues()
        .calculateStartPadding(LayoutDirection.Ltr)

    val contentStartInset = LocalRailContentInset.current + cutoutStart

    val wallpaperBackdrop = rememberLayerBackdrop()

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    val pagerHeaderScroll = remember(pagerState, pagerStates) {
        if (pagerState == null || pagerStates.isEmpty()) {
            null
        } else {
            derivedStateOf {
                val height = headerHeightPx.toFloat()
                interpolateOverPages(pagerState, pagerStates.lastIndex) { index ->
                    val state = pagerStates.getOrNull(index) ?: return@interpolateOverPages 0f
                    val first = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    val raw = if (first == null || first.index > 0) {
                        height
                    } else {
                        -first.offset.toFloat()
                    }
                    raw.coerceIn(0f, height)
                }
            }
        }
    }

    val collapsedTitleAlpha = remember(listState, autoHideTitle, multiSelect, pagerState, pagerStates, pagerHeaderScroll) {
        derivedStateOf {
            val total = headerHeightPx.coerceAtLeast(1).toFloat()
            fun progressOf(state: LazyListState?): Float {
                if (state == null) return 0f
                val scrolled = if (state.firstVisibleItemIndex > 0) {
                    total
                } else {
                    state.firstVisibleItemScrollOffset.toFloat()
                }
                return (scrolled / (total * 0.7f)).coerceIn(0f, 1f)
            }
            when {
                !autoHideTitle || multiSelect -> 1f
                pagerHeaderScroll != null ->
                    (pagerHeaderScroll.value / (total * 0.8f)).coerceIn(0f, 1f)
                else -> progressOf(listState)
            }
        }
    }

    val pagerHeaderAlpha = remember(pagerHeaderScroll) {
        if (pagerHeaderScroll == null) {
            null
        } else {
            derivedStateOf {
                val total = headerHeightPx.coerceAtLeast(1).toFloat()
                val t = (pagerHeaderScroll.value / total - 0.62f) / 0.24f
                (1f - t).coerceIn(0f, 1f)
            }
        }
    }

    val barTitle = if (multiSelect && selectionCount > 0) {
        "已选择 $selectionCount 项"
    } else {
        title
    }
    var shownTitle by remember { mutableStateOf(barTitle) }
    val titleSwap = remember { Animatable(1f) }
    var prevSelecting by remember { mutableStateOf(multiSelect) }
    LaunchedEffect(barTitle, multiSelect) {
        val selectionChanged = multiSelect != prevSelecting
        prevSelecting = multiSelect
        if (shownTitle == barTitle) return@LaunchedEffect
        if (selectionChanged && multiSelect) {

            titleSwap.snapTo(0f)
            shownTitle = barTitle
            titleSwap.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        } else if (selectionChanged) {
            titleSwap.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            shownTitle = barTitle
            titleSwap.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        } else {
            shownTitle = barTitle
        }
    }

    Scaffold(

        modifier = modifier.fillMaxSize(),
        containerColor = if (WallpaperManager.state.collectAsState().value.hasImage) {
            Color.Transparent
        } else {
            MiuixTheme.colorScheme.surface
        },
        topBar = {
            GlassTopAppBar(
                title = shownTitle,
                largeTitle = shownTitle,

                contentModifier = Modifier.padding(start = LocalRailContentInset.current),

                subtitle = if (isWideScreen) "" else subtitle.orEmpty(),

                expandable = !compactTitle && !isWideScreen,
                backdrop = backdrop,
                progressiveBlur = advancedMaterial,
                scrollBehavior = scrollBehavior,

                titleAlpha = {
                    titleSwap.value * if (isWideScreen) 1f else collapsedTitleAlpha.value
                },

                centerObstructionStart = railObstructionStart ?: Dp.Unspecified,
                navigationIcon = {

                    androidx.compose.animation.AnimatedContent(
                        targetState = multiSelect,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "detailNavIcon",
                    ) { selecting ->
                        GlassIconButton(
                            onClick = {
                                if (selecting && onExitMultiSelect != null) onExitMultiSelect() else onBack()
                            },
                        ) {
                            Icon(
                                imageVector = if (selecting) MiuixIcons.Close else MiuixIcons.Glass.ChevronBackward,
                                contentDescription = if (selecting) "退出多选" else "返回",
                                modifier = Modifier.size(if (selecting) 22.dp else 25.dp).offset(x = 1.5.dp, y = 0.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                actions = {

                    val hasAction = endAction != null
                    AnimatedContent(
                        targetState = endActionKey ?: title,
                        modifier = if (hasAction) Modifier.width(endActionWidth) else Modifier,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(190)) togetherWith fadeOut(animationSpec = tween(140))
                        },
                        contentAlignment = Alignment.Center,
                        label = "detailEndAction",
                    ) { endAction?.invoke() }
                },
                bottomContent = topBarBottomContent,
            )
        },
    ) { contentPadding ->
        val topBarHeightDp = contentPadding.calculateTopPadding()
        CompositionLocalProvider(LocalActiveBackdrop provides (activeBackdrop ?: wallpaperBackdrop)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()

                        .layerBackdrop(backdrop),
                ) {

                    if (!hideWallpaper && fixedWallpaperBackdrop != null) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(wallpaperBackdrop)
                                .fixedWallpaperRegion(fixedWallpaperBackdrop),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MiuixTheme.colorScheme.surface),
                        )
                        if (!hideWallpaper) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(wallpaperBackdrop),
                            ) {
                                WallpaperBackground()
                            }
                        }
                    }
                    Box(Modifier.matchParentSize()) { background() }
                val listBody: @Composable () -> Unit = {
                    val pg = pagerState
                    val pages = pagerList
                    if (pg != null && pages != null) {

                        Box(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pg,
                                modifier = Modifier.fillMaxSize(),

                                beyondViewportPageCount = 1,
                                userScrollEnabled = !multiSelect,
                            ) { page ->
                                val pageState = pagerStates.getOrNull(page) ?: listState
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = pageState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior.state.overscrollOffset = offset })
                                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                                            .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                                        contentPadding = PaddingValues(
                                            bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                                                .asPaddingValues().calculateBottomPadding() + DetailBottomReserve,
                                        ),
                                    ) {

                                        item(key = "detail-header-space") {
                                            Spacer(Modifier.height(topBarHeightDp + headerHeightDp))
                                        }
                                        pages(this, page)
                                    }
                                    if (pagerPageOverlay != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    top = topBarHeightDp + headerHeightDp,
                                                    bottom = 96.dp,
                                                ),
                                        ) {
                                            pagerPageOverlay.invoke(page)
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = topBarHeightDp)
                                    .graphicsLayer {
                                        translationY = -(pagerHeaderScroll?.value ?: 0f)
                                        alpha = pagerHeaderAlpha?.value ?: 1f
                                    },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { headerHeightPx = it.height },
                                ) {
                                    header()
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior.state.overscrollOffset = offset })
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                            contentPadding = PaddingValues(
                                top = topBarHeightDp,
                                bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                                    .asPaddingValues().calculateBottomPadding() + DetailBottomReserve,
                            ),
                        ) {
                            item {
                                Box(Modifier.fillMaxWidth().onSizeChanged { headerHeightPx = it.height }) {
                                    header()
                                }
                            }
                            list()
                        }
                    }
                }

                val refresh = pullRefresh
                if (refresh != null) {

                    PullToRefresh(
                        isRefreshing = refresh.isRefreshing,
                        onRefresh = refresh.onRefresh,
                        contentPadding = PaddingValues(top = topBarHeightDp + refresh.topInset),
                        refreshTexts = refresh.texts,
                        modifier = Modifier.fillMaxSize().padding(start = contentStartInset),
                    ) {
                        Box(Modifier.fillMaxSize().then(listModifier)) {
                            listBody()
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize().padding(start = contentStartInset).then(listModifier)) {
                        listBody()
                    }
                }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = contentStartInset)
                        .padding(top = topBarHeightDp + headerHeightDp, bottom = 96.dp),
                ) {
                    overlay()
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song, Boolean) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onExport: (() -> Unit)? = null,
) {
    val playlists by MusicLibrary.playlists.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val songs = playlist?.songs.orEmpty()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val pickCover = rememberPlaylistCoverPick()
    val cover = remember(playlist?.cover, songs) {
        playlist?.cover?.ifBlank { null }
            ?: songs.firstOrNull()?.let { StreamApi.coverUrl(it, CoverSize.SMALL.request) }?.ifBlank { null }
    }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }
    var confirmDelete by remember { mutableStateOf(false) }
    val allSelected = songs.isNotEmpty() && songs.all { it.id in selectedIds }
    fun toggleSelectAll() {
        selectedIds = if (allSelected) emptySet() else songs.map { it.id }.toSet()
    }
    LaunchedEffect(selectedIds, songs) {
        val picked = songs.filter { it.id in selectedIds }
        if (picked.isEmpty()) {
            onSelectionChange(null)
        } else {
            onSelectionChange(
                SelectionBarState(
                    count = picked.size,
                    actions = listOf(
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                            enabled = picked.isNotEmpty(),
                        ) { confirmDelete = true },
                    ),
                    selectAllAction = SelectionAction(
                        label = if (allSelected) "取消全选" else "全选",
                        icon = MiuixIcons.SelectAll,
                        tint = if (allSelected) scheme.primary else Color.Unspecified,
                    ) { toggleSelectAll() },
                    onExit = { selectedIds = emptySet() },
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState { from, to ->
        MusicLibrary.reorderPlaylistSongs(playlistId, from, to)
    }

    val detailDensity = LocalDensity.current
    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect
        val edge = with(detailDensity) { 96.dp.toPx() }
        val maxStep = with(detailDensity) { 16.dp.toPx() }
        while (isActive) {
            withFrameNanos { }
            val info = listState.layoutInfo
            val top = info.viewportStartOffset.toFloat()
            val bottom = info.viewportEndOffset.toFloat()
            val p = dragState.pointer.y
            val step = when {
                p < top + edge -> -maxStep * ((top + edge - p) / edge).coerceIn(0.15f, 1.6f)
                p > bottom - edge -> maxStep * ((p - (bottom - edge)) / edge).coerceIn(0.15f, 1.6f)
                else -> 0f
            }
            if (step != 0f) {
                listState.scrollBy(step)
                dragState.onViewportScrolled()
            }
        }
    }

    fun songGeoms(): List<ReorderGeom> = listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
        val dataIndex = info.index - 1
        if (dataIndex < 0) null else ReorderGeom(
            dataIndex,
            0f,
            info.offset.toFloat(),
            0f,
            info.size.toFloat(),
        )
    }

    var confirmRemoveCover by remember { mutableStateOf(false) }
    val hasCustomCover = !playlist?.cover.isNullOrBlank()

    val menuAnchor = rememberGlassPopupAnchor()
    var menuExpanded by remember { mutableStateOf(false) }

    val topEndAction: (@Composable () -> Unit)? = if (multiSelect) {
        {
            GlassIconButton(
                onClick = { toggleSelectAll() },
            ) {
                Icon(
                    imageVector = MiuixIcons.SelectAll,
                    contentDescription = if (allSelected) "取消全选" else "全选",
                    modifier = Modifier.size(22.dp),
                    tint = (if (allSelected) scheme.primary else Color.Unspecified).takeOrElse { MiuixTheme.colorScheme.onSurface },
                )
            }
        }
    } else if (songs.isNotEmpty()) {
        {
            GlassIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    menuExpanded = !menuExpanded
                },
                modifier = Modifier.glassPopupAnchor(
                    anchor = menuAnchor,
                    cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                    floating = true,
                ),
            ) {
                Icon(
                    imageVector = MiuixIcons.Glass.More,
                    contentDescription = "更多操作",
                    modifier = Modifier.size(23.dp).glassPopupAnchorContent(menuAnchor),
                    tint = scheme.onSurface,
                )
            }
        }
    } else {
        null
    }

    val endActionWidth = 44.dp

    DetailPage(
        title = if (multiSelect) "已选择 ${selectedIds.size} 项" else playlist?.name ?: "歌单",
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        autoHideTitle = true,
        listState = listState,

        listModifier = Modifier.dragReorderSurface(dragState, multiSelect) { songGeoms() },
        endAction = topEndAction,
        endActionKey = multiSelect,
        endActionWidth = endActionWidth,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { selectedIds = emptySet() },
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pickCover(playlistId)
                        },
                    ),
                ) {
                    if (cover == null) {
                        EmptyCoverCard(
                            icon = MiuixIcons.Glass.Item,
                            iconAlpha = 0.35f,
                        )
                    } else {
                        NetCover(
                            url = cover,
                            modifier = Modifier.size(96.dp),
                            corner = 22.dp,
                            maxSize = CoverSize.SMALL.decode,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = playlist?.name ?: "歌单",
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${songs.size} 首歌曲",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = MiuixIcons.Glass.Music1,
                        text = "歌单还没有歌曲",
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                TrackRow(
                    song = song,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(200),
                            placementSpec = dragState.placementSpec(),
                            fadeOutSpec = tween(260),
                        )
                        .dragReorderItem(
                            id = song.id,
                            index = index,
                            state = dragState,
                            enabled = multiSelect,
                            snapshot = { songGeoms() },
                        ),
                    onClick = {
                        if (multiSelect) {
                            selectedIds = if (song.id in selectedIds) {
                                selectedIds - song.id
                            } else {
                                selectedIds + song.id
                            }
                        } else {
                            onPlay(songs, index)
                        }
                    },
                    onMore = { onMore(song, true) },
                    selectionMode = multiSelect,
                    selected = song.id in selectedIds,
                    contentPadding = PaddingValues(
                        start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp,
                    ),
                    onLongPress = if (multiSelect) null else {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIds = selectedIds + song.id
                        }
                    },
                )
            }
        }
    }

    GlassMenuPopup(
        expanded = menuExpanded,
        anchor = menuAnchor,
        anchorContent = {
            Icon(
                imageVector = MiuixIcons.Glass.More,
                contentDescription = null,
                modifier = Modifier.size(23.dp),
                tint = scheme.onSurface,
            )
        },
        backdrop = backdrop,
        onDismiss = { menuExpanded = false },
        items = buildList {
            add(
                GlassMenuEntry(label = "播放全部", icon = MiuixIcons.Glass.Play) {
                    if (songs.isNotEmpty()) onPlay(songs, 0)
                },
            )
            if (onExport != null) {
                add(
                    GlassMenuEntry(label = "导出歌单", icon = MiuixIcons.Glass.PasteEditPoint) {
                        onExport()
                    },
                )
            }
            if (hasCustomCover) {
                add(
                    GlassMenuEntry(label = "清除封面", icon = MiuixIcons.Glass.Cutout) {
                        confirmRemoveCover = true
                    },
                )
            }
        },
    )

    ConfirmDialog(
        show = confirmDelete,
        title = "删除歌曲",
        summary = "是否删除这些歌曲？",
        onConfirm = {
            confirmDelete = false
            val targets = songs.filter { it.id in selectedIds }
            targets.forEach { MusicLibrary.removeSongFromPlaylist(playlistId, it.id) }
            selectedIds = emptySet()
        },
        onDismiss = { confirmDelete = false },
        backdrop = rememberPopupBackdrop(),
    )

    ConfirmDialog(
        show = confirmRemoveCover,
        title = "删除歌单封面",
        summary = "确认移除歌单封面吗？移除后歌单封面将显示第一首歌曲的专辑封面",
        confirmText = "确定",
        onConfirm = {
            confirmRemoveCover = false
            val old = MusicLibrary.playlist(playlistId)?.cover.orEmpty()
            MusicLibrary.setPlaylistCover(playlistId, "")
            PlaylistCoverStore.delete(old)
            Toast.makeText(context, "已移除歌单封面", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { confirmRemoveCover = false },
        backdrop = rememberPopupBackdrop(),
    )
}

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
) {
    val songs by MusicLibrary.favorites.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }

    var confirmUnfavorite by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIds, songs) {
        val picked = songs.filter { it.id in selectedIds }
        if (picked.isEmpty()) {
            onSelectionChange(null)
        } else {
            onSelectionChange(
                SelectionBarState(
                    count = picked.size,
                    actions = listOf(
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                        ) { confirmUnfavorite = true },
                    ),
                    onExit = { selectedIds = emptySet() },
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState { from, to ->
        MusicLibrary.reorderFavorites(from, to)
    }

    val detailDensity = LocalDensity.current
    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect
        val edge = with(detailDensity) { 96.dp.toPx() }
        val maxStep = with(detailDensity) { 16.dp.toPx() }
        while (isActive) {
            withFrameNanos { }
            val info = listState.layoutInfo
            val top = info.viewportStartOffset.toFloat()
            val bottom = info.viewportEndOffset.toFloat()
            val p = dragState.pointer.y
            val step = when {
                p < top + edge -> -maxStep * ((top + edge - p) / edge).coerceIn(0.15f, 1.6f)
                p > bottom - edge -> maxStep * ((p - (bottom - edge)) / edge).coerceIn(0.15f, 1.6f)
                else -> 0f
            }
            if (step != 0f) {
                listState.scrollBy(step)
                dragState.onViewportScrolled()
            }
        }
    }
    fun songGeoms(): List<ReorderGeom> = listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
        val dataIndex = info.index - 1
        if (dataIndex < 0) null else ReorderGeom(
            dataIndex,
            0f,
            info.offset.toFloat(),
            0f,
            info.size.toFloat(),
        )
    }

    DetailPage(
        title = "我喜欢的歌曲",
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        autoHideTitle = true,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { selectedIds = emptySet() },
        listState = listState,

        listModifier = Modifier.dragReorderSurface(dragState, multiSelect) { songGeoms() },
        endAction = {
            GlassIconButton(
                onClick = {
                    if (songs.isEmpty()) return@GlassIconButton
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPlay(songs, 0)
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Play,
                    contentDescription = "播放全部",
                    modifier = Modifier.size(23.dp),
                    tint = scheme.onSurface,
                )
            }
        },
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyCoverCard(
                    icon = MiuixIcons.FavoritesFill,
                    iconAlpha = 0.55f,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "我喜欢的歌曲",
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${songs.size} 首歌曲",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        if (songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = MiuixIcons.FavoritesFill,
                        text = "暂无收藏的歌曲",
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                TrackRow(
                    song = song,
                    modifier = Modifier

                        .animateItem(
                            fadeInSpec = tween(200),
                            placementSpec = dragState.placementSpec(),
                            fadeOutSpec = tween(260),
                        )
                        .dragReorderItem(
                            id = song.id,
                            index = index,
                            state = dragState,
                            enabled = multiSelect,
                            snapshot = { songGeoms() },
                        ),
                    onClick = {
                        if (multiSelect) {
                            selectedIds = if (song.id in selectedIds) {
                                selectedIds - song.id
                            } else {
                                selectedIds + song.id
                            }
                        } else {
                            onPlay(songs, index)
                        }
                    },
                    onMore = { onMore(song) },
                    selectionMode = multiSelect,
                    selected = song.id in selectedIds,
                    onLongPress = if (multiSelect) null else {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIds = selectedIds + song.id
                        }
                    },
                )
            }
        }
    }

    ConfirmDialog(
        show = confirmUnfavorite,
        title = "取消收藏",
        summary = "是否取消收藏这些歌曲？",
        confirmText = "取消收藏",
        onConfirm = {
            confirmUnfavorite = false
            val targets = songs.filter { it.id in selectedIds }
            targets.forEach { MusicLibrary.removeFavorite(it.id) }
            selectedIds = emptySet()
        },
        onDismiss = { confirmUnfavorite = false },
        backdrop = rememberPopupBackdrop(),
    )
}

@Composable
fun AlbumScreen(
    albumName: String,
    artistName: String,
    albumId: String = "",
    songId: String = "",
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
) {
    val columns = songColumns()
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var cover by remember { mutableStateOf<String?>(null) }
    var albumTitle by remember { mutableStateOf(albumName) }
    var albumArtist by remember { mutableStateOf(artistName) }
    var publishDate by remember { mutableStateOf("") }
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(loading) {
        if (!loading) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    fun itemAlpha(index: Int): Float =
        ((reveal.value - (index * 0.015f).coerceAtMost(0.12f)) / 0.7f).coerceIn(0f, 1f)


    LaunchedEffect(albumName, artistName, albumId, songId) {
        loading = true
        var id = albumId
        var exactSong: Song? = null
        if (id.isBlank() && songId.isNotBlank()) {
            exactSong = runCatching { StreamApi.songDetail(songId) }.getOrNull()
            id = exactSong?.albumId.orEmpty()
        }
        if (id.isBlank()) {
            id = runCatching { StreamApi.resolveAlbumId(albumName, artistName) }
                .getOrNull()
                .orEmpty()
        }
        val info = if (id.isNotBlank()) {
            runCatching { StreamApi.albumInfo(id) }.getOrNull()
        } else {
            null
        }
        if (info != null) {
            songs = info.songs
            albumTitle = info.name.ifBlank { albumName }
            cover = StreamApi.sizedCover(info.cover, CoverSize.MEDIUM.request).ifBlank { null }
            albumArtist = info.artist
            publishDate = info.publishDate
        } else if (exactSong != null && exactSong.album.isNotBlank()) {

            songs = emptyList()
            albumTitle = exactSong.album
            cover = exactSong.cover.ifBlank { null }
            publishDate = ""
        } else {

            val kw = listOf(albumName, artistName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim()
            val guess = if (kw.isBlank()) {
                emptyList()
            } else {
                runCatching { StreamApi.searchNetease(kw, 50, 1) }.getOrDefault(emptyList())
            }
            val target = StreamApi.normalizeName(albumName)
            val picked = if (target.isEmpty()) {
                emptyList()
            } else {
                guess.filter { StreamApi.normalizeName(it.album) == target }
            }
            songs = picked
            cover = picked.firstOrNull()
                ?.let { StreamApi.coverUrl(it, CoverSize.MEDIUM.request) }
                ?.ifBlank { null }
            publishDate = ""
        }
        loading = false
    }

    val albumArtistLabel = remember(albumArtist, songs) {
        when {
            albumArtist.isNotBlank() -> albumArtist
            else -> {
                val mainArtists = songs.mapNotNull {
                    it.artists.firstOrNull()?.trim()?.takeIf { n -> n.isNotEmpty() }
                }.distinct()
                when {
                    mainArtists.size > 1 -> "Various Artists"
                    mainArtists.size == 1 -> mainArtists.first()
                    else -> "未知专辑艺术家"
                }
            }
        }
    }

    DetailPage(
        title = albumTitle,
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        autoHideTitle = true,
        endAction = {
            GlassIconButton(
                onClick = {
                    if (songs.isEmpty()) return@GlassIconButton
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPlay(songs, 0)
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Play,
                    contentDescription = "播放全部",
                    modifier = Modifier.size(23.dp),
                    tint = scheme.onSurface,
                )
            }
        },
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NetCover(
                    url = cover,
                    modifier = Modifier.size(96.dp),
                    corner = 22.dp,
                    maxSize = CoverSize.MEDIUM.decode,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = albumTitle,
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = albumArtistLabel,
                    color = scheme.onSurfaceSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${songs.size} 首歌曲" + if (publishDate.isNotBlank()) " · $publishDate" else "",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
        },
        overlay = {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(240)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                RefreshIndicator(text = "正在刷新")
            }
        },
    ) {
        if (!loading) when {
            songs.isEmpty() -> item { EmptyHint("暂无歌曲") }
            else -> itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                SongPairRow(
                    pair = group,
                    startIndex = groupIndex * columns,
                    columns = columns,
                ) { index, song ->
                    val a = itemAlpha(index)
                    TrackRow(
                        song = song,
                        onClick = { onPlay(songs, index) },
                        onMore = { onMore(song) },
                        showCover = false,
                        showAlbum = false,
                        contentPadding = if (columns == 2) {
                            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        } else {
                            PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        },
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyCoverCard(
    icon: ImageVector,
    iconAlpha: Float,
) {
    val shape = rememberSquircleShape(22.dp)
    Card(
        cornerRadius = 22.dp,
        modifier = Modifier.size(96.dp).cardGlass(shape),
        insideMargin = PaddingValues(0.dp),
        colors = cardGlassColors(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                colorFilter = ColorFilter.tint(
                    MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = iconAlpha)
                ),
            )
        }
    }
}
