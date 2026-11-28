package moe.megurine.music.music

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.DefaultMediaCodecAdapterFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import moe.megurine.music.R
import moe.megurine.music.ui.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var radioPlayer: RadioSessionPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(IslandShareNotificationProvider(this))

        val exo = createExo(MusicLibrary.settings.value.highPrecisionOutput)
        player = exo

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val initialRadioPlayer = RadioSessionPlayer(exo)
        radioPlayer = initialRadioPlayer
        ListenMoeController.onSessionInvalidate = { mainHandler.post { radioPlayer?.refresh() } }

        session = MediaSession.Builder(this, initialRadioPlayer)
            .setSessionActivity(openApp)
            .setCallback(HyperMediaSessionCallback())
            .build()

        updateCustomLayout()
        serviceScope.launch {
            while (isActive) {
                delay(1_000)
                if (ListenMoeController.state.value.active) radioPlayer?.refresh()
            }
        }

        serviceScope.launch {
            var lastPrecision = MusicLibrary.settings.value.highPrecisionOutput
            MusicLibrary.settings.collect { settings ->
                if (settings.highPrecisionOutput != lastPrecision) {
                    lastPrecision = settings.highPrecisionOutput
                    rebuildPlayer(settings.highPrecisionOutput)
                }
            }
        }
        serviceScope.launch {
            combine(
                PlaybackController.state,
                MusicLibrary.favorites,
                ListenMoeController.state,
                MusicLibrary.settings,
            ) { st, fav, radio, settings ->
                val id = st.song?.id
                RadioLayoutKey(
                    mode = st.mode,
                    songId = id,
                    liked = id != null && fav.any { it.id == id },
                    radio = radio.active,
                    hideIcons = settings.hideControllerIcons,
                    rightIcon = settings.controllerRightIcon,
                )
            }.distinctUntilChanged().collect { updateCustomLayout() }
        }

        PlaybackController.attach(exo)
    }

    
    private fun createExo(highPrecision: Boolean): ExoPlayer {
        val mediaSourceFactory = ProgressiveMediaSource.Factory(
            MediaCache.dataSourceFactory(this),
            ExtractorsFactory {
                arrayOf(
                    Mp3Extractor(),
                    FragmentedMp4Extractor(SubtitleParser.Factory.UNSUPPORTED),
                    Mp4Extractor(SubtitleParser.Factory.UNSUPPORTED),
                    FlacExtractor(),
                    OggExtractor(),
                    WavExtractor(),
                    AdtsExtractor(),
                    AmrExtractor(),
                    Ac3Extractor(),
                )
            },
        )

        mediaSourceFactory.setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                300,
                1500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(this, AudioOnlyRenderersFactory(this, highPrecision), mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            )

            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }



    private fun rebuildPlayer(highPrecision: Boolean) {
        val oldExo = player ?: return

        val radioStation = ListenMoeController.state.value.takeIf { it.active }?.station

        val items = (0 until oldExo.mediaItemCount).map(oldExo::getMediaItemAt)
        val index = oldExo.currentMediaItemIndex
        val position = oldExo.currentPosition.coerceAtLeast(0L)
        val wasPlaying = oldExo.playWhenReady
        val params = oldExo.playbackParameters

        PlaybackController.saveSession(blocking = true)

        PlaybackController.detach(keepRadio = radioStation != null)

        val newExo = createExo(highPrecision)
        if (items.isNotEmpty()) {
            newExo.setMediaItems(items, index.coerceAtMost(items.lastIndex), position)
            newExo.prepare()
        }
        newExo.playbackParameters = params
        newExo.playWhenReady = wasPlaying

        val newRadioPlayer = RadioSessionPlayer(newExo)
        radioPlayer = newRadioPlayer
        ListenMoeController.onSessionInvalidate = { mainHandler.post { newRadioPlayer.refresh() } }
        session?.setPlayer(newRadioPlayer)

        player = newExo

        PlaybackController.attach(newExo)

        oldExo.release()
        updateCustomLayout()
    }

    @Suppress("DEPRECATION")
    private fun buildCustomLayout(): List<CommandButton> {
        if (ListenMoeController.state.value.active) return emptyList()
        val settings = MusicLibrary.settings.value
        if (settings.hideControllerIcons) return emptyList()
        val state = PlaybackController.state.value
        val songId = state.song?.id
        val liked = songId != null && MusicLibrary.isFavorite(songId)
        val closeApp = settings.controllerRightIcon == MusicSettings.CONTROLLER_ICON_CLOSE
        val (modeIcon, modeLabel) = when (state.mode) {
            PlayMode.ORDER -> R.drawable.ic_island_mode_order to "列表循环"
            PlayMode.SHUFFLE -> R.drawable.ic_island_mode_shuffle to "随机播放"
            PlayMode.REPEAT_ONE -> R.drawable.ic_island_mode_repeat_one to "单曲循环"
        }
        return listOf(
            CommandButton.Builder()
                .setDisplayName(modeLabel)
                .setIconResId(modeIcon)
                .setSessionCommand(SessionCommand(ACTION_CYCLE_MODE, Bundle.EMPTY))
                .setExtras(
                    Bundle().apply {
                        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 0)
                    }
                )
                .build(),
            CommandButton.Builder()
                .setDisplayName(if (closeApp) "关闭应用" else if (liked) "取消收藏" else "收藏")
                .setIconResId(
                    when {
                        closeApp -> R.drawable.ic_island_close
                        liked -> R.drawable.ic_island_favorite_fill
                        else -> R.drawable.ic_island_favorite
                    }
                )
                .setSessionCommand(
                    SessionCommand(
                        if (closeApp) ACTION_CLOSE_APP else ACTION_TOGGLE_FAVORITE,
                        Bundle.EMPTY,
                    )
                )
                .setExtras(
                    Bundle().apply {
                        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 2)
                    }
                )
                .build(),
        )
    }

    @Suppress("DEPRECATION")
    private fun updateCustomLayout() {
        session?.setCustomLayout(buildCustomLayout())
    }

    private inner class HyperMediaSessionCallback : MediaSession.Callback {

        @Suppress("DEPRECATION")
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_CYCLE_MODE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CLOSE_APP, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(buildCustomLayout())
                .build()
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: Int,
        ): Int {
            if (ListenMoeController.state.value.active && command in RADIO_BLOCKED_COMMANDS) {
                return SessionResult.RESULT_INFO_SKIPPED
            }

            if (!ListenMoeController.state.value.active &&
                session.player.repeatMode == Player.REPEAT_MODE_ONE
            ) {
                when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    -> {
                        PlaybackController.next()
                        return SessionResult.RESULT_INFO_SKIPPED
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    -> {
                        PlaybackController.previous()
                        return SessionResult.RESULT_INFO_SKIPPED
                    }
                }
            }
            if (command == Player.COMMAND_PLAY_PAUSE) {
                if (session.player.isPlaying) {
                    PlaybackController.pause()
                } else {
                    PlaybackController.play()
                }
                return SessionResult.RESULT_INFO_SKIPPED
            }
            return super.onPlayerCommandRequest(session, controller, command)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_CYCLE_MODE -> PlaybackController.cyclePlayMode()
                ACTION_TOGGLE_FAVORITE -> PlaybackController.currentSong()?.let {
                    MusicLibrary.toggleFavorite(it)
                }
                ACTION_CLOSE_APP -> {
                    PlaybackController.saveSession()
                    stopSelf()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }
            updateCustomLayout()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackController.saveSession()
        val p = player
        if (p == null || !p.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        ListenMoeController.onSessionInvalidate = null
        PlaybackController.detach()
        session?.run {
            player.release()
            release()
        }
        session = null
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_CYCLE_MODE = "moe.megurine.music.CYCLE_MODE"
        private const val ACTION_TOGGLE_FAVORITE = "moe.megurine.music.TOGGLE_FAVORITE"
        private const val ACTION_CLOSE_APP = "moe.megurine.music.CLOSE_APP"

        private val RADIO_BLOCKED_COMMANDS = setOf(
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SET_REPEAT_MODE,
            Player.COMMAND_SET_SHUFFLE_MODE,
        )
    }

    private data class RadioLayoutKey(
        val mode: PlayMode,
        val songId: String?,
        val liked: Boolean,
        val radio: Boolean,
        val hideIcons: Boolean,
        val rightIcon: String,
    )
}

@UnstableApi
private class AudioOnlyRenderersFactory(
    private val context: Context,
    private val highPrecision: Boolean,
) : RenderersFactory {
    override fun createRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput,
    ): Array<Renderer> {
        val sinkBuilder = DefaultAudioSink.Builder(context)
        if (highPrecision) {

            sinkBuilder
                .setAudioProcessorChain(PcmPlaybackProcessorChain())
                .setEnableFloatOutput(true)
                .setEnableAudioOutputPlaybackParameters(false)
                .setAudioOutputProvider(
                    HighPrecisionAudioOutputProvider(
                        AudioTrackAudioOutputProvider.Builder(context).build(),
                    ),
                )
        }
        return arrayOf(
            MediaCodecAudioRenderer(
                context,
                DefaultMediaCodecAdapterFactory(context),
                MediaCodecSelector.DEFAULT,
                true,
                eventHandler,
                audioRendererEventListener,
                sinkBuilder.build(),
            )
        )
    }
}



@UnstableApi
private class HighPrecisionAudioOutputProvider(
    private val delegate: AudioOutputProvider,
) : AudioOutputProvider {
    override fun getFormatSupport(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.FormatSupport =
        delegate.getFormatSupport(adapt(formatConfig))

    override fun getOutputConfig(formatConfig: AudioOutputProvider.FormatConfig): AudioOutputProvider.OutputConfig =
        delegate.getOutputConfig(adapt(formatConfig))

    override fun getAudioOutput(outputConfig: AudioOutputProvider.OutputConfig): AudioOutput =
        delegate.getAudioOutput(outputConfig)

    override fun addListener(listener: AudioOutputProvider.Listener) = delegate.addListener(listener)

    override fun removeListener(listener: AudioOutputProvider.Listener) = delegate.removeListener(listener)

    override fun setClock(clock: Clock) = delegate.setClock(clock)

    override fun release() = delegate.release()

    private fun adapt(config: AudioOutputProvider.FormatConfig): AudioOutputProvider.FormatConfig {
        val highResolutionOutput = config.enableHighResolutionPcmOutput &&
            Util.isEncodingHighResolutionPcm(config.format.pcmEncoding)
        return if (highResolutionOutput && !config.enablePlaybackParameters) {
            config.buildUpon().setEnablePlaybackParameters(true).build()
        } else {
            config
        }
    }
}
