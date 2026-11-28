package moe.megurine.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.LyricLine
import moe.megurine.music.music.NetCover
import moe.megurine.music.ui.utils.saveCoverOnLongPress
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.AfterSalesPolicy
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec

private const val CoverFadeSpecMs = 320
private const val CoverPage = 0
private const val LyricPage = 1
private const val PagerSnapThreshold = 0.15f

@Composable
internal fun PlayerStage(
    state: HyperPlayerState,
    track: PlayerTrack,
    circular: Boolean,
    pageCorner: Dp,
    coverFraction: Float,
    coverScale: Float,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    lines: List<LyricLine>,
    currentIndex: Int,
    lyricsLoading: Boolean = false,
    showTranslation: Boolean,
    lyricCentered: Boolean,
    lyricSwitchOnTap: Boolean,
    allowLyricSwitch: Boolean = true,
    lyricInset: Dp?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentInset: Dp = 0.dp,
    coverOffsetY: Dp = 0.dp,
    onPageProgress: (Float) -> Unit = {},
) {

    Crossfade(
        targetState = lyricSwitchOnTap,
        modifier = modifier,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "lyricSwitchImplementation",
    ) { tapMode ->
        if (tapMode) {
            TapSwitchStage(
                state = state,
                track = track,
                circular = circular,
                pageCorner = pageCorner,
                coverFraction = coverFraction,
                coverScale = coverScale,
                showLyrics = showLyrics,
                onShowLyricsChange = onShowLyricsChange,
                lines = lines,
                currentIndex = currentIndex,
                lyricsLoading = lyricsLoading,
                showTranslation = showTranslation,
                lyricCentered = lyricCentered,
                allowLyricSwitch = allowLyricSwitch,
                lyricInset = lyricInset,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentInset = contentInset,
                coverOffsetY = coverOffsetY,
                onPageProgress = onPageProgress,
            )
        } else {
            SwipeSwitchStage(
                state = state,
                track = track,
                circular = circular,
                pageCorner = pageCorner,
                coverFraction = coverFraction,
                coverScale = coverScale,
                showLyrics = showLyrics,
                onShowLyricsChange = onShowLyricsChange,
                lines = lines,
                currentIndex = currentIndex,
                lyricsLoading = lyricsLoading,
                showTranslation = showTranslation,
                lyricCentered = lyricCentered,
                allowLyricSwitch = allowLyricSwitch,
                lyricInset = lyricInset,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentInset = contentInset,
                coverOffsetY = coverOffsetY,
                onPageProgress = onPageProgress,
            )
        }
    }
}

@Composable
private fun SwipeSwitchStage(
    state: HyperPlayerState,
    track: PlayerTrack,
    circular: Boolean,
    pageCorner: Dp,
    coverFraction: Float,
    coverScale: Float,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    lines: List<LyricLine>,
    currentIndex: Int,
    lyricsLoading: Boolean = false,
    showTranslation: Boolean,
    lyricCentered: Boolean,
    allowLyricSwitch: Boolean,
    lyricInset: Dp?,
    modifier: Modifier,
    alignment: Alignment,
    contentInset: Dp,
    coverOffsetY: Dp,
    onPageProgress: (Float) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = if (showLyrics) LyricPage else CoverPage,
        pageCount = { if (allowLyricSwitch) 2 else 1 },
    )
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
        snapPositionalThreshold = PagerSnapThreshold,
    )

    LaunchedEffect(pagerState.settledPage) {
        val lyrics = pagerState.settledPage == LyricPage
        if (lyrics != showLyrics) onShowLyricsChange(lyrics)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { onPageProgress(it) }
    }

    LaunchedEffect(allowLyricSwitch) {
        if (!allowLyricSwitch && pagerState.currentPage != CoverPage) {
            pagerState.scrollToPage(CoverPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.clipToBounds(),
        flingBehavior = flingBehavior,
        beyondViewportPageCount = 1,
        userScrollEnabled = track.hasSong && allowLyricSwitch,
        key = { it },
    ) { page ->
        if (page == CoverPage) {
            PlayerCoverLayer(
                state = state,
                track = track,
                circular = circular,
                pageCorner = pageCorner,
                coverFraction = coverFraction,
                coverScale = coverScale,
                contentInset = contentInset,
                alignment = alignment,
                offsetY = coverOffsetY,
                onTap = null,
            )
        } else {
            PlayerLyricLayer(
                state = state,
                lines = lines,
                currentIndex = currentIndex,
                lyricsLoading = lyricsLoading,
                showTranslation = showTranslation,
                lyricCentered = lyricCentered,
                lyricInset = lyricInset,
                onDismiss = null,
            )
        }
    }
}

@Composable
private fun TapSwitchStage(
    state: HyperPlayerState,
    track: PlayerTrack,
    circular: Boolean,
    pageCorner: Dp,
    coverFraction: Float,
    coverScale: Float,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    lines: List<LyricLine>,
    currentIndex: Int,
    lyricsLoading: Boolean = false,
    showTranslation: Boolean,
    lyricCentered: Boolean,
    allowLyricSwitch: Boolean,
    lyricInset: Dp?,
    modifier: Modifier,
    alignment: Alignment,
    contentInset: Dp,
    coverOffsetY: Dp,
    onPageProgress: (Float) -> Unit,
) {
    LaunchedEffect(showLyrics) { onPageProgress(if (showLyrics) 1f else 0f) }
    Crossfade(
        targetState = showLyrics,
        animationSpec = tween(durationMillis = CoverFadeSpecMs, easing = FastOutSlowInEasing),
        modifier = modifier.clipToBounds(),
        label = "playerStageFade",
    ) { lyrics ->
        if (lyrics) {
            PlayerLyricLayer(
                state = state,
                lines = lines,
                currentIndex = currentIndex,
                lyricsLoading = lyricsLoading,
                showTranslation = showTranslation,
                lyricCentered = lyricCentered,
                lyricInset = lyricInset,
                onDismiss = { onShowLyricsChange(false) },
            )
        } else {
            PlayerCoverLayer(
                state = state,
                track = track,
                circular = circular,
                pageCorner = pageCorner,
                coverFraction = coverFraction,
                coverScale = coverScale,
                contentInset = contentInset,
                alignment = alignment,
                offsetY = coverOffsetY,
                onTap = if (allowLyricSwitch) ({ onShowLyricsChange(true) }) else null,
            )
        }
    }
}

@Composable
internal fun PlayerCoverLayer(
    state: HyperPlayerState,
    track: PlayerTrack,
    circular: Boolean,
    pageCorner: Dp,
    coverFraction: Float,
    coverScale: Float,
    contentInset: Dp,
    alignment: Alignment,
    offsetY: Dp = 0.dp,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        val contentWidth = (maxWidth - contentInset * 2).coerceAtLeast(0.dp)

        val desiredOffset = offsetY.coerceAtLeast(0.dp)
        val maxSizeForOffset = (maxHeight - desiredOffset * 2).coerceAtLeast(0.dp)
        val coverSize = (minOf(contentWidth, maxHeight) * coverFraction)
            .coerceAtMost(maxSizeForOffset)
            .coerceAtLeast(0.dp)
        val appliedOffset = desiredOffset.coerceAtMost(((maxHeight - coverSize) / 2).coerceAtLeast(0.dp))
        Box(
            modifier = Modifier
                .offset(y = appliedOffset)
                .size(coverSize)
                .coverFrame(circular, pageCorner)
                .then(
                    if (track.hasSong) {
                        Modifier
                    } else {
                        Modifier.background(
                            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            CoverArt(
                track = track,
                pageCorner = pageCorner,
                coverScale = coverScale,
                onTap = onTap,
            )
        }
    }
}

@Composable
internal fun PlayerLyricLayer(
    state: HyperPlayerState,
    lines: List<LyricLine>,
    currentIndex: Int,
    lyricsLoading: Boolean = false,
    showTranslation: Boolean,
    lyricCentered: Boolean,
    lyricInset: Dp?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {

    val hasLyrics = lines.isNotEmpty()
    Box(modifier = modifier.fillMaxSize()) {
        if (hasLyrics) {

            key(lines) {
                HyperLyricsView(
                    lines = lines,
                    currentIndex = currentIndex,
                    showTranslation = showTranslation,
                    centered = lyricCentered,
                    onSeek = { state.seekTo(it) },
                    modifier = Modifier.fillMaxSize(),
                    horizontalInset = lyricInset,
                    onDismiss = onDismiss,
                )
            }
        } else {
            NoLyricsHint(onBack = onDismiss, loading = lyricsLoading)
        }
    }
}

@Composable
private fun CoverArt(
    track: PlayerTrack,
    pageCorner: Dp,
    coverScale: Float,
    onTap: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = coverScale
                scaleY = coverScale
            }
            .saveCoverOnLongPress(
                url = track.coverUrl,
                name = track.title,
                onTap = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (track.hasSong) {
            Crossfade(
                targetState = track.coverUrl ?: "",
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "albumCrossfade",
            ) { url ->
                NetCover(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    corner = pageCorner,
                    contentDescription = track.title,
                    maxSize = CoverSize.LARGE.decode,
                )
            }
        }
    }
}

@Composable
private fun NoLyricsHint(onBack: (() -> Unit)? = null, loading: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onBack != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            imageVector = MiuixIcons.Glass.AfterSalesPolicy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            colorFilter = ColorFilter.tint(PlayerPagePalette.secondary.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (loading) "加载歌词中..." else "没有歌词",
            color = PlayerPagePalette.secondary,
            fontSize = 15.sp,
        )
    }
}
