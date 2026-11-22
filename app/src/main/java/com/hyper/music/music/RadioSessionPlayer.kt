package com.hyper.music.music

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
internal class RadioSessionPlayer(player: Player) : ForwardingSimpleBasePlayer(player) {

    fun refresh() {
        invalidateState()
    }

    override fun getState(): SimpleBasePlayer.State {
        val base = super.getState()
        val radio = ListenMoeController.state.value
        if (!radio.active) return base
        return runCatching { radioState(base, radio) }.getOrDefault(base)
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady && ListenMoeController.state.value.active) {
            getPlayer().seekToDefaultPosition()
        }
        return super.handleSetPlayWhenReady(playWhenReady)
    }

    private fun radioState(base: SimpleBasePlayer.State, radio: ListenMoeState): SimpleBasePlayer.State {
        val index = base.currentMediaItemIndex
        val items = base.getPlaylist()
        if (items.isEmpty() || index !in items.indices) return base

        val song = radio.song
        val durationMs = song?.durationMs ?: 0L
        val metadata = (items[index].mediaMetadata ?: MediaMetadata.EMPTY).buildUpon()
        if (song != null) {
            metadata.setTitle(song.title)
            metadata.setArtist(song.artist.ifBlank { "LISTEN.moe" })
            if (song.album.isNotBlank()) metadata.setAlbumTitle(song.album)
            if (song.cover.isNotBlank()) runCatching { metadata.setArtworkUri(Uri.parse(song.cover)) }
            if (song.durationMs > 0L) metadata.setDurationMs(song.durationMs)
        }
        val updatedMetadata = metadata.build()

        val updated = items.mapIndexed { itemIndex, item ->
            if (itemIndex != index) {
                item
            } else {
                val builder = item.buildUpon()
                    .setMediaMetadata(updatedMetadata)
                    .setMediaItem(item.mediaItem.buildUpon().setMediaMetadata(updatedMetadata).build())
                    .setLiveConfiguration(null)
                    .setIsSeekable(true)
                if (durationMs > 0L) builder.setDurationUs(durationMs * 1000L)
                builder.build()
            }
        }

        val position = PlaybackController.radioPositionMs()
        val clamped = if (durationMs > 0L) position.coerceIn(0L, durationMs) else position
        val commands = base.availableCommands.buildUpon()
            .remove(Player.COMMAND_SEEK_TO_NEXT)
            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_BACK)
            .remove(Player.COMMAND_SEEK_FORWARD)
            .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
            .remove(Player.COMMAND_SET_REPEAT_MODE)
            .remove(Player.COMMAND_SET_SHUFFLE_MODE)
            .build()

        return base.buildUpon()
            .setPlaylist(updated)
            .setContentPositionMs(clamped)
            .setAvailableCommands(commands)
            .build()
    }
}
