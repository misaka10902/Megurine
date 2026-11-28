package moe.megurine.music.ui.activities
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.blur.LayerBackdrop

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import java.io.File
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import moe.megurine.music.music.AudioQuality
import moe.megurine.music.music.Downloader
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.PlaylistCoverStore
import moe.megurine.music.music.ListenMoeController
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.music.PlaybackService
import moe.megurine.music.music.Playlist
import moe.megurine.music.music.PlaylistImporter
import moe.megurine.music.music.Song
import moe.megurine.music.music.WallpaperBackground
import moe.megurine.music.music.WallpaperManager
import moe.megurine.music.music.CoverLoader
import moe.megurine.music.music.LocalSongs
import moe.megurine.music.music.MediaCache
import moe.megurine.music.music.WebDavApi
import moe.megurine.music.music.MeloxBridge
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.components.AppPickerKind
import moe.megurine.music.ui.components.AppPickerSheet
import moe.megurine.music.ui.components.AppTooltip
import moe.megurine.music.ui.components.ArtistPickerSheet
import moe.megurine.music.ui.components.ConfirmDialog
import moe.megurine.music.music.LocalFixedWallpaperBackdrop
import moe.megurine.music.music.LocalIsWideScreen
import moe.megurine.music.music.LocalRailContentInset
import moe.megurine.music.music.LocalRailObstructionStart
import moe.megurine.music.music.fixedWallpaperRegion
import moe.megurine.music.ui.components.HyperSheet
import moe.megurine.music.ui.components.PlaybackQueueSheet
import moe.megurine.music.ui.components.PlaylistPickerSheet
import moe.megurine.music.ui.components.PlaylistAddMenuPopup
import moe.megurine.music.ui.components.PlaylistJsonImportDialog
import moe.megurine.music.ui.components.PlaylistLinkImportDialog
import moe.megurine.music.ui.components.HyperBottomBar
import moe.megurine.music.ui.components.HyperNavigationRail
import moe.megurine.music.ui.components.SelectionActionBar
import moe.megurine.music.ui.components.SelectionBarState
import moe.megurine.music.ui.components.SleepTimerSheet
import moe.megurine.music.ui.components.TrackMoreSheet
import moe.megurine.music.ui.effects.miuix.LocalActiveBackdrop
import moe.megurine.music.ui.effects.miuix.LocalPageBackdrop
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.rememberAdvancedMaterialEnabled
import moe.megurine.music.ui.player.BottomBarAreaHeight
import moe.megurine.music.ui.player.HyperMiniPlayer
import moe.megurine.music.ui.player.HyperPlayerLayer
import moe.megurine.music.ui.player.PlayerArtwork
import moe.megurine.music.ui.player.MiniPlayerHeight
import moe.megurine.music.ui.player.MiniPlayerWidthFraction
import moe.megurine.music.ui.player.MiniPlayerToNavBarSpacing
import moe.megurine.music.ui.player.rememberHyperPlayerState
import moe.megurine.music.ui.screens.AlbumScreen
import moe.megurine.music.ui.screens.AboutAppScreen
import moe.megurine.music.ui.screens.ArtistScreen
import moe.megurine.music.ui.screens.DataSettingsScreen
import moe.megurine.music.ui.screens.LibraryScreen
import moe.megurine.music.ui.screens.ExploreScreen
import moe.megurine.music.ui.screens.FavoritesScreen
import moe.megurine.music.ui.screens.HistoryScreen
import moe.megurine.music.ui.screens.HyperSearchScreen
import moe.megurine.music.ui.screens.PlaylistDetailScreen
import moe.megurine.music.ui.screens.OnlinePlaylistScreen
import moe.megurine.music.ui.search.GlassSearchOverlay
import moe.megurine.music.ui.search.rememberGlassSearchBarAlpha
import moe.megurine.music.ui.search.glassSearchBarTransition
import moe.megurine.music.ui.screens.PlaylistScreen
import moe.megurine.music.ui.screens.PlayerSettingsScreen
import moe.megurine.music.ui.screens.SourceSettingsScreen
import moe.megurine.music.ui.screens.DownloadSettingsScreen
import moe.megurine.music.ui.screens.LyricsSettingsScreen
import moe.megurine.music.ui.screens.PlaybackSettingsScreen
import moe.megurine.music.ui.screens.SettingsScreen
import moe.megurine.music.ui.screens.WallpaperScreen
import moe.megurine.music.ui.theme.HyperTheme
import moe.megurine.music.ui.utils.BottomFloatingHeight
import moe.megurine.music.ui.utils.BottomFloatingWidthFraction
import moe.megurine.music.ui.utils.applyThemeAwareSystemBars
import moe.megurine.music.ui.utils.bottomContentFade
import moe.megurine.music.ui.utils.isLargeScreen
import moe.megurine.music.ui.utils.rememberWindowSizeDp
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.floor
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import moe.megurine.music.ui.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.NotInterested
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.core.NavKey
import moe.megurine.music.ui.components.SheetCard
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

private val TAB_TITLES = listOf("探索", "歌单", "历史", "设置")

private sealed interface MusicRoute : NavKey {
    val navId: Long
    data class Artist(val name: String, val id: String = "", override val navId: Long = 0L) : MusicRoute
    data class Album(
        val name: String,
        val artist: String,
        val albumId: String = "",
        val songId: String = "",
        override val navId: Long = 0L,
    ) : MusicRoute
    data class PlaylistDetail(val playlistId: String, override val navId: Long = 0L) : MusicRoute
    data class OnlinePlaylist(
        val playlistId: String,
        val playlistName: String,
        override val navId: Long = 0L,
    ) : MusicRoute
    data class Favorites(override val navId: Long = 0L) : MusicRoute
    data class Wallpaper(override val navId: Long = 0L) : MusicRoute
    data class PlayerSettings(override val navId: Long = 0L) : MusicRoute
    data class PlaybackSettings(override val navId: Long = 0L) : MusicRoute
    data class SourceSettings(override val navId: Long = 0L) : MusicRoute
    data class DownloadSettings(override val navId: Long = 0L) : MusicRoute
    data class LyricsSettings(override val navId: Long = 0L) : MusicRoute
    data class DataSettings(override val navId: Long = 0L) : MusicRoute
    data class Library(override val navId: Long = 0L) : MusicRoute
    data class AboutApp(override val navId: Long = 0L) : MusicRoute
}

private data object RootRoute : NavKey

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        applyThemeAwareSystemBars()
        WallpaperManager.init(this)
        setContent {
            HyperTheme {
                HyperApp()
            }
        }
    }

    override fun onStop() {
        PlaybackController.saveSession()
        super.onStop()
    }

    override fun onDestroy() {

        CoverLoader.clearPinned()
        super.onDestroy()
    }
}

@Composable
fun HyperApp() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }


    val windowSize = rememberWindowSizeDp()
    val wideScreen = isLargeScreen(windowSize)
    val expandRail = windowSize.width >= 1200.dp
    val railState = rememberGlassNavigationRailState(initialExpanded = expandRail)
    LaunchedEffect(expandRail) {
        railState.expanded = expandRail
    }

    val railInset = if (wideScreen) {
        (GlassNavigationRailDefaults.Margin + GlassNavigationRailDefaults.ExpandedWidth) * railState.progress
    } else {
        0.dp
    }

    val cutoutStart = WindowInsets.displayCutout.asPaddingValues()
        .calculateStartPadding(LayoutDirection.Ltr)
    val contentStartInset = railInset + cutoutStart
    val navCornerRadius = rememberNavSystemCornerRadius()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    var navJob by remember { mutableStateOf<Job?>(null) }
    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, isNavigating) {
        if (!isNavigating && selectedTab != pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
    }
    val selectTab: (Int) -> Unit = { index ->
        if (index != selectedTab) {
            navJob?.cancel()
            selectedTab = index
            isNavigating = true
            navJob = scope.launch {
                val myJob = coroutineContext.job
                try {
                    pagerState.springAnimateToPage(index)
                } finally {
                    if (navJob == myJob) {
                        isNavigating = false
                        if (pagerState.currentPage != index) selectedTab = pagerState.currentPage
                    }
                }
            }
        }
    }

    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchBarAlpha = rememberGlassSearchBarAlpha(searchExpanded)
    val exploreScrollBehavior = MiuixScrollBehavior()
    val historyScrollBehavior = MiuixScrollBehavior()
    val playlistScrollBehavior = MiuixScrollBehavior()
    val settingsScrollBehavior = MiuixScrollBehavior()

    val pageScrollBehaviors = remember(
        exploreScrollBehavior,
        playlistScrollBehavior,
        historyScrollBehavior,
        settingsScrollBehavior,
    ) {
        listOf(exploreScrollBehavior, playlistScrollBehavior, historyScrollBehavior, settingsScrollBehavior)
    }

    val barScrollBehavior = MiuixScrollBehavior()

    val playerState = rememberHyperPlayerState()
    val playerCoverUrl = playerState.track.coverUrl
    LaunchedEffect(playerCoverUrl) {
        if (!playerCoverUrl.isNullOrBlank()) PlayerArtwork.prefetch(playerCoverUrl)
    }

    val navBackStack = remember { navBackStackOf(RootRoute) }
    var routeSeq by remember { mutableStateOf(0L) }
    val route = navBackStack.lastOrNull() as? MusicRoute
    fun pushRoute(target: MusicRoute) {
        val stamped = when (target) {
            is MusicRoute.Artist -> target.copy(navId = ++routeSeq)
            is MusicRoute.Album -> target.copy(navId = ++routeSeq)
            is MusicRoute.PlaylistDetail -> target.copy(navId = ++routeSeq)
            is MusicRoute.OnlinePlaylist -> target.copy(navId = ++routeSeq)
            is MusicRoute.Favorites -> target.copy(navId = ++routeSeq)
            is MusicRoute.Wallpaper -> target.copy(navId = ++routeSeq)
            is MusicRoute.PlayerSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.PlaybackSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.SourceSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.DownloadSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.LyricsSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.DataSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.Library -> target.copy(navId = ++routeSeq)
            is MusicRoute.AboutApp -> target.copy(navId = ++routeSeq)
        }
        navBackStack.add(stamped)
    }
    fun popRoute() { if (navBackStack.size > 1) navBackStack.removeLastOrNull() }

    val miniPlayerShift by animateDpAsState(

        targetValue = if (wideScreen) {
            0.dp
        } else if (route == null && !searchExpanded) 0.dp else MiniPlayerHeight + MiniPlayerToNavBarSpacing,
        animationSpec = tween(durationMillis = 300),
        label = "miniPlayerShift",
    )

    var moreSong by remember { mutableStateOf<Song?>(null) }
    var moreDelete by remember { mutableStateOf<((Song) -> Unit)?>(null) }
    var moreDeleteLabel by remember { mutableStateOf("删除歌曲") }
    var moreDeleteIcon by remember { mutableStateOf<ImageVector>(MiuixIcons.Delete) }
    var moreFromFavorites by remember { mutableStateOf(false) }
    var artistPickerSong by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createForSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var appPickerSong by remember { mutableStateOf<Song?>(null) }
    var appPickerKind by remember { mutableStateOf(AppPickerKind.OpenWith) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var selectionBar by remember { mutableStateOf<SelectionBarState?>(null) }
    val selectionActive = selectionBar != null
    fun exitSelection() {
        selectionBar?.onExit?.invoke()
        selectionBar = null
    }
    var pickerSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var exportJson by remember { mutableStateOf<String?>(null) }

    var addMenuExpanded by remember { mutableStateOf(false) }
    val addMenuAnchor = rememberGlassPopupAnchor()
    var showLinkImport by remember { mutableStateOf(false) }
    var linkImportBusy by remember { mutableStateOf(false) }
    var linkImportStatus by remember { mutableStateOf<String?>(null) }
    var linkImportError by remember { mutableStateOf<String?>(null) }
    var linkImportJob by remember { mutableStateOf<Job?>(null) }
    var showJsonImport by remember { mutableStateOf(false) }
    var jsonImportBusy by remember { mutableStateOf(false) }
    var jsonImportStatus by remember { mutableStateOf<String?>(null) }
    var jsonImportError by remember { mutableStateOf<String?>(null) }
    var jsonImportJob by remember { mutableStateOf<Job?>(null) }

    var confirmDeletePlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var confirmDeleteHistory by remember { mutableStateOf<Song?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    var downloadDeleteConfirm by remember { mutableStateOf<List<Song>?>(null) }
    var pendingDirectDelete by remember { mutableStateOf(emptyList<Song>()) }
    var pendingMediaDelete by remember { mutableStateOf(emptyList<Song>()) }
    var downloadsRefreshKey by remember { mutableIntStateOf(0) }

    val rootView = LocalView.current
    val focusManager = LocalFocusManager.current
    val imm = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }

    fun hideKeyboard() {
        focusManager.clearFocus()
        rootView.clearFocus()
        imm?.hideSoftInputFromWindow(rootView.windowToken, 0)
    }

    val overlayVisible = route != null || moreSong != null || pickerSongs.isNotEmpty() ||
        showCreateDialog || renameTarget != null || showQualitySheet || appPickerSong != null ||
        showQueueSheet || showSleepTimer || confirmDeletePlaylists.isNotEmpty() ||
        confirmDeleteHistory != null || downloadDeleteConfirm != null ||
        showLinkImport || showJsonImport || addMenuExpanded

    LaunchedEffect(selectedTab, overlayVisible) {
        if (selectedTab != 0 || overlayVisible) hideKeyboard()
    }

    DisposableEffect(context) {
        val owner = context as? androidx.lifecycle.LifecycleOwner
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (playerState.isExpanded) hideKeyboard()
            }
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }

    fun openMore(
        song: Song,
        delete: ((Song) -> Unit)?,
        label: String,
        fromFavorites: Boolean = false,
        deleteIcon: ImageVector = MiuixIcons.Delete,
    ) {
        hideKeyboard()
        if (song.isCloud) {
            moreDelete = { target ->
                scope.launch {
                    val message = WebDavApi.delete(target).fold(
                        onSuccess = { "已从云端删除" },
                        onFailure = { it.message ?: "删除失败" },
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    downloadsRefreshKey++
                }
            }
            moreDeleteLabel = "从云端删除歌曲"
        } else {
            moreDelete = delete
            moreDeleteLabel = label
        }
        moreDeleteIcon = deleteIcon
        moreFromFavorites = fromFavorites
        moreSong = song
    }

    fun finishDownloadDelete(mediaSongs: List<Song>, mediaOk: Boolean) {
        scope.launch {

            val directDeleted = withContext(Dispatchers.IO) {
                LocalSongs.deleteDirect(context, pendingDirectDelete)
            }
            val actuallyDeleted = directDeleted.toMutableList()
            if (mediaOk) actuallyDeleted.addAll(mediaSongs)
            val uris = actuallyDeleted.mapNotNull { it.localUri.ifBlank { null } }.toSet()
            if (uris.isNotEmpty()) {

                MusicLibrary.removeByLocalUri(uris)
                PlaybackController.removeByLocalUri(uris)
                MediaCache.removeByLocalUris(context, uris)
                LocalSongs.notifyChanged()
            }
            pendingDirectDelete = emptyList()
            pendingMediaDelete = emptyList()
            downloadDeleteConfirm = null
            downloadsRefreshKey++
            val total = actuallyDeleted.size
            Toast.makeText(
                context,
                if (total > 0) "已删除 $total 首歌曲" else "删除失败",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val delLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->

        val mediaOk = result.resultCode == Activity.RESULT_OK
        finishDownloadDelete(pendingMediaDelete, mediaOk)
    }

    fun startDownloadDelete(targets: List<Song>) {
        val plan = LocalSongs.deletePlan(targets)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && plan.mediaStoreSongs.isNotEmpty()) {

            pendingDirectDelete = plan.directSongs
            pendingMediaDelete = plan.mediaStoreSongs
            try {
                val req = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    plan.mediaStoreSongs.map { Uri.parse(it.localUri) },
                )
                delLauncher.launch(IntentSenderRequest.Builder(req.intentSender).build())
            } catch (_: Throwable) {

                pendingDirectDelete = plan.directSongs + plan.mediaStoreSongs
                pendingMediaDelete = emptyList()
                finishDownloadDelete(emptyList(), false)
            }
        } else {

            pendingDirectDelete = plan.directSongs + plan.mediaStoreSongs
            pendingMediaDelete = emptyList()
            finishDownloadDelete(emptyList(), false)
        }
    }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {  },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            runCatching { if (future.isDone) future.get().release() }
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        showJsonImport = true
        jsonImportBusy = true
        jsonImportError = null
        jsonImportStatus = "读取文件中…"
        jsonImportJob = scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } ?: throw IllegalStateException("无法读取该文件")
                val res = PlaylistImporter.importFromJson(text) { done, total ->
                    jsonImportStatus = "匹配网易云歌曲 $done/$total"
                }
                val playlist = MusicLibrary.createPlaylist(res.name)
                var added = 0
                res.songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                Toast.makeText(context, "已添加歌单，共${added}首歌曲", Toast.LENGTH_LONG).show()
                jsonImportBusy = false
                jsonImportStatus = null
                showJsonImport = false
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                jsonImportBusy = false
                jsonImportStatus = null
                jsonImportError = e.message ?: "导入失败"
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = exportJson
        exportJson = null
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess {
                Toast.makeText(context, "歌单已导出", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "导出失败：${it.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val backgroundColor = MiuixTheme.colorScheme.surface
    val wallpaperActive by WallpaperManager.state.collectAsState()

    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val wallpaperBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val navDisplayBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }


    val searchPageBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val activeBackdrop: Backdrop = navDisplayBackdrop

    fun contentPlayable(uri: Uri): Boolean {
        uri.getQueryParameter("path")?.let { path ->
            val file = File(path)
            return file.isFile && file.canRead()
        }
        MeloxBridge.resolveFile(context, uri)?.let { return true }
        val fd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull()
        if (fd != null) {
            fd.close()
            return true
        }
        return false
    }

    fun localSongPlayable(raw: String): Boolean = runCatching {
        val uri = Uri.parse(raw)
        when (uri?.scheme?.lowercase()) {
            "file" -> uri.path?.let { File(it) }?.let { it.isFile && it.canRead() } == true
            "content" -> contentPlayable(uri)
            else -> false
        }
    }.getOrDefault(false)

    fun playList(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        val target = songs.getOrNull(index) ?: return
        scope.launch {

            val playable = withContext(Dispatchers.IO) {
                when {
                    target.isLocal -> localSongPlayable(target.localUri)
                    target.isCloud -> WebDavApi.checkPlayable(context, target)
                    else -> true
                }
            }
            if (playable) {
                PlaybackController.playSongs(songs, index)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            } else {
                Toast.makeText(context, "无法播放该歌曲", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = selectionActive) { exitSelection() }
    BackHandler(enabled = navBackStack.size > 1 && !selectionActive) { popRoute() }

    BackHandler(enabled = searchExpanded && !selectionActive && !playerState.isExpanded) {
        searchExpanded = false
    }

    val advancedMaterial = rememberAdvancedMaterialEnabled()

    val activePageLayer = remember { mutableStateOf<Backdrop?>(backdrop) }

    val playerPageLayer = rememberLayerBackdrop { drawContent() }
    var pageLayerBeforePlayer by remember { mutableStateOf<Backdrop?>(null) }
    LaunchedEffect(playerState.isExpanded) {
        if (playerState.isExpanded) {
            pageLayerBeforePlayer = activePageLayer.value
            activePageLayer.value = playerPageLayer
        } else if (activePageLayer.value === playerPageLayer) {
            activePageLayer.value = pageLayerBeforePlayer ?: backdrop
        }
    }

    var pageLayerBeforeSearch by remember { mutableStateOf<Backdrop?>(null) }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            pageLayerBeforeSearch = activePageLayer.value
            activePageLayer.value = searchPageBackdrop
        } else if (activePageLayer.value === searchPageBackdrop) {
            activePageLayer.value = pageLayerBeforeSearch ?: backdrop
        }
    }

    CompositionLocalProvider(
        LocalAdvancedMaterialEnabled provides advancedMaterial,
        LocalActiveBackdrop provides wallpaperBackdrop,
        LocalPageBackdrop provides activePageLayer,
        LocalFixedWallpaperBackdrop provides wallpaperBackdrop,
        LocalIsWideScreen provides wideScreen,
        LocalRailContentInset provides (if (wideScreen) railInset else 0.dp),

        LocalRailObstructionStart provides {
            if (wideScreen && !railState.expanded && railState.collapsedWidth > 0.dp) {
                railState.collapsedStart
            } else {
                null
            }
        },
    ) {
    @Composable
    fun NavPage(content: @Composable (LayerBackdrop) -> Unit) {

        val pageSurface = MiuixTheme.colorScheme.surface
        val pageLayer = rememberLayerBackdrop {
            drawRect(pageSurface)
            drawContent()
        }
        DisposableEffect(pageLayer) {
            activePageLayer.value = pageLayer
            onDispose {

                if (activePageLayer.value === pageLayer) activePageLayer.value = backdrop
            }
        }
        content(pageLayer)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { _ ->
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {

        val noPredictiveBackOwner = remember {
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher =
                    NavigationEventDispatcher().also { it.isEnabled = false }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(navDisplayBackdrop),
        ) {


        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(wallpaperBackdrop),
        ) {
            WallpaperBackground()
        }
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides noPredictiveBackOwner) {
        val navDisplay: @Composable (Modifier) -> Unit = { navModifier ->
        NavDisplay(
            backStack = navBackStack,

            modifier = navModifier
                .graphicsLayer { alpha = searchBarAlpha.value }

                .then(
                    if (route is MusicRoute.AboutApp) {
                        Modifier
                    } else {
                        Modifier.bottomContentFade(
                            startFromBottom = MiniPlayerToNavBarSpacing +
                                (if (wideScreen) 0.dp else BottomBarAreaHeight) + 16.dp,
                            bottomAlpha = 0.12f,
                        )
                    }
                ),

            onBack = {
                if (selectionActive) exitSelection() else popRoute()
            },
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                enableCornerClip = true,

                cornerClipRadius = if (wideScreen) 0.dp else navCornerRadius,
                dimAmount = 0.5f,
                backdropColor = if (wallpaperActive.hasImage) Color.Transparent else MiuixTheme.colorScheme.surface,
            ),
        ) {
            entry<RootRoute> {

    val barTitle = if (selectionActive) {
        "已选择 ${selectionBar?.count ?: 0} 项"
    } else {
        TAB_TITLES[selectedTab]
    }
    var shownTitle by remember { mutableStateOf(barTitle) }
    val titleSwapAlpha = remember { Animatable(1f) }
    var prevSelection by remember { mutableStateOf(selectionActive) }
    LaunchedEffect(barTitle, selectionActive) {
        val selectionChanged = selectionActive != prevSelection
        prevSelection = selectionActive
        if (shownTitle == barTitle) return@LaunchedEffect
        if (selectionChanged) {
            titleSwapAlpha.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            shownTitle = barTitle
            titleSwapAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        } else {

            shownTitle = barTitle
        }
    }

    var actionsSnapshot by remember { mutableStateOf<SelectionBarState?>(null) }
    LaunchedEffect(selectionBar) {
        if (selectionBar != null) actionsSnapshot = selectionBar
    }

    val topActionsContent: (@Composable () -> Unit) = {
        AnimatedContent(
            targetState = selectionActive,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(200)) +
                        scaleIn(animationSpec = tween(220), initialScale = 0.9f)
                    ).togetherWith(fadeOut(animationSpec = tween(140)))
                    .using(SizeTransform(clip = false) { _, _ -> tween(200, easing = FastOutSlowInEasing) })
            },
            contentAlignment = Alignment.Center,
            label = "topActions",
        ) { selecting ->
            if (selecting) {

                val bar = selectionBar ?: actionsSnapshot
                val extra = bar?.singleAction
                val selectAll = bar?.selectAllAction
                Row {
                    if (selectAll != null) {
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                selectAll.onClick()
                            },
                        ) {
                            Icon(
                                imageVector = selectAll.icon,
                                contentDescription = selectAll.label,
                                modifier = Modifier.size(22.dp),
                                tint = (selectAll.tint).takeOrElse { MiuixTheme.colorScheme.onSurface },
                            )
                        }

                        if (extra != null) Spacer(Modifier.width(10.dp))
                    }

                    if (extra != null) {
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                extra.onClick()
                            },
                        ) {
                            Icon(
                                imageVector = extra.icon,
                                contentDescription = extra.label,
                                modifier = Modifier.size(22.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {


                    0 -> Unit

                    1 -> GlassIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            addMenuExpanded = !addMenuExpanded
                        },
                        modifier = Modifier.glassPopupAnchor(
                            anchor = addMenuAnchor,
                            cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                            floating = true,
                        ),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "添加歌单",
                            modifier = Modifier.size(23.dp).glassPopupAnchorContent(addMenuAnchor),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }

                    2 -> GlassIconButton(
                        onClick = {
                            if (MusicLibrary.history.value.isEmpty()) return@GlassIconButton
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            confirmClearHistory = true
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = "清空历史记录",
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }

                    3 -> {
                        val helpTooltip = rememberTooltipState()
                        val density = LocalDensity.current
                        val helpTooltipProvider = remember(density) {
                            object : PopupPositionProvider {
                                override fun calculatePosition(
                                    anchorBounds: IntRect,
                                    windowSize: IntSize,
                                    layoutDirection: LayoutDirection,
                                    popupContentSize: IntSize,
                                ): IntOffset {
                                    val marginPx = with(density) { 8.dp.roundToPx() }
                                    val spacingPx = with(density) { 8.dp.roundToPx() }
                                    val x = (windowSize.width - popupContentSize.width - marginPx)
                                        .coerceAtLeast(marginPx)
                                    val y = (anchorBounds.bottom + spacingPx)
                                        .coerceAtMost(windowSize.height - popupContentSize.height - marginPx)
                                        .coerceAtLeast(marginPx)
                                    return IntOffset(x, y)
                                }
                            }
                        }
                        TooltipBox(
                            positionProvider = helpTooltipProvider,
                            tooltip = {
                                AppTooltip(
                                    title = "操作提示",
                                    lines = listOf(
                                        "长按播放界面的专辑图片可将图片保存到相册",
                                        "长按更多选项顶部的歌曲信息可复制歌曲名称",
                                        "长按播放键可进入沉浸模式",
                                        "左右滑动播放栏中部可切换歌曲",
                                        "长按歌单详情页的图片区域可更换歌单封面",
                                    ),
                                    maxWidth = 336.dp,
                                )
                            },
                            state = helpTooltip,
                        ) {
                            GlassIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    scope.launch { helpTooltip.show() }
                                },
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Help,
                                    contentDescription = "操作提示",
                                    modifier = Modifier.size(22.dp),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

                Scaffold(
                    modifier = Modifier
                        .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                    containerColor = if (wallpaperActive.hasImage) Color.Transparent else MiuixTheme.colorScheme.surface,
                    topBar = {
                        PagerTopBarScrollSync(
                            bar = barScrollBehavior,
                            pagerState = pagerState,
                            pages = pageScrollBehaviors,
                        )
                        GlassTopAppBar(
                            title = shownTitle,
                            largeTitle = shownTitle,

                            contentModifier = Modifier
                                .padding(start = if (wideScreen) railInset else 0.dp)
                                .glassSearchBarTransition(searchExpanded) { 1f },

                            expandable = !wideScreen,
                            subtitle = "",
                            backdrop = if (advancedMaterial) backdrop else null,
                            progressiveBlur = advancedMaterial,
                            scrollBehavior = barScrollBehavior,

                            titleAlpha = {
                                titleSwapAlpha.value * if (wideScreen) railState.progress else 1f
                            },
                            navigationIcon = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectionActive,
                                    enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.8f),
                                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.85f),
                                ) {
                                    GlassIconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        exitSelection()
                                    }) {
                                        Icon(
                                            imageVector = MiuixIcons.Close,
                                            contentDescription = "取消多选",
                                            modifier = Modifier.size(22.dp),
                                            tint = MiuixTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            },
                            actions = { topActionsContent() },
                        )
                    },
                    bottomBar = {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    MiniPlayerHeight + MiniPlayerToNavBarSpacing +
                                        (if (wideScreen) 0.dp else BottomBarAreaHeight),
                                ),
                        )
                    },
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    if (wallpaperActive.hasImage) Color.Transparent else backgroundColor,
                                ),
                        )


                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .fixedWallpaperRegion(wallpaperBackdrop),
                        )
                        HorizontalPager(
                            state = pagerState,

                            modifier = Modifier.fillMaxSize().padding(start = contentStartInset),

                            flingBehavior = pagerFlingBehavior,

                            beyondViewportPageCount = 3,
                            userScrollEnabled = !selectionActive,
                        ) { tab ->
                            Box(modifier = Modifier.fillMaxSize()) {
                            when (tab) {
                            0 -> ExploreScreen(
                                onPlaySongs = { list, index -> playList(list, index) },
                                onMore = { song -> openMore(song, null, "删除歌曲") },
                                onOpenArtist = { name, id -> pushRoute(MusicRoute.Artist(name, id)) },
                                onOpenAlbum = { name, artist, id ->
                                    pushRoute(MusicRoute.Album(name, artist, id))
                                },
                                onOpenFavorites = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Favorites())
                                },
                                onOpenDownloads = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Library())
                                },
                                onOpenRadio = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    ListenMoeController.enter()
                                    playerState.expand()
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                active = !overlayVisible && !playerState.isExpanded,
                                scrollBehavior = exploreScrollBehavior,
                            )

                            2 -> HistoryScreen(
                                onPlay = { list, index -> playList(list, index) },
                                onMore = { song ->
                                    openMore(song, { s -> confirmDeleteHistory = s }, "从历史记录中删除")
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = historyScrollBehavior,
                                onSelectionChange = { selectionBar = it },
                                onBatchAddToPlaylist = { songs -> pickerSongs = songs },
                                onBatchAddToQueue = { songs ->
                                    songs.forEach { PlaybackController.addToQueue(it) }
                                    Toast.makeText(
                                        context,
                                        "已添加 ${songs.size} 首到播放队列",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                onBatchRemoveHistory = { songs ->
                                    songs.forEach { MusicLibrary.removeHistory(it.id) }
                                    Toast.makeText(
                                        context,
                                        "已删除 ${songs.size} 条历史记录",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )

                            1 -> PlaylistScreen(
                                onOpen = { playlist -> pushRoute(MusicRoute.PlaylistDetail(playlistId = playlist.id)) },
                                onCreate = {
                                    createForSongs = emptyList()
                                    showCreateDialog = true
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = playlistScrollBehavior,
                                onRename = { playlist -> renameTarget = playlist },
                                onSelectionChange = { selectionBar = it },
                                onBatchDelete = { list -> confirmDeletePlaylists = list },
                            )

                            else -> SettingsScreen(
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = settingsScrollBehavior,
                                hyperBackdrop = wallpaperBackdrop,
                                onOpenWallpaper = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Wallpaper())
                                },
                                onOpenPlayerSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.PlayerSettings())
                                },
                                onOpenPlaybackSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.PlaybackSettings())
                                },
                                onOpenSourceSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.SourceSettings())
                                },
                                onOpenDownloadSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.DownloadSettings())
                                },
                                onOpenDataSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.DataSettings())
                                },
                                onOpenAbout = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.AboutApp())
                                },
                            )
                            }
                            }
                        }

                    }
                }
            }
            entry<MusicRoute.Artist> { current ->
                NavPage { pageBackdrop ->
                    ArtistScreen(
                        artistName = current.name,
                        artistId = current.id,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song -> openMore(song, null, "删除歌曲") },
                        onOpenAlbum = { album, id ->
                            pushRoute(MusicRoute.Album(album, current.name, id))
                        },
                    )
                }
            }
            entry<MusicRoute.Album> { current ->
                NavPage { pageBackdrop ->
                    AlbumScreen(
                        albumName = current.name,
                        artistName = current.artist,
                        albumId = current.albumId,
                        songId = current.songId,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song -> openMore(song, null, "删除歌曲") },
                    )
                }
            }
            entry<MusicRoute.PlaylistDetail> { current ->
                NavPage { pageBackdrop ->
                    PlaylistDetailScreen(
                        playlistId = current.playlistId,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song, removable ->
                            openMore(
                                song,
                                if (removable) {
                                    { s -> MusicLibrary.removeSongFromPlaylist(current.playlistId, s.id) }
                                } else null,
                                "从歌单移除",
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                        onExport = {
                            val p = MusicLibrary.playlist(current.playlistId)
                            if (p == null || p.songs.isEmpty()) {
                                Toast.makeText(context, "歌单为空，无法导出", Toast.LENGTH_SHORT).show()
                            } else {
                                exportJson = PlaylistImporter.exportJson(p)
                                val safeName = p.name
                                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                                    .ifBlank { "playlist" }
                                exportLauncher.launch("$safeName.json")
                            }
                        },
                    )
                }
            }
            entry<MusicRoute.OnlinePlaylist> { current ->
                NavPage { pageBackdrop ->
                    OnlinePlaylistScreen(
                        playlistId = current.playlistId,
                        playlistName = current.playlistName,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song -> openMore(song, null, "删除歌曲") },
                    )
                }
            }
            entry<MusicRoute.Wallpaper> {
                NavPage { pageBackdrop ->
                    WallpaperScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.PlayerSettings> {
                NavPage { pageBackdrop ->
                    PlayerSettingsScreen(
                        onBack = { popRoute() },
                        onOpenLyricsSettings = {
                            pushRoute(MusicRoute.LyricsSettings())
                        },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.LyricsSettings> {
                NavPage { pageBackdrop ->
                    LyricsSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.PlaybackSettings> {
                NavPage { pageBackdrop ->
                    PlaybackSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.SourceSettings> {
                NavPage { pageBackdrop ->
                    SourceSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.DownloadSettings> {
                NavPage { pageBackdrop ->
                    DownloadSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.DataSettings> {
                NavPage { pageBackdrop ->
                    DataSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.Favorites> {
                NavPage { pageBackdrop ->
                    FavoritesScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song ->
                            openMore(
                                song,
                                { s -> MusicLibrary.removeFavorite(s.id) },
                                "取消收藏",
                                fromFavorites = true,
                                deleteIcon = MiuixIcons.Glass.NotInterested,
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                    )
                }
            }
            entry<MusicRoute.Library> {
                NavPage { pageBackdrop ->
                    LibraryScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song ->
                            openMore(
                                song,
                                { s -> downloadDeleteConfirm = listOf(s) },
                                "删除歌曲",
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                        onBatchAddToPlaylist = { songs -> pickerSongs = songs },
                        onBatchAddToQueue = { songs ->
                            songs.forEach { PlaybackController.addToQueue(it) }
                            Toast.makeText(
                                context,
                                "已添加 ${songs.size} 首到播放队列",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onRequestDelete = { startDownloadDelete(it) },
                        onCloudDelete = { songs ->
                            scope.launch {
                                var deleted = 0
                                songs.forEach { song ->
                                    if (WebDavApi.delete(song).isSuccess) deleted++
                                }
                                Toast.makeText(
                                    context,
                                    if (deleted > 0) "已从云端删除 $deleted 首歌曲" else "云端删除失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                downloadsRefreshKey++
                            }
                        },
                        refreshSignal = downloadsRefreshKey,
                    )
                }
            }
            entry<MusicRoute.AboutApp> {
                NavPage { pageBackdrop ->
                    AboutAppScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
        }
        }

        navDisplay(Modifier.fillMaxSize())
        }
        }


        if (wideScreen && !searchExpanded) {
            HyperNavigationRail(
                selectedTab = selectedTab,
                onTabSelected = selectTab,
                onOpenFavorites = { pushRoute(MusicRoute.Favorites()) },
                onOpenLibrary = { pushRoute(MusicRoute.Library()) },
                onOpenSearch = { searchExpanded = true },
                state = railState,
                backdrop = navDisplayBackdrop,
            )
        }

        GlassSearchOverlay(
            expanded = searchExpanded,
            onDismissRequest = { searchExpanded = false },
            backdrop = searchPageBackdrop,
            label = "搜索歌曲、歌手、专辑和歌单...",
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            backEnabled = !playerState.isExpanded,
            modifier = Modifier.bottomContentFade(
                startFromBottom = MiniPlayerToNavBarSpacing +
                    (if (wideScreen) 0.dp else BottomBarAreaHeight) + 16.dp,
                bottomAlpha = 0.12f,
            ),
        ) { q ->
            HyperSearchScreen(
                query = q,
                onQueryText = { searchQuery = it },
                active = searchExpanded,
                onPlaySongs = { list, index ->
                    playList(list, index)
                    searchExpanded = false
                },
                onMore = { song -> openMore(song, null, "删除歌曲") },
                onOpenArtist = { name, id ->
                    searchExpanded = false
                    playerState.collapse()
                    pushRoute(MusicRoute.Artist(name, id))
                },
                onOpenAlbum = { name, artist, id ->
                    searchExpanded = false
                    playerState.collapse()
                    pushRoute(MusicRoute.Album(name, artist, id))
                },
                onOpenPlaylist = { playlist ->
                    searchExpanded = false
                    playerState.collapse()
                    pushRoute(
                        MusicRoute.OnlinePlaylist(
                            playlistId = playlist.id,
                            playlistName = playlist.name,
                        )
                    )
                },
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val dockFraction = BottomFloatingWidthFraction

            AnimatedVisibility(
                visible = !selectionActive && route !is MusicRoute.Wallpaper,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(240)) { it / 2 } + fadeOut(tween(160)),
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(

                            start = if (wideScreen) {
                                railInset + 30.dp * railState.progress
                            } else {
                                0.dp
                            },
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    HyperMiniPlayer(
                        state = playerState,
                        backdrop = activeBackdrop,
                        onExpand = {
                            if (playerState.track.hasSong) {
                                playerState.expand()
                            } else {
                                Toast.makeText(context, "当前播放列表为空", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenQueue = { showQueueSheet = true },
                        widthFraction = MiniPlayerWidthFraction,
                        modifier = Modifier
                            .offset(y = miniPlayerShift)
                            .padding(

                                bottom = if (wideScreen) {
                                    MiniPlayerToNavBarSpacing + 12.dp
                                } else {
                                    BottomBarAreaHeight + MiniPlayerToNavBarSpacing
                                },
                            ),
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = route == null && !selectionActive && !searchExpanded && !wideScreen,

                    enter = slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(220)),
                    exit = slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(160)),
                ) {

                    val showSearchButton = selectedTab == 0 || selectedTab == 1
                    HyperBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = selectTab,
                        hyperBackdrop = activeBackdrop,
                        widthFraction = dockFraction,
                        trailingAction = if (showSearchButton) {
                            {
                                GlassIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        searchExpanded = true
                                    },
                                    size = BottomFloatingHeight,
                                    backdrop = if (advancedMaterial) activeBackdrop else null,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Glass.Search,
                                        contentDescription = "搜索",
                                        modifier = Modifier.size(26.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }

                AnimatedVisibility(
                    visible = selectionActive,
                    enter = fadeIn(tween(200, delayMillis = 70)) +
                        scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.88f),
                    exit = fadeOut(tween(150)) +
                        scaleOut(tween(200), targetScale = 0.92f),
                ) {
                    selectionBar?.let { bar ->
                        SelectionActionBar(
                            state = bar,
                            backdrop = activeBackdrop,

                            modifier = Modifier.padding(
                                start = if (wideScreen) {
                                    railInset + 30.dp * railState.progress
                                } else {
                                    0.dp
                                },
                            ),
                            widthFraction = BottomFloatingWidthFraction,
                        )
                    }
                }
            }
        }

            CreatePlaylistDialog(
                show = showCreateDialog,
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    if (name.isNotBlank()) {
                        val playlist: Playlist = MusicLibrary.createPlaylist(name.trim())
                        createForSongs.forEach { MusicLibrary.addSongToPlaylist(playlist.id, it) }
                        createForSongs = emptyList()
                        showCreateDialog = false
                        Toast.makeText(context, "已创建「${playlist.name}」", Toast.LENGTH_SHORT).show()
                    }
                },
                backdrop = activeBackdrop,
            )

            CreatePlaylistDialog(
                show = renameTarget != null,
                title = "重命名歌单",
                initialName = renameTarget?.name.orEmpty(),
                confirmText = "保存",
                onDismiss = { renameTarget = null },
                onConfirm = { name ->
                    val target = renameTarget
                    if (target != null && name.isNotBlank()) {
                        MusicLibrary.renamePlaylist(target.id, name.trim())
                        renameTarget = null
                        Toast.makeText(context, "已重命名", Toast.LENGTH_SHORT).show()
                    }
                },
                backdrop = activeBackdrop,
            )

            TrackMoreSheet(
                song = moreSong,
                onDismiss = { moreSong = null },
                showFavoriteOption = !moreFromFavorites,
                onPlayNext = {
                    PlaybackController.playNext(it)
                    Toast.makeText(context, "下一首将播放该歌曲", Toast.LENGTH_SHORT).show()
                },
                onAddToQueue = {
                    PlaybackController.addToQueue(it)
                    Toast.makeText(context, "已添加到队列", Toast.LENGTH_SHORT).show()
                },
                onAddToPlaylist = { song ->
                    scope.launch {
                        kotlinx.coroutines.delay(120)
                        pickerSongs = listOf(song)
                    }
                },
                onAddToFavorite = { song ->
                    MusicLibrary.toggleFavoriteWithToast(context, song)
                },
                onOpenAlbum = { song ->

                    playerState.collapse()
                    searchExpanded = false

                    pushRoute(
                        MusicRoute.Album(
                            name = song.albumText,
                            artist = song.artists.firstOrNull() ?: "",
                            albumId = song.albumId,
                            songId = song.id,
                        ),
                    )
                },
                onOpenArtist = { song ->
                    playerState.collapse()
                    searchExpanded = false
                    when {
                        song.artists.size > 1 -> artistPickerSong = song
                        song.artists.isEmpty() -> pushRoute(MusicRoute.Artist(song.artistText))
                        else -> pushRoute(
                            MusicRoute.Artist(
                                song.artists.first(),
                                song.artistIds.firstOrNull().orEmpty(),
                            ),
                        )
                    }
                },
                onChangeQuality = { showQualitySheet = true },
                onOpenWith = { song ->
                    appPickerKind = AppPickerKind.OpenWith
                    appPickerSong = song
                },
                onEditMetadata = { song ->
                    appPickerKind = AppPickerKind.Metadata
                    appPickerSong = song
                },
                onDownload = { song ->
                    Toast.makeText(context, "开始下载…", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val result = Downloader.download(context, song)
                        val message = when (result) {
                            is Downloader.Result.Success -> "下载完成"
                            is Downloader.Result.Failure -> result.message
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
                onShare = { song ->
                    val text = "${song.name} - ${song.artistText}\n${StreamApi.shareUrl(song)}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, song.name)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享歌曲"))
                },
                onOpenOriginalVideo = { song ->
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(StreamApi.shareUrl(song)))
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "无法打开原视频", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = moreDelete,
                deleteLabel = moreDeleteLabel,
                deleteIcon = moreDeleteIcon,
                onUploadToCloud = { song ->
                    if (!WebDavApi.configured()) {
                        Toast.makeText(context, "请先配置 WebDAV 服务器", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "正在上传到云端…", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val result = WebDavApi.upload(context, song)
                            val message = result.fold(
                                onSuccess = { "已上传到云端" },
                                onFailure = { it.message ?: "上传失败" },
                            )
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            downloadsRefreshKey++
                        }
                    }
                },
            )

            PlaylistPickerSheet(
                songs = pickerSongs,
                onDismiss = { pickerSongs = emptyList() },
                onPick = { songs, playlist ->
                    var added = 0
                    songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                    val message = when {
                        added == 0 -> "该歌单已有这首歌"
                        songs.size == 1 -> "已添加到「${playlist.name}」"
                        added == songs.size -> "已添加 ${added} 首到「${playlist.name}」"
                        else -> "已添加 ${added} 首，其余已在「${playlist.name}」"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    pickerSongs = emptyList()
                },
                onCreateNew = { songs ->
                    pickerSongs = emptyList()
                    createForSongs = songs
                    showCreateDialog = true
                },
            )

            PlaylistAddMenuPopup(
                expanded = addMenuExpanded,
                anchor = addMenuAnchor,
                anchorContent = {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(23.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
                backdrop = activeBackdrop,
                onDismiss = { addMenuExpanded = false },
                onImportLink = {
                    linkImportBusy = false
                    linkImportStatus = null
                    linkImportError = null
                    showLinkImport = true
                },
                onImportJson = {
                    jsonLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "application/octet-stream",
                            "*/*",
                        ),
                    )
                },
                onCreate = {
                    createForSongs = emptyList()
                    showCreateDialog = true
                },
            )

            PlaylistLinkImportDialog(
                show = showLinkImport,
                busy = linkImportBusy,
                status = linkImportStatus,
                error = linkImportError,
                backdrop = activeBackdrop,
                onDismiss = {

                    linkImportJob?.cancel()
                    linkImportJob = null
                    linkImportBusy = false
                    showLinkImport = false
                    linkImportStatus = null
                    linkImportError = null
                },
                onImport = { link ->
                    linkImportJob?.cancel()
                    linkImportJob = scope.launch {
                        linkImportBusy = true
                        linkImportError = null
                        linkImportStatus = "获取歌单…"
                        try {
                            val res = PlaylistImporter.importFromLink(link) { done, total ->
                                linkImportStatus = "匹配网易云歌曲 $done/$total"
                            }
                            val playlist = MusicLibrary.createPlaylist(res.name)
                            var added = 0
                            res.songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                            if (res.cover.isNotBlank()) {
                                PlaylistCoverStore.importFromUrl(playlist.id, res.cover)?.let { path ->
                                    MusicLibrary.setPlaylistCover(playlist.id, path)
                                }
                            }
                            Toast.makeText(context, "已添加歌单，共${added}首歌曲", Toast.LENGTH_LONG).show()
                            linkImportBusy = false
                            linkImportStatus = null
                            showLinkImport = false
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            linkImportBusy = false
                            linkImportStatus = null
                            linkImportError = e.message ?: "导入失败"
                        } finally {
                            linkImportJob = null
                        }
                    }
                },
            )

            PlaylistJsonImportDialog(
                show = showJsonImport,
                busy = jsonImportBusy,
                status = jsonImportStatus,
                error = jsonImportError,
                backdrop = activeBackdrop,
                onDismiss = {
                    jsonImportJob?.cancel()
                    jsonImportJob = null
                    jsonImportBusy = false
                    jsonImportStatus = null
                    jsonImportError = null
                    showJsonImport = false
                },
            )

            ArtistPickerSheet(
                song = artistPickerSong,
                onDismiss = { artistPickerSong = null },
                onPick = { name, id -> pushRoute(MusicRoute.Artist(name, id)) },

                backdrop = activeBackdrop,
            )

            PlaybackQueueSheet(
                visible = showQueueSheet,
                onDismiss = { showQueueSheet = false },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = confirmDeletePlaylists.isNotEmpty(),
                title = "删除歌单",
                summary = if (confirmDeletePlaylists.size == 1) {
                    "确定要删除「${confirmDeletePlaylists.first().name}」吗？此操作不可恢复"
                } else {
                    "确定要删除选中的 ${confirmDeletePlaylists.size} 个歌单吗？此操作不可恢复"
                },
                confirmText = "删除",
                onConfirm = {
                    val targets = confirmDeletePlaylists
                    confirmDeletePlaylists = emptyList()
                    val deleteAll = targets.size > 1 && targets.size >= MusicLibrary.playlists.value.size
                    when {
                        targets.isEmpty() -> Unit
                        targets.size == 1 -> MusicLibrary.deletePlaylist(targets.first().id)
                        deleteAll -> MusicLibrary.deletePlaylists(targets.map { it.id })
                        else -> scope.launch {
                            targets.forEachIndexed { i, playlist ->
                                if (i > 0) delay(90)
                                MusicLibrary.deletePlaylist(playlist.id)
                            }
                        }
                    }
                    Toast.makeText(
                        context,
                        if (targets.size == 1) "已删除「${targets.first().name}」"
                        else "已删除 ${targets.size} 个歌单",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onDismiss = { confirmDeletePlaylists = emptyList() },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = confirmDeleteHistory != null,
                title = "删除历史记录",
                summary = "确定要从历史记录中删除「${confirmDeleteHistory?.name.orEmpty()}」吗？",
                confirmText = "删除",
                onConfirm = {
                    val target = confirmDeleteHistory
                    confirmDeleteHistory = null
                    if (target != null) MusicLibrary.removeHistory(target.id)
                },
                onDismiss = { confirmDeleteHistory = null },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = downloadDeleteConfirm != null,
                title = "删除歌曲",
                summary = if ((downloadDeleteConfirm?.size ?: 0) == 1) {
                    "确认从下载文件夹中删除这首歌吗？"
                } else {
                    "确认从下载文件夹中删除 ${downloadDeleteConfirm?.size ?: 0} 首歌曲吗？"
                },
                confirmText = "删除",
                onConfirm = {
                    val targets = downloadDeleteConfirm ?: emptyList()
                    downloadDeleteConfirm = null
                    startDownloadDelete(targets)
                },
                onDismiss = { downloadDeleteConfirm = null },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = confirmClearHistory,
                title = "清空历史记录",
                summary = "确定要清空全部历史播放记录吗？",
                confirmText = "清空",
                onConfirm = {
                    confirmClearHistory = false
                    MusicLibrary.clearHistory()
                },
                onDismiss = { confirmClearHistory = false },
                backdrop = activeBackdrop,
            )

            AppPickerSheet(
                visible = appPickerSong != null,
                kind = appPickerKind,
                song = appPickerSong,
                onDismiss = { appPickerSong = null },
            )

            QualitySheet(
                visible = showQualitySheet,
                onDismiss = { showQualitySheet = false },
                onPick = { quality ->
                    MusicLibrary.setQuality(quality)
                    PlaybackController.reloadCurrent()
                    showQualitySheet = false
                },
            )

            SleepTimerSheet(
                visible = showSleepTimer,
                onDismiss = { showSleepTimer = false },
            )

        HyperPlayerLayer(
            state = playerState,
            contentBackdrop = playerPageLayer,
            onOpenQueue = { showQueueSheet = true },
            onOpenSleepTimer = { showSleepTimer = true },
            onMore = {
                val current = PlaybackController.currentSong()
                if (current != null) openMore(current, null, "删除歌曲")
            },
        )

        }
    }
    }
}

@Composable
private fun CreatePlaylistDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    backdrop: Backdrop? = null,
    title: String = "新建歌单",
    initialName: String = "",
    confirmText: String = "创建",
) {
    var name by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(show) {
        if (show) name = initialName
    }

    val dismiss: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onDismiss()
    }
    val confirm: (String) -> Unit = { value ->
        keyboard?.hide()
        focusManager.clearFocus()
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onConfirm(value)
    }

    GlassDialog(
        title = title,
        show = show,
        onDismissRequest = dismiss,
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                NativeTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "歌单名称",
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        dismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = confirmText,
                    onClick = { confirm(name.trim()) },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QualitySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (AudioQuality) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val current = MusicLibrary.settings.value.quality

    HyperSheet(visible = visible, onDismiss = onDismiss, title = "音质") {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetCard {
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onPick(quality) }
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = quality.label,
                            color = if (quality == current) scheme.primary else scheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = quality.bitrateText,
                            color = if (quality == current) scheme.primary else scheme.onSurfaceSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun PagerTopBarScrollSync(
    bar: ScrollBehavior,
    pagerState: PagerState,
    pages: List<ScrollBehavior>,
) {
    val collapse = remember(pagerState, pages) {
        derivedStateOf {
            val last = pages.lastIndex
            if (last <= 0) return@derivedStateOf floatArrayOf(0f, 0f, 0f)
            val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, last.toFloat())
            val from = floor(position).toInt().coerceIn(0, last)
            val to = (from + 1).coerceAtMost(last)
            val t = (position - from).coerceIn(0f, 1f)
            val a = pages[from].state
            val b = pages[to].state
            floatArrayOf(
                a.heightOffset + (b.heightOffset - a.heightOffset) * t,
                a.contentOffset + (b.contentOffset - a.contentOffset) * t,
                a.overscrollOffset + (b.overscrollOffset - a.overscrollOffset) * t,
            )
        }
    }.value
    LaunchedEffect(bar, pages) {

        var frames = 0
        while (frames < 8) {
            val limit = bar.state.heightOffsetLimit

            if (limit > -Float.MAX_VALUE) {
                pages.forEach { page ->
                    if (page.state.heightOffsetLimit != limit) page.state.heightOffsetLimit = limit
                }
                break
            }
            withFrameNanos { }
            frames++
        }
    }
    SideEffect {
        val state = bar.state
        val limit = state.heightOffsetLimit

        pages.forEach { page ->
            if (page.state.heightOffsetLimit != limit) page.state.heightOffsetLimit = limit
        }

        if (state.heightOffset != collapse[0]) state.heightOffset = collapse[0]
        if (state.contentOffset != collapse[1]) state.contentOffset = collapse[1]
        if (state.overscrollOffset != collapse[2]) state.overscrollOffset = collapse[2]
    }
}
