package com.hyper.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.music.MusicApi
import com.hyper.music.music.OnlinePlaylistInfo
import com.hyper.music.music.PlaylistCoverStore
import com.hyper.music.music.Song
import com.hyper.music.ui.components.EmptyHint
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.components.TrackRow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal suspend fun saveOnlinePlaylistToLibrary(
    name: String,
    songs: List<Song>,
    coverUrl: String = "",
): Boolean {
    if (songs.isEmpty()) return false
    val playlist = MusicLibrary.createPlaylist(name.ifBlank { "在线歌单" })
    MusicLibrary.addSongsToPlaylist(playlist.id, songs)
    if (coverUrl.isNotBlank()) {
        PlaylistCoverStore.importFromUrl(playlist.id, coverUrl)?.let { path ->
            MusicLibrary.setPlaylistCover(playlist.id, path)
        }
    }
    return true
}

internal suspend fun addOnlinePlaylistToLibrary(
    playlistId: String,
    fallbackName: String,
    fallbackCover: String = "",
): Boolean {
    val info = runCatching { MusicApi.playlistTracks(playlistId) }
        .getOrDefault(OnlinePlaylistInfo("", "", emptyList()))
    return saveOnlinePlaylistToLibrary(
        name = info.name.ifBlank { fallbackName },
        songs = info.songs,
        coverUrl = info.cover.ifBlank { fallbackCover },
    )
}

@Composable
fun OnlinePlaylistScreen(
    playlistId: String,
    playlistName: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme
    val listState = rememberLazyListState()

    var name by remember(playlistId) { mutableStateOf(playlistName) }
    var songs by remember(playlistId) { mutableStateOf<List<Song>>(emptyList()) }
    var cover by remember(playlistId) { mutableStateOf("") }
    var loading by remember(playlistId) { mutableStateOf(true) }
    var adding by remember(playlistId) { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        loading = true
        val info = runCatching { MusicApi.playlistTracks(playlistId) }
            .getOrDefault(OnlinePlaylistInfo("", "", emptyList()))
        if (info.name.isNotBlank()) name = info.name
        cover = info.cover
        songs = info.songs
        loading = false
    }

    val coverUrl = remember(cover) {
        GdApi.sizedCover(cover, CoverSize.SMALL.request).ifBlank { null }
    }

    fun addToLibrary() {
        if (adding) return
        adding = true
        scope.launch {
            val ok = if (songs.isNotEmpty()) {
                saveOnlinePlaylistToLibrary(name, songs, cover)
            } else {
                addOnlinePlaylistToLibrary(playlistId, name, cover)
            }
            adding = false
            Toast.makeText(
                context,
                if (ok) "已添加该歌单" else "添加失败，请重试",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    DetailPage(
        title = name,
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        autoHideTitle = true,
        listState = listState,
        endAction = {
            GlassIconButton(onClick = { addToLibrary() }) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = "添加到歌单",
                    modifier = Modifier.size(25.dp),
                    tint = scheme.onSurface,
                )
            }
        },
        endActionKey = adding,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (coverUrl == null) {
                    EmptyCoverCard(
                        icon = MiuixIcons.Glass.Item,
                        iconAlpha = 0.35f,
                    )
                } else {
                    NetCover(
                        url = coverUrl,
                        modifier = Modifier.size(96.dp),
                        corner = 22.dp,
                        maxSize = CoverSize.SMALL.decode,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = name,
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (loading) "正在加载" else "${songs.size} 首歌曲",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PlayAllButton(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0) })
                Spacer(Modifier.height(8.dp))
            }
        },
        overlay = {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(240)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                RefreshIndicator(text = "正在加载")
            }
        },
    ) {
        if (songs.isEmpty()) {
            if (!loading) item { EmptyHint("歌单没有歌曲") }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    TrackRow(
                        song = song,
                        onClick = { onPlay(songs, index) },
                        onMore = { onMore(song) },
                        contentPadding = PaddingValues(
                            start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp,
                        ),
                    )
                }
            }
        }
    }
}
