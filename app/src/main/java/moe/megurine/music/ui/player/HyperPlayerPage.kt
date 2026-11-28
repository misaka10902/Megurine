package moe.megurine.music.ui.player

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.R
import moe.megurine.music.music.CloudTags
import moe.megurine.music.music.ListenMoeController
import moe.megurine.music.music.ListenMoeStation
import moe.megurine.music.music.LocalLyrics
import moe.megurine.music.music.LyricLine
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.MusicSettings
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.music.PlayMode
import moe.megurine.music.music.buildLyrics
import moe.megurine.music.music.currentLineIndex
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.components.HyperSheet
import moe.megurine.music.ui.components.SheetHeader
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.screens.LyricsSettingsRows
import moe.megurine.music.ui.utils.applyThemeAwareSystemBars
import moe.megurine.music.ui.utils.isAppDarkTheme
import moe.megurine.music.ui.utils.isCompactWindow
import moe.megurine.music.ui.utils.rememberWindowSizeDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.glass.EditSubtitle
import top.yukonga.miuix.kmp.icon.glass.OpenWithOtherApp
import top.yukonga.miuix.kmp.icon.glass.Translate

internal object PlayerPagePalette {
    var lightMode by mutableStateOf(false)
        private set

    fun setLight(value: Boolean) {
        if (lightMode != value) lightMode = value
    }

    val primary: Color get() = if (lightMode) Color(0xFF15151A) else Color.White
    val secondary: Color get() = primary.copy(alpha = 0.66f)
    val controlDim: Color get() = primary.copy(alpha = 0.78f)
}

private val PageTextPrimary: Color get() = PlayerPagePalette.primary
private val PageTextSecondary: Color get() = PlayerPagePalette.secondary
private val ControlTint: Color get() = PlayerPagePalette.primary
private val ControlTintDim: Color get() = PlayerPagePalette.controlDim

private object PlayerSpacing {
    val ProgressSideInset = 20.dp
    val ProgressToTime = 4.dp
    val TimeToControls = 20.dp
    val ControlsToFunctions = 16.dp
    val MainControlGap = 20.dp

    val FunctionRowInset = 20.dp
}

private val PageSideInset = 18.dp


private const val ChromeFadeMs = 220

data class PlayerReveal(
    val header: Float = 1f,
    val cover: Float = 1f,
    val controls: Float = 1f,
)

@Composable
fun HyperPlayerPageContent(
    state: HyperPlayerState,
    modifier: Modifier = Modifier,
    reveal: PlayerReveal = PlayerReveal(),
    onOpenQueue: () -> Unit = {},
    onMore: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val radio by ListenMoeController.state.collectAsState()
    val track = state.track
    val settings by MusicLibrary.settings.collectAsState()
    val favorites by MusicLibrary.favorites.collectAsState()
    val isFavorite = state.playback.song?.let { song -> favorites.any { it.id == song.id } } ?: false

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        .coerceAtLeast(26.dp)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .coerceAtLeast(16.dp)
    val contentTopPadding = statusBarPadding + 26.dp

    val flowMode = settings.playerBackground == "flow"
    val flowDark = when (settings.flowStyle) {
        "dark" -> true
        "light" -> false
        else -> isAppDarkTheme()
    }
    SideEffect { PlayerPagePalette.setLight(flowMode && !flowDark) }
    val lightSystemBars = flowMode && !flowDark
    DisposableEffect(lightSystemBars) {
        val activity = context as? Activity
        if (lightSystemBars) activity?.applyThemeAwareSystemBars(false)
        onDispose { if (lightSystemBars) activity?.applyThemeAwareSystemBars() }
    }

    val circular = settings.coverCircular
    val pageCorner = if (circular) CoverCircleCorner else PlayPageCoverCorner

    var showLyrics by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    val radioActive = radio.active || state.playback.radio
    val lyricSong = state.playback.song

    val cachedLyrics = remember(lyricSong?.id, radioActive) {
        val song = lyricSong
        if (!radioActive && song != null && !song.isLocal && !song.isCloud) {
            StreamApi.cachedLyric(song)?.let { buildLyrics(it.lyric, it.translation, it.roman) }
        } else null
    }

    val loadedLyrics = remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    val loadedSongId = remember { mutableStateOf("") }

    val lyricFetching = remember { mutableStateOf(false) }
    val currentSongId = lyricSong?.id.orEmpty()
    LaunchedEffect(currentSongId, radioActive) {
        if (radioActive) {
            loadedLyrics.value = emptyList()
            loadedSongId.value = ""
            lyricFetching.value = false
            return@LaunchedEffect
        }
        val song = lyricSong
        if (song == null) {
            loadedLyrics.value = emptyList()
            loadedSongId.value = ""
            lyricFetching.value = false
            return@LaunchedEffect
        }

        if (!song.isCloud) {
            StreamApi.cachedLyric(song)?.let {
                loadedLyrics.value = buildLyrics(it.lyric, it.translation, it.roman)
                loadedSongId.value = song.id
                lyricFetching.value = false
                return@LaunchedEffect
            }
        }
        lyricFetching.value = true
        val data = when {
            song.isLocal -> runCatching {
                withContext(Dispatchers.IO) { LocalLyrics.read(context, song) }
            }.getOrNull()
            song.isCloud -> runCatching {
                withContext(Dispatchers.IO) { CloudTags.lyric(context, song.remotePath) }
            }.getOrNull()
            track.lyricId.isNotBlank() -> runCatching { StreamApi.fullLyric(song) }.getOrNull()
            else -> null
        }

        if (state.playback.song?.id == song.id) {
            loadedLyrics.value = data?.let { buildLyrics(it.lyric, it.translation, it.roman) }
                ?: emptyList()
            loadedSongId.value = song.id
            lyricFetching.value = false
        }
    }

    val lyricReady = cachedLyrics != null ||
        (currentSongId.isNotEmpty() && loadedSongId.value == currentSongId)
    val shownLyrics = when {
        cachedLyrics != null -> cachedLyrics
        lyricReady -> loadedLyrics.value
        else -> emptyList()
    }
    val lyricsLoading = !lyricReady || lyricFetching.value

    LaunchedEffect(radioActive) {
        if (radioActive) showLyrics = false
    }

    val shownIndex = currentLineIndex(shownLyrics, state.positionMs)

    val chromeVisible = !state.immersiveMode
    val onToggleImmersive: () -> Unit = {
        if (state.toggleImmersiveMode()) {
            Toast.makeText(context, "沉浸模式", Toast.LENGTH_SHORT).show()
        }
    }

    val artworkState = remember {
        mutableStateOf<Pair<ImageBitmap?, AlbumPalette?>>(
            PlayerArtwork.peek(track.coverUrl) ?: (null to null),
        )
    }
    val artworkUrl = track.coverUrl
    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            artworkState.value = null to null
            return@LaunchedEffect
        }
        val cached = PlayerArtwork.peek(artworkUrl)
        if (cached != null) {
            artworkState.value = cached
            return@LaunchedEffect
        }
        val loaded = PlayerArtwork.load(artworkUrl) ?: return@LaunchedEffect
        if (track.coverUrl == artworkUrl) artworkState.value = loaded
    }
    val artwork = artworkState.value

    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val scope = rememberCoroutineScope()
    var fetchingOnline by remember { mutableStateOf(false) }
    val onFetchOnline: () -> Unit = {
        if (!fetchingOnline) {
            fetchingOnline = true
            scope.launch {
                val match = runCatching { ListenMoeController.findNeteaseMatch() }.getOrNull()
                fetchingOnline = false
                if (match != null) {
                    val startAt = ListenMoeController.positionMs()
                    PlaybackController.playSongs(listOf(match), 0, startAt)
                } else {
                    Toast.makeText(context, "未找到同名歌曲", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize = rememberWindowSizeDp()
        val isLandscape = maxWidth > maxHeight

        val systemView = LocalView.current
        DisposableEffect(isLandscape) {
            val window = (systemView.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, systemView) }
            if (isLandscape) {
                controller?.hide(WindowInsetsCompat.Type.navigationBars())
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(WindowInsetsCompat.Type.navigationBars())
            }
            onDispose {
                if (isLandscape) controller?.show(WindowInsetsCompat.Type.navigationBars())
            }
        }

        val isTablet = minOf(maxWidth, maxHeight) >= 600.dp
        val compact = !isLandscape && !isTablet &&
            (maxHeight < 720.dp || isCompactWindow(windowSize))
        val scale = if (compact) {
            minOf(maxWidth / 400.dp, maxHeight / 620.dp).coerceIn(0.68f, 1f)
        } else {
            1f
        }

        val coverScale by animateFloatAsState(
            targetValue = if (state.isPlaying) 1f else if (compact) 0.92f else 0.8f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "coverScale",
        )

        PlayerBackground(
            flowMode = flowMode,
            flowDark = flowDark,
            coverBitmap = artwork.first,
            palette = artwork.second,
            advancedMaterial = advancedMaterial,
            playing = state.isPlaying,
        )

        if (isTablet) {
            TabletPlayerUi(
                state = state,
                track = track,
                reveal = reveal,
                settings = settings,
                isFavorite = isFavorite,
                circular = circular,
                pageCorner = pageCorner,
                coverScale = coverScale,
                lyrics = shownLyrics,
                lyricIndex = shownIndex,
                lyricsLoading = lyricsLoading,
                onToggleFavorite = { state.playback.song?.let { MusicLibrary.toggleFavorite(it) } },
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                haptic = haptic,
                radioMode = radioActive,
                radioStation = radio.station,
                onSwitchStation = { ListenMoeController.switchStation() },
                onFetchOnline = onFetchOnline,
                fetchingOnline = fetchingOnline,
                chromeVisible = chromeVisible,
                onToggleImmersive = onToggleImmersive,
                onOpenLyricsSettings = { showLyricsSheet = true },
            )
        } else if (isLandscape) {
            LandscapePlayerUi(
                state = state,
                track = track,
                reveal = reveal,
                settings = settings,
                isFavorite = isFavorite,
                circular = circular,
                pageCorner = pageCorner,
                coverScale = coverScale,
                lyrics = shownLyrics,
                lyricIndex = shownIndex,
                lyricsLoading = lyricsLoading,
                showLyrics = showLyrics && !radioActive,
                onShowLyricsChange = { showLyrics = it },
                onToggleFavorite = { state.playback.song?.let { MusicLibrary.toggleFavorite(it) } },
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                haptic = haptic,
                radioMode = radioActive,
                radioStation = radio.station,
                onSwitchStation = { ListenMoeController.switchStation() },
                onFetchOnline = onFetchOnline,
                fetchingOnline = fetchingOnline,
                chromeVisible = chromeVisible,
                onToggleImmersive = onToggleImmersive,
                onOpenLyricsSettings = { showLyricsSheet = true },
            )
        } else {
            PortraitPlayerUi(
                state = state,
                track = track,
                reveal = reveal,
                settings = settings,
                isFavorite = isFavorite,
                circular = circular,
                pageCorner = pageCorner,
                coverScale = coverScale,
                lyrics = shownLyrics,
                lyricIndex = shownIndex,
                lyricsLoading = lyricsLoading,
                showLyrics = showLyrics && !radioActive,
                onShowLyricsChange = { showLyrics = it },
                onToggleFavorite = { state.playback.song?.let { MusicLibrary.toggleFavorite(it) } },
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                haptic = haptic,
                compact = compact,
                scale = scale,
                contentTopPadding = contentTopPadding,
                navBarPadding = navBarPadding,
                radioMode = radioActive,
                radioStation = radio.station,
                onSwitchStation = { ListenMoeController.switchStation() },
                onFetchOnline = onFetchOnline,
                fetchingOnline = fetchingOnline,
                chromeVisible = chromeVisible,
                onToggleImmersive = onToggleImmersive,
                onOpenLyricsSettings = { showLyricsSheet = true },
            )
        }

        HyperSheet(visible = showLyricsSheet, onDismiss = { showLyricsSheet = false }) {
            SheetHeader("歌词设置", standardTitle = true)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer),
            ) {
                LyricsSettingsRows(includeTranslation = false)
            }
        }
    }
}

@Composable
private fun TabletPlayerUi(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    settings: MusicSettings,
    isFavorite: Boolean,
    circular: Boolean,
    pageCorner: Dp,
    coverScale: Float,
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    lyricsLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    haptic: HapticFeedback,
    radioMode: Boolean,
    radioStation: ListenMoeStation,
    onSwitchStation: () -> Unit,
    onFetchOnline: () -> Unit,
    fetchingOnline: Boolean,
    chromeVisible: Boolean,
    onToggleImmersive: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(26.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 36.dp, end = 36.dp, top = statusTop, bottom = navBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        PlayerLyricLayer(
            state = state,
            lines = lyrics,
            currentIndex = lyricIndex,
            lyricsLoading = lyricsLoading,
            showTranslation = settings.showLyricsTranslation,
            lyricCentered = settings.lyricCentered,
            lyricInset = 0.dp,
            onDismiss = null,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )

        Spacer(Modifier.width(32.dp))


        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(ChromeFadeMs - 60, easing = LinearOutSlowInEasing)),
            ) {
                PlayerTitle(
                    track = track,
                    revealHeader = reveal.header,
                    centered = settings.titleCentered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                )
            }

            PlayerCoverLayer(
                state = state,
                track = track,
                circular = circular,
                pageCorner = pageCorner,
                coverFraction = 0.94f,
                coverScale = coverScale,
                contentInset = 0.dp,
                alignment = Alignment.Center,
                offsetY = 0.dp,
                onTap = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            PlayerTransport(
                state = state,
                track = track,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                haptic = haptic,
                modifier = Modifier.fillMaxWidth(),
                scale = 0.95f,
                chromeVisible = chromeVisible,
                radioMode = radioMode,
                radioStation = radioStation,
                onSwitchStation = onSwitchStation,
                onFetchOnline = onFetchOnline,
                fetchingOnline = fetchingOnline,
                onToggleImmersive = onToggleImmersive,
                lyricSlide = 1f,
                lyricsActionsPersistent = true,
                onOpenLyricsSettings = onOpenLyricsSettings,
            )
        }
    }
}

@Composable
private fun PortraitPlayerUi(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    settings: MusicSettings,
    isFavorite: Boolean,
    circular: Boolean,
    pageCorner: Dp,
    coverScale: Float,
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    lyricsLoading: Boolean,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    haptic: HapticFeedback,
    compact: Boolean,
    scale: Float,
    contentTopPadding: Dp,
    navBarPadding: Dp,
    radioMode: Boolean,
    radioStation: ListenMoeStation,
    onSwitchStation: () -> Unit,
    onFetchOnline: () -> Unit,
    fetchingOnline: Boolean,
    chromeVisible: Boolean,
    onToggleImmersive: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
) {
    var lyricSlide by remember { mutableFloatStateOf(if (showLyrics) 1f else 0f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentTopPadding,
                bottom = navBarPadding + 12.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing)) +
                expandVertically(
                    animationSpec = tween(ChromeFadeMs, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ),
            exit = fadeOut(tween(ChromeFadeMs - 60, easing = LinearOutSlowInEasing)) +
                shrinkVertically(
                    animationSpec = tween(ChromeFadeMs, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ),
        ) {
            PlayerTitle(
                track = track,
                revealHeader = reveal.header,
                centered = settings.titleCentered,
                modifier = Modifier.padding(horizontal = PageSideInset),
            )
        }


        PlayerStage(
            state = state,
            track = track,
            circular = circular,
            pageCorner = pageCorner,
            coverFraction = if (compact) 0.88f else CoverSizeFraction,
            coverScale = coverScale,
            showLyrics = showLyrics,
            onShowLyricsChange = onShowLyricsChange,
            lines = lyrics,
            currentIndex = lyricIndex,
            lyricsLoading = lyricsLoading,
            showTranslation = settings.showLyricsTranslation,
            lyricCentered = settings.lyricCentered,
            lyricSwitchOnTap = settings.lyricSwitchOnTap,
            allowLyricSwitch = !radioMode,

            lyricInset = PageSideInset + PlayerSpacing.ProgressSideInset,
            alignment = Alignment.Center,
            contentInset = if (compact) 4.dp else PageSideInset,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),

            coverOffsetY = if (compact) 12.dp else 36.dp * scale,
            onPageProgress = { lyricSlide = it },
        )

        PlayerTransport(
            state = state,
            track = track,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onOpenQueue = onOpenQueue,
            onMore = onMore,
            onOpenSleepTimer = onOpenSleepTimer,
            haptic = haptic,
            scale = scale,
            reveal = reveal.controls,
            chromeVisible = chromeVisible,
            topSpacer = if (compact) 0.dp else 20.dp,
            modifier = Modifier.padding(horizontal = PageSideInset),
            radioMode = radioMode,
            radioStation = radioStation,
            onSwitchStation = onSwitchStation,
            onFetchOnline = onFetchOnline,
            fetchingOnline = fetchingOnline,
            onToggleImmersive = onToggleImmersive,
            lyricSlide = lyricSlide,
            onOpenLyricsSettings = onOpenLyricsSettings,
        )
    }
}

@Composable
private fun LandscapePlayerUi(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    settings: MusicSettings,
    isFavorite: Boolean,
    circular: Boolean,
    pageCorner: Dp,
    coverScale: Float,
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    lyricsLoading: Boolean,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    haptic: HapticFeedback,
    radioMode: Boolean,
    radioStation: ListenMoeStation,
    onSwitchStation: () -> Unit,
    onFetchOnline: () -> Unit,
    fetchingOnline: Boolean,
    chromeVisible: Boolean,
    onToggleImmersive: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
) {
    var lyricSlide by remember { mutableFloatStateOf(if (showLyrics) 1f else 0f) }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(26.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, top = statusTop, bottom = navBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerStage(
            state = state,
            track = track,
            circular = circular,
            pageCorner = pageCorner,
            coverFraction = 0.86f,
            coverScale = coverScale,
            showLyrics = showLyrics,
            onShowLyricsChange = onShowLyricsChange,
            lines = lyrics,
            currentIndex = lyricIndex,
            lyricsLoading = lyricsLoading,
            showTranslation = settings.showLyricsTranslation,
            lyricCentered = settings.lyricCentered,
            lyricSwitchOnTap = settings.lyricSwitchOnTap,
            allowLyricSwitch = !radioMode,
            lyricInset = null,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 24.dp),
            onPageProgress = { lyricSlide = it },
        )

        PlayerTransport(
            state = state,
            track = track,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onOpenQueue = onOpenQueue,
            onMore = onMore,
            onOpenSleepTimer = onOpenSleepTimer,
            haptic = haptic,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            centerVertically = true,
            chromeVisible = chromeVisible,
            radioMode = radioMode,
            radioStation = radioStation,
            onSwitchStation = onSwitchStation,
            onFetchOnline = onFetchOnline,
            fetchingOnline = fetchingOnline,
            onToggleImmersive = onToggleImmersive,
            lyricSlide = lyricSlide,
            lyricsActionsPersistent = true,
            onOpenLyricsSettings = onOpenLyricsSettings,
            title = {
                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing)) +
                        expandVertically(
                            animationSpec = tween(ChromeFadeMs, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(tween(ChromeFadeMs - 60, easing = LinearOutSlowInEasing)) +
                        shrinkVertically(
                            animationSpec = tween(ChromeFadeMs, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    PlayerTitle(
                        track = track,
                        revealHeader = reveal.header,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        centered = settings.titleCentered,
                    )
                }
            },
        )
    }
}

@Composable
private fun PlayerTransport(
    state: HyperPlayerState,
    track: PlayerTrack,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    reveal: Float = 1f,
    chromeVisible: Boolean = true,
    topSpacer: Dp = 0.dp,
    centerVertically: Boolean = false,
    radioMode: Boolean = false,
    radioStation: ListenMoeStation = ListenMoeStation.JPOP,
    onSwitchStation: () -> Unit = {},
    onFetchOnline: () -> Unit = {},
    fetchingOnline: Boolean = false,
    onToggleImmersive: () -> Unit = {},
    lyricSlide: Float = 1f,
    lyricsActionsPersistent: Boolean = false,
    onOpenLyricsSettings: () -> Unit = {},
    title: (@Composable () -> Unit)? = null,
) {
    val lyricsSettings by MusicLibrary.settings.collectAsState()

    val radio by ListenMoeController.state.collectAsState()
    val radioSong = radio.song
    var radioPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(radioMode, radioSong?.id) {
        if (!radioMode) {
            radioPosition = 0L
            return@LaunchedEffect
        }
        while (true) {
            radioPosition = ListenMoeController.positionMs()
            delay(500)
        }
    }
    val targetProgress = if (radioMode) {
        val duration = radioSong?.durationMs ?: 0L
        if (duration > 0L) (radioPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    } else {
        safeProgress(state.positionMs, track.durationMs)
    }


    val lyricsActionsAlpha by animateFloatAsState(
        targetValue = if (chromeVisible) 1f else 0f,
        animationSpec = tween(ChromeFadeMs, easing = FastOutSlowInEasing),
        label = "lyricsActionsAlpha",
    )
    val chromeEnterTop = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing)) +
        expandVertically(tween(ChromeFadeMs, easing = FastOutSlowInEasing))
    val chromeExitTop = fadeOut(tween(ChromeFadeMs - 60, easing = LinearOutSlowInEasing)) +
        shrinkVertically(tween(ChromeFadeMs, easing = FastOutSlowInEasing))
    val chromeEnterBottom = fadeIn(tween(ChromeFadeMs, easing = FastOutSlowInEasing)) +
        expandVertically(tween(ChromeFadeMs, easing = FastOutSlowInEasing), expandFrom = Alignment.Bottom)
    val chromeExitBottom = fadeOut(tween(ChromeFadeMs - 60, easing = LinearOutSlowInEasing)) +
        shrinkVertically(tween(ChromeFadeMs, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Bottom)

    Column(
        modifier = modifier.graphicsLayer {
            val e = reveal.coerceIn(0f, 1f)
            alpha = e
            translationY = (1f - e) * 28.dp.toPx()
        },
        verticalArrangement = if (centerVertically) Arrangement.Center else Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        title?.invoke()
        if (topSpacer > 0.dp) Spacer(Modifier.height(topSpacer))

        val lyricsActions: @Composable () -> Unit = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = if (lyricsActionsPersistent) {
                            lyricsActionsAlpha
                        } else if (radioMode) {
                            0f
                        } else {
                            lyricSlide.coerceIn(0f, 1f)
                        }
                        alpha = p
                        translationX = if (lyricsActionsPersistent) 0f else (1f - p) * size.width
                    }
                    .padding(horizontal = PlayerSpacing.ProgressSideInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageIconButton(
                    imageVector = MiuixIcons.Glass.EditSubtitle,
                    description = "歌词设置",
                    iconSize = 26.dp,
                    tint = ControlTintDim,
                    boxSize = 30.dp,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenLyricsSettings()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.Glass.Translate,
                    description = "歌词翻译",
                    iconSize = 26.dp,
                    tint = if (lyricsSettings.showLyricsTranslation) ControlTint else ControlTintDim,
                    boxSize = 30.dp,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        MusicLibrary.setShowLyricsTranslation(!lyricsSettings.showLyricsTranslation)
                    },
                )
            }
        }
        if (lyricsActionsPersistent) {
            lyricsActions()
            Spacer(Modifier.height(PlayerSpacing.ProgressToTime))
        }
        AnimatedVisibility(
            visible = chromeVisible,
            enter = chromeEnterTop,
            exit = chromeExitTop,
        ) {
            Column {
                if (!lyricsActionsPersistent) {
                    lyricsActions()
                    Spacer(Modifier.height(PlayerSpacing.ProgressToTime))
                }
                HandoffProgressBar(
                    target = targetProgress,
                    onSeek = { if (!radioMode) state.seekTo((it * track.durationMs).toLong()) },
                    onSeekFinished = { if (!radioMode) state.seekTo((it * track.durationMs).toLong()) },
                    enabled = chromeVisible && !radioMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerSpacing.ProgressSideInset),
                )
                Spacer(Modifier.height(PlayerSpacing.ProgressToTime))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerSpacing.ProgressSideInset),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(if (radioMode) radioPosition else state.positionMs),
                        color = PageTextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = formatDuration(
                            if (radioMode) radioSong?.durationMs ?: 0L else track.durationMs,
                        ),
                        color = PageTextSecondary,
                        fontSize = 12.sp,
                    )
                }

                Spacer(Modifier.height(PlayerSpacing.TimeToControls))
            }
        }

        val ctrlIcon = 30.dp
        val ctrlBox = 56.dp * scale
        val ctrlGap = PlayerSpacing.MainControlGap * scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = !radioMode,
                enter = fadeIn(tween(300)) + expandHorizontally(tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200, easing = FastOutSlowInEasing)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PageIconButton(
                        description = "上一首",
                        iconSize = ctrlIcon,
                        tint = ControlTint,
                        boxSize = ctrlBox,
                        content = { MiuixSkipIcon(isNext = false, tint = ControlTint, size = ctrlIcon) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.previous()
                        },
                    )
                    Spacer(Modifier.size(ctrlGap))
                }
            }
            PlayPauseButton(
                isPlaying = state.isPlaying,
                buttonSize = 64.dp * scale,
                iconSize = 44.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    state.togglePlayPause()
                },
                onLongClick = onToggleImmersive,
            )
            AnimatedVisibility(
                visible = !radioMode,
                enter = fadeIn(tween(300)) + expandHorizontally(tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200, easing = FastOutSlowInEasing)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(ctrlGap))
                    PageIconButton(
                        description = "下一首",
                        iconSize = ctrlIcon,
                        tint = ControlTint,
                        boxSize = ctrlBox,
                        content = { MiuixSkipIcon(isNext = true, tint = ControlTint, size = ctrlIcon) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.next()
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = chromeEnterBottom,
            exit = chromeExitBottom,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(PlayerSpacing.ControlsToFunctions))
                AnimatedContent(
                    targetState = radioMode,
                    transitionSpec = {
                        fadeIn(tween(320, delayMillis = 60)) togetherWith fadeOut(tween(180))
                    },
                    label = "transportFunctions",
                ) { radio ->
                    if (radio) {
                        RadioActionsRow(
                            station = radioStation,
                            haptic = haptic,
                            scale = scale,
                            fetching = fetchingOnline,
                            onOpenQueue = onOpenQueue,
                            onSwitchStation = onSwitchStation,
                            onFetchOnline = onFetchOnline,
                        )
                    } else {
                        val fnIcon = 23.dp
                        val fnBox = 44.dp * scale
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerSpacing.FunctionRowInset),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayModeButton(
                                mode = state.playMode,
                                boxSize = fnBox,
                                iconScale = 1f,
                                enabled = chromeVisible,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    state.cyclePlayMode()
                                },
                            )
                            PageIconButton(
                                imageVector = MiuixIcons.Timer,
                                description = "定时关闭",
                                iconSize = fnIcon,
                                tint = ControlTintDim,
                                boxSize = fnBox,
                                enabled = chromeVisible,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onOpenSleepTimer()
                                },
                            )
                            FavoriteButton(
                                isFavorite = isFavorite,
                                boxSize = fnBox,
                                enabled = chromeVisible,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onToggleFavorite()
                                },
                            )
                            PageIconButton(
                                imageVector = MiuixIcons.Playlist,
                                description = "播放列表",
                                iconSize = 24.dp,
                                tint = ControlTintDim,
                                boxSize = fnBox,
                                enabled = chromeVisible,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onOpenQueue()
                                },
                            )
                            PageIconButton(
                                imageVector = MiuixIcons.More,
                                description = "更多",
                                iconSize = 22.dp,
                                tint = ControlTintDim,
                                boxSize = fnBox,
                                enabled = chromeVisible,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onMore()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioActionsRow(
    station: ListenMoeStation,
    haptic: HapticFeedback,
    scale: Float,
    fetching: Boolean,
    onOpenQueue: () -> Unit,
    onSwitchStation: () -> Unit,
    onFetchOnline: () -> Unit,
) {
    val box = 44.dp * scale
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageIconButton(
            imageVector = MiuixIcons.Playlist,
            description = "播放队列",
            iconSize = 22.dp,
            tint = ControlTintDim,
            boxSize = box,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onOpenQueue()
            },
        )
        Spacer(Modifier.width(14.dp))
        PageIconButton(
            imageVector = MiuixIcons.Refresh,
            description = "切换到${station.other.label}",
            iconSize = 22.dp,
            tint = ControlTintDim,
            boxSize = box,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onSwitchStation()
            },
        )
        Spacer(Modifier.width(14.dp))
        PageIconButton(
            imageVector = MiuixIcons.Glass.OpenWithOtherApp,
            description = "切换至内置音源",
            iconSize = 22.dp,
            tint = if (fetching) ControlTintDim.copy(alpha = 0.35f) else ControlTintDim,
            boxSize = box,
            onClick = {
                if (fetching) return@PageIconButton
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onFetchOnline()
            },
        )
        Spacer(Modifier.width(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_listen_moe),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "来自LISTEN.moe",
                color = PageTextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun PlayerTitle(
    track: PlayerTrack,
    revealHeader: Float,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
) {
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val e = revealHeader.coerceIn(0f, 1f)
                alpha = e

                translationY = (1f - e) * -14.dp.toPx()
            },
    ) {
        val sideInset = if (centered) 0.dp else PlayerSpacing.ProgressSideInset
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sideInset),
            horizontalAlignment = align,
        ) {
            Text(
                text = track.title,
                color = PageTextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = track.artist,
                color = PageTextSecondary,
                fontSize = 14.sp,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    buttonSize: Dp = 64.dp,
    iconSize: Dp = 44.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "playPause",
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = MiuixIcons.Pause,
            contentDescription = "暂停",
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = progress
                    val s = 0.70f + 0.30f * progress
                    scaleX = s
                    scaleY = s
                    rotationZ = -55f * (1f - progress)
                },
            colorFilter = ColorFilter.tint(ControlTint),
        )
        Image(
            imageVector = MiuixIcons.Play,
            contentDescription = "播放",
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = 1f - progress
                    val s = 0.70f + 0.30f * (1f - progress)
                    scaleX = s
                    scaleY = s
                    rotationZ = 55f * progress
                },
            colorFilter = ColorFilter.tint(ControlTint),
        )
    }
}

@Composable
private fun MiuixSkipIcon(
    isNext: Boolean,
    tint: Color,
    size: Dp,
) {
    val barModifier = Modifier
        .width(2.5.dp)
        .height(size * 0.62f)
        .clip(RoundedCornerShape(1.25.dp))
        .background(tint)
    val triangleModifier = Modifier
        .size(size * 0.9f)
        .rotate(if (isNext) 0f else 180f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isNext) {
            Box(barModifier)
            Spacer(Modifier.width(size * 0.08f))
        }
        Image(
            imageVector = MiuixIcons.Play,
            contentDescription = null,
            modifier = triangleModifier,
            colorFilter = ColorFilter.tint(tint),
        )
        if (isNext) {
            Spacer(Modifier.width(size * 0.08f))
            Box(barModifier)
        }
    }
}

private val PlayModeVectorIconSize = 25.dp

@Composable
private fun PlayModeButton(
    mode: PlayMode,
    onClick: () -> Unit,
    boxSize: Dp = 44.dp,
    iconScale: Float = 1f,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { playerControlIconTransition() },
            label = "playModeIcon",
        ) { target ->
            val description = when (target) {
                PlayMode.ORDER -> "列表循环"
                PlayMode.SHUFFLE -> "随机播放"
                PlayMode.REPEAT_ONE -> "单曲循环"
            }
            if (target == PlayMode.ORDER) {
                Image(
                    imageVector = RepeatAllIcon,
                    contentDescription = description,
                    modifier = Modifier.size(PlayModeVectorIconSize * iconScale),
                    colorFilter = ColorFilter.tint(ControlTintDim),
                )
            } else {
                Image(
                    imageVector = if (target == PlayMode.SHUFFLE) ShuffleIcon else RepeatOneIcon,
                    contentDescription = description,
                    modifier = Modifier.size(PlayModeVectorIconSize * iconScale),
                    colorFilter = ColorFilter.tint(ControlTintDim),
                )
            }
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isFavorite,
            transitionSpec = { playerControlIconTransition() },
            label = "favoriteIcon",
        ) { liked ->
            Image(
                imageVector = if (liked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                contentDescription = if (liked) "取消收藏" else "加入收藏",
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(if (liked) ControlTint else ControlTintDim),
            )
        }
    }
}

@Composable
private fun PageIconButton(
    description: String,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: (@Composable () -> Unit)? = null,
    boxSize: Dp = 44.dp,
    tint: Color = ControlTint,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Image(
                imageVector = requireNotNull(imageVector),
                contentDescription = description,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}


@Composable
private fun HandoffProgressBar(
    target: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420, easing = LinearEasing),
        label = "progressHandoff",
    )
    HyperProgressBar(
        progress = progress,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun HyperProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var pending by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(progress, pending) {
        val target = pending ?: return@LaunchedEffect
        if (kotlin.math.abs(target - progress) < 0.012f) pending = null
    }

    val expand by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 620f,
            visibilityThreshold = 0.001f,
        ),
        label = "seekExpand",
    )

    val shown = when {
        scrubbing -> scrubValue
        pending != null -> pending!!
        else -> if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
    }
    val trackColor = PageTextPrimary.copy(alpha = 0.30f)
    val playedColor = PageTextPrimary

    Box(
        modifier = modifier
            .height(34.dp)
            .then(if (enabled) Modifier.pointerInput(Unit) {
                detectTapGestures { offset ->
                    val value = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(value)
                    onSeekFinished(value)
                }
            } else Modifier)
            .then(if (enabled) Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubbing = true
                        scrubValue = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        pending = scrubValue
                        onSeek(scrubValue)
                        onSeekFinished(scrubValue)
                        scrubbing = false
                    },
                    onDragCancel = { scrubbing = false },
                ) { change, _ ->
                    change.consume()
                    scrubValue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                }
            } else Modifier)
            .drawBehind {
                val trackHeight = (7.dp + 8.dp * expand).toPx()
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                val top = (size.height - trackHeight) / 2f

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )

                val playedWidth = size.width * shown
                if (playedWidth > 0f) {
                    drawRoundRect(
                        color = playedColor,
                        topLeft = Offset(0f, top),
                        size = Size(playedWidth.coerceAtLeast(trackHeight), trackHeight),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

private fun safeProgress(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    val raw = positionMs / durationMs.toFloat()
    if (raw.isNaN() || raw.isInfinite()) return 0f
    return raw.coerceIn(0f, 1f)
}

private fun <S> androidx.compose.animation.AnimatedContentTransitionScope<S>.playerControlIconTransition() =
    (
        fadeIn(tween(180, delayMillis = 130, easing = LinearOutSlowInEasing)) +
            scaleIn(
                tween(180, delayMillis = 130, easing = LinearOutSlowInEasing),
                initialScale = 0.6f,
            )
        ).togetherWith(
        fadeOut(tween(120, delayMillis = 20, easing = LinearEasing)) +
            scaleOut(tween(140, easing = LinearOutSlowInEasing), targetScale = 0.5f),
    )
