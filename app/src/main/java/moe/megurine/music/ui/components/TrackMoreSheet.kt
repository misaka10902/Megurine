package moe.megurine.music.ui.components

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.music.CoverLoader
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.MeloxBridge
import moe.megurine.music.music.MetadataEditBridge
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.NetCover
import moe.megurine.music.music.Playlist
import moe.megurine.music.music.Song
import moe.megurine.music.music.StreamApi
import moe.megurine.music.ui.effects.miuix.rememberPopupBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Edit
import top.yukonga.miuix.kmp.icon.glass.GridView
import top.yukonga.miuix.kmp.icon.glass.OverseasMusic
import top.yukonga.miuix.kmp.icon.glass.UploadCloud
import top.yukonga.miuix.kmp.icon.glass.Video
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.ReplyAll
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import moe.megurine.music.R
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

private val ArtistPickerRowHeight = 52.dp

@Composable
fun HyperSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    insideMargin: DpSize = DpSize(12.dp, 0.dp),
    followIme: Boolean = false,

    immersive: Boolean = false,

    contentScrolls: Boolean = false,

    backdrop: Backdrop? = null,

    progressiveBlur: Boolean = false,

    scrollOffset: (() -> Float)? = null,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    headerBottomContent: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val context = LocalContext.current
    val navBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val gestureNavigation = remember(context) {
        runCatching {
            Settings.Secure.getInt(context.contentResolver, "navigation_mode", 2) == 2
        }.getOrDefault(true)
    }
    val bottomGap = when {
        navBottom <= 0.dp -> 0.dp

        contentScrolls -> navBottom
        gestureNavigation -> navBottom + 12.dp
        else -> navBottom
    }

    val popupBackdrop = rememberPopupBackdrop()
    val hyperBackdrop = backdrop ?: popupBackdrop

    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    GlassBottomSheet(
        show = visible,
        onDismissRequest = onDismiss,
        backdrop = if (advancedMaterial) hyperBackdrop else null,

        modifier = modifier,
        progressiveBlur = progressiveBlur,
        scrollOffset = scrollOffset,
        title = title,
        startAction = startAction,
        endAction = endAction,
        headerBottomContent = headerBottomContent ?: {},
        cornerRadius = 32.dp,
        insideMargin = insideMargin,
        outsideMargin = DpSize(0.dp, 0.dp),

        defaultWindowInsetsPadding = followIme,
        content = { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (immersive) Modifier else Modifier.padding(bottom = bottomGap)),
            ) {
                content(innerPadding)
            }
        },
    )
}

@Composable
fun SheetHeader(
    text: String,
    modifier: Modifier = Modifier,
    standardTitle: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (standardTitle) {
                    Modifier.padding(top = 6.dp, bottom = 12.dp)
                } else {
                    Modifier.padding(top = 2.dp, bottom = 6.dp).offset(y = (-10).dp)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = if (standardTitle) MiuixTheme.textStyles.title4.fontSize else 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun SheetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
fun TrackMoreSheet(
    song: Song?,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToFavorite: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenArtist: (Song) -> Unit,
    onChangeQuality: (Song) -> Unit,
    onOpenWith: (Song) -> Unit,
    onEditMetadata: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onOpenOriginalVideo: (Song) -> Unit,
    onDelete: ((Song) -> Unit)? = null,
    deleteLabel: String = "删除歌曲",
    deleteIcon: ImageVector = MiuixIcons.Delete,
    showFavoriteOption: Boolean = true,
    onUploadToCloud: (Song) -> Unit = {},
) {
    var lastSong by remember { mutableStateOf<Song?>(null) }
    if (song != null) lastSong = song
    val scheme = MiuixTheme.colorScheme
    val context = LocalContext.current
    val settings by MusicLibrary.settings.collectAsState()
    val quality = settings.quality
    val favorites by MusicLibrary.favorites.collectAsState()
    var showComments by remember { mutableStateOf(false) }
    LaunchedEffect(song) { if (song != null) showComments = false }

    HyperSheet(visible = song != null, onDismiss = onDismiss, contentScrolls = true) {
        val target = lastSong ?: return@HyperSheet
        val isFavorite = favorites.any { it.id == target.id }
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = showComments,
            transitionSpec = {
                ContentTransform(
                    initialContentExit = fadeOut(animationSpec = tween(150)),
                    targetContentEnter = fadeIn(animationSpec = tween(210)),
                    sizeTransform = SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ -> tween(240, easing = FastOutSlowInEasing) },
                    ),
                )
            },
            label = "trackMoreComments",
        ) { inComments ->
        if (inComments) {
            CommentsContent(song = target, onBack = { showComments = false })
        } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .pointerInput(target.name) {
                        detectTapGestures(
                            onLongPress = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("歌曲名称", target.name))
                                Toast.makeText(context, "已复制歌曲名称", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NetCover(
                        url = StreamApi.coverUrl(target, CoverSize.SMALL.request).ifBlank { null },
                        modifier = Modifier.size(56.dp),
                        maxSize = CoverSize.SMALL.decode,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            color = scheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (target.isBili && settings.showSourceTags) {
                                SourceBadge(text = "B站", container = SourceColorBili)
                                Spacer(Modifier.width(5.dp))
                            } else if (target.isJoox && settings.showSourceTags) {
                                SourceBadge(text = "JOOX", container = SourceColorJoox)
                                Spacer(Modifier.width(5.dp))
                            } else if (target.isCloud) {
                                SourceBadge(text = "云端", container = SourceColorCloud)
                                Spacer(Modifier.width(5.dp))
                            } else if (target.isLocal) {
                                SourceBadge(text = "本地", container = SourceColorLocal)
                                Spacer(Modifier.width(5.dp))
                            } else if (settings.showSourceTags) {
                                SourceBadge(text = "网易", container = SourceColorNetease)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = if (target.isBili) target.artistText else target.subtitle,
                                color = scheme.onSurfaceSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                MenuEntry(MiuixIcons.Add, "下一首播放") { onDismiss(); onPlayNext(target) }
                MenuEntry(MiuixIcons.Playlist, "加入队列") { onDismiss(); onAddToQueue(target) }
                MenuEntry(MiuixIcons.AddCircle, "加入歌单") { onDismiss(); onAddToPlaylist(target) }
                if (showFavoriteOption) {
                    MenuEntry(
                        icon = if (isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        text = if (isFavorite) "取消收藏" else "加入收藏",
                    ) { onDismiss(); onAddToFavorite(target) }
                }

                if (target.isLocal) {
                    val cloudReady = settings.webdavUrl.isNotBlank()
                    MenuEntry(
                        icon = MiuixIcons.Glass.UploadCloud,
                        text = "上传到云端",
                        tint = if (cloudReady) {
                            scheme.onSurface
                        } else {
                            scheme.onSurfaceSecondary.copy(alpha = 0.4f)
                        },
                        enabled = cloudReady,
                    ) {
                        onDismiss()
                        onUploadToCloud(target)
                    }
                    MenuEntry(MiuixIcons.Glass.GridView, "在其他应用打开") {
                        onDismiss()
                        onOpenWith(target)
                    }
                    MenuEntry(MiuixIcons.Glass.Edit, "元数据编辑") {
                        onDismiss()
                        onEditMetadata(target)
                    }
                }

                if (target.isCloud) {

                } else if (target.isBili) {
                    MenuEntry(MiuixIcons.Glass.Video, "查看原视频") { onDismiss(); onOpenOriginalVideo(target) }
                    MenuEntry(MiuixIcons.Share, "分享") { onDismiss(); onShare(target) }
                } else if (!target.isLocal) {

                    if (!target.isJoox) {
                        MenuEntry(MiuixIcons.Album, "专辑：${target.albumText}") { onDismiss(); onOpenAlbum(target) }
                        MenuEntry(MiuixIcons.Contacts, "艺术家：${target.artistText}") { onDismiss(); onOpenArtist(target) }
                    }
                    MenuEntry(MiuixIcons.Music, "音质：${quality.label} · ${quality.bitrateText}") { onDismiss(); onChangeQuality(target) }
                    if (!target.isJoox) {
                        MenuEntry(MiuixIcons.ReplyAll, "查看评论") { showComments = true }
                    }
                    MenuEntry(MiuixIcons.Download, "下载到本地") { onDismiss(); onDownload(target) }
                    MenuEntry(MiuixIcons.Share, "分享") { onDismiss(); onShare(target) }
                }
                if (onDelete != null) {
                    MenuEntry(deleteIcon, deleteLabel, tint = scheme.error) {
                        onDismiss()
                        onDelete(target)
                    }
                }
            }
        }
        }
        }
        }
        }
    }
}

enum class AppPickerKind { OpenWith, Metadata }

@Composable
fun AppPickerSheet(
    visible: Boolean,
    kind: AppPickerKind,
    song: Song?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    HyperSheet(visible = visible && song != null, onDismiss = onDismiss) {
        val target = song
        SheetHeader("选择应用", standardTitle = true)
        SheetCard {
            when (kind) {
                AppPickerKind.OpenWith -> {
                    AppEntry(
                        icon = MiuixIcons.Glass.OverseasMusic,
                        text = "Melox",
                        iconSize = AppIconSize * 1.18f,
                    ) {
                        onDismiss()
                        if (target != null) launchInApp(context, target, MeloxBridge.PACKAGE, "Melox")
                    }
                    AppEntry(painterResource(R.drawable.ic_app_halcyon), "Halcyon") {
                        onDismiss()
                        if (target != null) launchInApp(context, target, MeloxBridge.HALCYON_PACKAGE, "Halcyon")
                    }
                    AppEntry(painterResource(R.drawable.ic_app_megurine_music), "椒盐音乐", iconSize = 20.dp) {
                        onDismiss()
                        if (target != null) launchInApp(context, target, MeloxBridge.SALT_PACKAGE, "椒盐音乐")
                    }
                }

                AppPickerKind.Metadata -> {
                    AppEntry(painterResource(R.drawable.ic_app_lyrico), "Lyrico") {
                        onDismiss()
                        target?.let { launchEditor(context, it, MetadataEditBridge.Target.LYRICO, "Lyrico") }
                    }
                    AppEntry(painterResource(R.drawable.ic_app_music_tags), "音乐标签") {
                        onDismiss()
                        target?.let { launchEditor(context, it, MetadataEditBridge.Target.MUSIC_TAGS, "音乐标签") }
                    }
                    AppEntry(painterResource(R.drawable.ic_app_salt_analysis), "椒盐音频分析") {
                        onDismiss()
                        target?.let { launchEditor(context, it, MetadataEditBridge.Target.SALT_ANALYSIS, "椒盐音频分析") }
                    }
                    AppEntry(painterResource(R.drawable.ic_app_musictagger), "音签 MusicTagger") {
                        onDismiss()
                        target?.let { launchEditor(context, it, MetadataEditBridge.Target.MUSIC_TAGGER, "音签 MusicTagger") }
                    }
                }
            }
        }
    }
}

private fun launchInApp(context: Context, song: Song, packageName: String, label: String) {
    when (MeloxBridge.play(context, song, packageName)) {
        MeloxBridge.Result.NotInstalled ->
            Toast.makeText(context, "还没有安装$label", Toast.LENGTH_SHORT).show()
        MeloxBridge.Result.Failed ->
            Toast.makeText(context, "无法在${label}中打开", Toast.LENGTH_SHORT).show()
        MeloxBridge.Result.Launched -> Unit
    }
}

private fun launchEditor(context: Context, song: Song, target: MetadataEditBridge.Target, label: String) {
    when (MetadataEditBridge.edit(context, song, target)) {
        MetadataEditBridge.Result.NotInstalled ->
            Toast.makeText(context, "还没有安装$label", Toast.LENGTH_SHORT).show()
        MetadataEditBridge.Result.Failed ->
            Toast.makeText(context, "无法在${label}中编辑该歌曲", Toast.LENGTH_SHORT).show()
        MetadataEditBridge.Result.Launched -> Unit
    }
}

@Composable
private fun AppEntry(
    icon: ImageVector,
    text: String,
    iconSize: Dp = AppIconSize,
    onClick: () -> Unit,
) {
    AppEntryContent(text, onClick) {
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun AppEntry(
    icon: androidx.compose.ui.graphics.painter.Painter,
    text: String,
    iconSize: Dp = AppIconSize,
    onClick: () -> Unit,
) {
    AppEntryContent(text, onClick) {
        androidx.compose.foundation.Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
        )
    }
}

private val AppIconSize = 24.dp

@Composable
private fun AppEntryContent(
    text: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
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
        Box(Modifier.size(AppIconSize), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuEntry(
    icon: ImageVector,
    text: String,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            colorFilter = ColorFilter.tint(tint.copy(alpha = 0.92f)),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlaylistPickerSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onPick: (List<Song>, Playlist) -> Unit,
    onCreateNew: (List<Song>) -> Unit,
) {
    var lastSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    if (songs.isNotEmpty()) lastSongs = songs
    val list by MusicLibrary.playlists.collectAsState()
    val scheme = MiuixTheme.colorScheme

    HyperSheet(visible = songs.isNotEmpty(), onDismiss = onDismiss, contentScrolls = true) {
        val target = lastSongs
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader("加入歌单")
            SheetCard {
                MenuEntry(MiuixIcons.Add, "新建歌单", fontSize = 17.sp) {
                    onDismiss(); onCreateNew(target)
                }
                if (list.isEmpty()) {
                    Text(
                        text = "还没有歌单，先新建一个吧",
                        color = scheme.onSurfaceSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    )
                } else {
                    list.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onPick(target, playlist); onDismiss() }
                                .padding(horizontal = 4.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = playlist.name,
                                color = scheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${playlist.songs.size} 首",
                                color = scheme.onSurfaceSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistPickerSheet(
    song: Song?,
    onDismiss: () -> Unit,
    onPick: (String, String) -> Unit,
    backdrop: Backdrop? = null,
) {
    var lastSong by remember { mutableStateOf<Song?>(null) }
    if (song != null) lastSong = song
    val scheme = MiuixTheme.colorScheme

    HyperSheet(visible = song != null, onDismiss = onDismiss, backdrop = backdrop, contentScrolls = true) {
        val target = lastSong ?: return@HyperSheet
        val visible = song != null
        val listState = rememberLazyListState()
        var avatars by remember(target.id) { mutableStateOf<Map<Int, String>>(emptyMap()) }
        LaunchedEffect(target.id, visible) {
            if (!visible) return@LaunchedEffect
            avatars = emptyMap()
            target.artists.forEachIndexed { index, artist ->
                launch {
                    val url = runCatching {
                        StreamApi.artistAvatar(artist, target.artistIds.getOrNull(index).orEmpty())
                    }.getOrDefault("")
                    if (url.isNotBlank()) avatars = avatars + (index to url)
                }
            }
        }
        LaunchedEffect(target.id) {
            if (target.artists.isNotEmpty()) listState.scrollToItem(0)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val listMax = if (maxHeight.value.isFinite()) {
                (maxHeight - 58.dp).coerceAtLeast(150.dp)
            } else {
                420.dp
            }
            val cardHeight = (ArtistPickerRowHeight * target.artists.size + 12.dp)
                .coerceAtMost(listMax)
            Column(modifier = Modifier.fillMaxWidth()) {
                SheetHeader("选择歌手", standardTitle = true)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surfaceContainer),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
                        itemsIndexed(target.artists) { index, artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        onPick(artist, target.artistIds.getOrNull(index).orEmpty())
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ArtistPickerAvatar(url = avatars[index].orEmpty())
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = artist,
                                    color = scheme.onSurface,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPickerAvatar(url: String) {
    val scheme = MiuixTheme.colorScheme
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    val alpha = remember(url) { Animatable(0f) }
    LaunchedEffect(url) {
        if (url.isBlank()) return@LaunchedEffect
        val loaded = CoverLoader.load(url, 180)
        if (loaded != null) {
            bitmap = loaded
            alpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            imageVector = MiuixIcons.ContactsCircle,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            colorFilter = ColorFilter.tint(scheme.onSurface.copy(alpha = 0.92f)),
        )
        val image = bitmap
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { this.alpha = alpha.value },
            )
        }
    }
}
