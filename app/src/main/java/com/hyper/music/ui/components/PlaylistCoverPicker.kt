package com.hyper.music.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.PlaylistCoverStore
import kotlinx.coroutines.launch

@Composable
fun rememberPlaylistCoverPick(): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetId by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val id = targetId
        if (uri == null || id.isBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            val path = PlaylistCoverStore.importFromUri(id, uri)
            if (path != null) {
                val old = MusicLibrary.playlist(id)?.cover.orEmpty()
                MusicLibrary.setPlaylistCover(id, path)
                PlaylistCoverStore.delete(old)
                Toast.makeText(context, "已更换歌单封面", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "更换失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    return { id ->
        if (id.isNotBlank()) {
            targetId = id
            Toast.makeText(context, "选择一张图片作为歌单封面", Toast.LENGTH_SHORT).show()
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
