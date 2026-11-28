package moe.megurine.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.music.LyricLine
import moe.megurine.music.music.MusicLibrary
import moe.megurine.music.music.PlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.abs

private val LyricsInnerPadding = 8.dp

private const val FOLLOW_RESUME_DELAY_MS = 5_000L

private val LyricScrollSpec = spring<Float>(
    dampingRatio = 1.1f,
    stiffness = 420f,
    visibilityThreshold = 0.5f,
)

@Composable
fun HyperLyricsView(
    lines: List<LyricLine>,
    currentIndex: Int,
    showTranslation: Boolean,
    centered: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    horizontalInset: Dp? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val settings by MusicLibrary.settings.collectAsState()
    val playback by PlaybackController.state.collectAsState()
    val baseFontSize = settings.lyricFontSize
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceIn(0, lines.lastIndex.coerceAtLeast(0)),
    )

    var isUserBrowsing by remember(lines) { mutableStateOf(false) }
    var positioned by remember(lines) { mutableStateOf(false) }
    var followResumeKey by remember(lines) { mutableStateOf(0) }


    val revealAlpha = remember(lines) { Animatable(0f) }
    LaunchedEffect(positioned) {
        if (positioned) revealAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
    }
    val browsingState = rememberUpdatedState(isUserBrowsing)
    val scrollInCode = remember { mutableStateOf(false) }
    val isManualScrolling by remember {
        derivedStateOf { listState.isScrollInProgress && !scrollInCode.value }
    }

    val browsingModifier = Modifier.pointerInput(lines) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var movement = Offset.Zero
            var browsingStarted = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) break
                movement += change.position - change.previousPosition
                if (!browsingStarted) {

                    if (abs(movement.x) > viewConfiguration.touchSlop &&
                        abs(movement.x) >= abs(movement.y)
                    ) break
                    if (abs(movement.y) > viewConfiguration.touchSlop &&
                        abs(movement.y) > abs(movement.x)
                    ) {
                        browsingStarted = true
                        isUserBrowsing = true
                    }
                }
            }
        }
    }

    LaunchedEffect(isManualScrolling, isUserBrowsing, playback.isPlaying) {
        if (!playback.isPlaying || isManualScrolling || !isUserBrowsing) return@LaunchedEffect
        delay(FOLLOW_RESUME_DELAY_MS)
        if (!isManualScrolling && isUserBrowsing) {
            isUserBrowsing = false
            followResumeKey++
        }
    }

    LaunchedEffect(currentIndex, lines, followResumeKey) {
        if (currentIndex < 0 || lines.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        if (isManualScrolling) {
            snapshotFlow { isManualScrolling }.first { !it }
        }
        if (browsingState.value) {

            positioned = true
            return@LaunchedEffect
        }

        val info = listState.layoutInfo
        val target = info.visibleItemsInfo.firstOrNull { it.index == currentIndex }

        if (!positioned) {
            scrollInCode.value = true
            try {
                if (target == null) listState.scrollToItem(currentIndex.coerceAtMost(lines.lastIndex))

                val after = listState.layoutInfo
                val item = after.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                if (item != null) {
                    val d = item.offset + item.size / 2f -
                        (after.viewportStartOffset + after.viewportEndOffset) / 2f
                    if (abs(d) > 0.5f) listState.dispatchRawDelta(d)
                }
            } finally {
                scrollInCode.value = false
            }
            positioned = true
            return@LaunchedEffect
        }

        scrollInCode.value = true
        try {

            if (target == null) {
                listState.animateScrollToItem(currentIndex.coerceAtMost(lines.lastIndex))
            }
            val after = listState.layoutInfo
            val item = after.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                ?: return@LaunchedEffect
            val d = item.offset + item.size / 2f -
                (after.viewportStartOffset + after.viewportEndOffset) / 2f
            if (abs(d) > 0.5f) listState.animateScrollBy(d, LyricScrollSpec)
        } finally {
            scrollInCode.value = false
        }
    }

    val blurEnabled = playback.isPlaying && settings.advancedMaterial && !isUserBrowsing

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(browsingModifier),
    ) {
        val half = maxHeight / 2 - 46.dp

        if (lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无歌词",
                    color = PlayerPagePalette.primary.copy(alpha = 0.6f),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val edgeFade = Brush.verticalGradient(
                0f to Color.Transparent,
                0.14f to Color.Black,
                0.86f to Color.Black,
                1f to Color.Transparent,
            )
            val sideInset = horizontalInset
                ?: if (centered) LyricsInnerPadding else maxWidth * (1f - CoverSizeFraction) / 2f
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen

                        alpha = revealAlpha.value
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = edgeFade, blendMode = BlendMode.DstIn)
                    },
                contentPadding = PaddingValues(
                    start = sideInset,
                    end = sideInset,
                    top = half.coerceAtLeast(0.dp),
                    bottom = half.coerceAtLeast(0.dp),
                ),
                horizontalAlignment = if (centered) {
                    Alignment.CenterHorizontally
                } else {
                    Alignment.Start
                },
            ) {
                items(lines.size, key = { i -> "${lines[i].timeMs}_$i" }) { i ->
                    LyricRow(
                        line = lines[i],
                        distance = abs(i - currentIndex),
                        isCurrent = i == currentIndex,
                        showTranslation = showTranslation,
                        centered = centered,
                        baseFontSize = baseFontSize,
                        blurEnabled = blurEnabled,
                        onSeek = { time ->
                            isUserBrowsing = false
                            onSeek(time)
                        },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricRow(
    line: LyricLine,
    distance: Int,
    isCurrent: Boolean,
    showTranslation: Boolean,
    centered: Boolean,
    baseFontSize: Float,
    blurEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onDismiss: (() -> Unit)?,
) {
    val weightSettings by MusicLibrary.settings.collectAsState()
    val secondary = if (weightSettings.showLyricRoman && line.roman.isNotBlank()) {
        line.roman
    } else {
        line.translation
    }
    val hasTranslation = showTranslation && secondary.isNotBlank()

    val targetAlpha = if (isCurrent) 1f else when (distance) {
        1 -> 0.58f
        2 -> 0.44f
        else -> 0.30f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing),
        label = "lyricRowAlpha",
    )
    val blurDp by animateDpAsState(
        targetValue = if (!blurEnabled || isCurrent) {
            0.dp
        } else {
            (distance * 1.6f).coerceAtMost(5.5f).dp
        },
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "lyricRowBlur",
    )

    val fontSize = baseFontSize.sp
    val translationSize = (baseFontSize * 0.81f).sp
    val mainLineHeight = (baseFontSize * 1.43f).sp
    val translationLineHeight = (baseFontSize * 1.14f).sp
    val baseWeight = weightSettings.lyricFontWeight
    val fontWeight = FontWeight(baseWeight)
    val translationWeight = FontWeight((baseWeight - 100).coerceAtLeast(300))


    val alignFraction = animateFloatAsState(
        targetValue = if (centered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "lyricAlignFraction",
    )
    var mainLayout by remember(line.text) { mutableStateOf<TextLayoutResult?>(null) }
    var secondaryLayout by remember(secondary) { mutableStateOf<TextLayoutResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (isCurrent) onDismiss?.invoke() else onSeek(line.timeMs)
            }
            .padding(vertical = 12.dp)
            .then(if (blurEnabled) Modifier.blur(blurDp) else Modifier),
    ) {
        if (line.text.isNotBlank()) {
            Text(
                text = line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedTextAlignment(mainLayout, alignFraction),
                color = PlayerPagePalette.primary.copy(alpha = alpha),
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = TextAlign.Start,
                lineHeight = mainLineHeight,
                onTextLayout = { mainLayout = it },
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }
        AnimatedVisibility(
            visible = hasTranslation && line.text.isNotBlank(),
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                expandVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ),
            exit = fadeOut(tween(160, easing = LinearOutSlowInEasing)) +
                shrinkVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animatedTextAlignment(secondaryLayout, alignFraction),
                    color = PlayerPagePalette.primary.copy(alpha = alpha * 0.78f),
                    fontSize = translationSize,
                    fontWeight = translationWeight,
                    textAlign = TextAlign.Start,
                    lineHeight = translationLineHeight,
                    onTextLayout = { secondaryLayout = it },
                )
            }
        }
    }
}



private fun Modifier.animatedTextAlignment(
    layout: TextLayoutResult?,
    progress: androidx.compose.runtime.State<Float>,
): Modifier = drawWithContent {
    if (layout == null || layout.lineCount == 0) {
        drawContent()
    } else {
        repeat(layout.lineCount) { lineIndex ->
            val lineLeft = layout.getLineLeft(lineIndex)
            val lineRight = layout.getLineRight(lineIndex)
            val lineWidth = lineRight - lineLeft
            val offset = ((size.width - lineWidth) / 2f - lineLeft) * progress.value
            withTransform({ translate(left = offset) }) {
                clipRect(
                    left = -size.width,
                    top = layout.getLineTop(lineIndex),
                    right = size.width * 2f,
                    bottom = layout.getLineBottom(lineIndex),
                ) {
                    this@drawWithContent.drawContent()
                }
            }
        }
    }
}
