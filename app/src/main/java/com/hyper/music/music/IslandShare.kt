package com.hyper.music.music

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import org.json.JSONObject

@OptIn(UnstableApi::class)
class IslandShareNotificationProvider(context: Context) : MediaNotification.Provider {

    private val delegate = DefaultMediaNotificationProvider(context)

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val created = delegate.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback,
        )
        IslandShare.attach(created.notification)
        return created
    }

    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = delegate.handleCustomCommand(mediaSession, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.getNotificationChannelInfo()
}

object IslandShare {

    const val EXTRA_ISLAND_MEDIA = "miui.focus.param.media"

    private const val NETEASE_URL = "https://music.163.com/song?id="

    fun attach(notification: Notification) {
        val extras = notification.extras
        val song = PlaybackController.currentSong()
        if (song == null || ListenMoeController.state.value.active) {
            extras.remove(EXTRA_ISLAND_MEDIA)
            return
        }
        val link = shareLink(song)
        if (link == null) {
            extras.remove(EXTRA_ISLAND_MEDIA)
            return
        }
        val shareData = JSONObject()
            .put("title", song.name)
            .put("content", song.artistText)
            .put("shareContent", link)
        val island = JSONObject()
            .put("param_v2", JSONObject().put("param_island", JSONObject().put("shareData", shareData)))
        extras.putString(EXTRA_ISLAND_MEDIA, island.toString())
    }

    private fun shareLink(song: Song): String? {
        if (song.isLocal || song.id.isBlank()) return null
        return when (song.source) {
            "netease" -> song.id.toLongOrNull()?.let { NETEASE_URL + it }
            BiliApi.SOURCE -> BiliApi.pageUrl(song.id)
            else -> null
        }
    }
}
