package moe.megurine.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import moe.megurine.music.music.CoverSize
import moe.megurine.music.music.PlayMode
import moe.megurine.music.music.PlaybackController
import moe.megurine.music.music.PlaybackUiState
import moe.megurine.music.music.StreamApi
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PlayerTrack(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?,
    val lyricId: String = "",
    val hasSong: Boolean,
)

class HyperPlayerState(
    private val coroutineScope: CoroutineScope,
) {
    private val expandAnimatable = Animatable(0f)
    private var dragRangePx = 1f
    private var dragJob: Job? = null

    val expandProgress: Float get() = expandAnimatable.value

    var isExpanded: Boolean by mutableStateOf(false)
        private set

    
    var immersiveMode: Boolean by mutableStateOf(false)
        private set

    var playback: PlaybackUiState by mutableStateOf(PlaybackUiState())

    val track: PlayerTrack
        get() {
            val song = playback.song
            return PlayerTrack(
                title = song?.name ?: "未播放歌曲",
                artist = song?.artistText ?: "快去邂逅合拍的节奏吧",
                album = song?.albumText ?: "",
                durationMs = if (playback.durationMs > 0) playback.durationMs else (song?.durationMs ?: 0L),
                coverUrl = song?.let {
                    StreamApi.coverUrl(it, CoverSize.LARGE.request).ifBlank { null }
                },
                lyricId = song?.lyricId.orEmpty(),
                hasSong = song != null,
            )
        }

    val isPlaying: Boolean get() = playback.isPlaying
    val positionMs: Long get() = playback.positionMs

    fun togglePlayPause() = PlaybackController.toggle()
    fun previous() = PlaybackController.previous()
    fun next() = PlaybackController.next()
    fun seekTo(ms: Long) = PlaybackController.seekTo(ms)
    fun cyclePlayMode() = PlaybackController.cyclePlayMode()

    
    fun toggleImmersiveMode(): Boolean {
        immersiveMode = !immersiveMode
        return immersiveMode
    }

    val playMode: PlayMode get() = playback.mode

    fun expand() {
        if (isExpanded) return
        isExpanded = true
        animateTo(1f, ExpandSpec)
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        animateTo(0f, CollapseSpec)
    }

    fun dragBy(deltaPx: Float, containerHeightPx: Float) {
        if (!isExpanded) return
        val range = containerHeightPx.absoluteValue.coerceAtLeast(1f)
        dragRangePx = range
        val target = (expandProgress - deltaPx / range).coerceIn(0f, 1f)
        dragJob?.cancel()
        dragJob = coroutineScope.launch { expandAnimatable.snapTo(target) }
    }

    fun endDrag(velocityY: Float) {
        if (!isExpanded) return
        val shouldCollapse = velocityY > 900f || expandProgress < 0.62f
        isExpanded = !shouldCollapse
        val target = if (shouldCollapse) 0f else 1f

        val progressVelocity = (-velocityY / dragRangePx).coerceIn(-2.2f, 2.2f)
        animateTo(target, if (shouldCollapse) CollapseSpec else ExpandSpec, progressVelocity)
    }

    private fun animateTo(target: Float, spec: AnimationSpec<Float>, initialVelocity: Float = 0f) {
        coroutineScope.launch { expandAnimatable.animateTo(target, spec, initialVelocity = initialVelocity) }
    }

    companion object {

        private val ExpandSpec: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 380f,
            visibilityThreshold = 0.0001f,
        )
        private val CollapseSpec: AnimationSpec<Float> = spring(
            dampingRatio = 1f,
            stiffness = 460f,
            visibilityThreshold = 0.0001f,
        )
    }
}

@Composable
fun rememberHyperPlayerState(): HyperPlayerState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { HyperPlayerState(scope) }

    val playback by PlaybackController.state.collectAsState()
    state.playback = playback

    return state
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = (ms / 1000f).roundToLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
