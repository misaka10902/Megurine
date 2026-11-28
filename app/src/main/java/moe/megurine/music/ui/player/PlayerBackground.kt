package moe.megurine.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private val PlayerBackgroundFallback = Color(0xFF6E6E73)

@Composable
internal fun PlayerBackground(
    flowMode: Boolean,
    flowDark: Boolean,
    coverBitmap: ImageBitmap?,
    palette: AlbumPalette?,
    advancedMaterial: Boolean,
    playing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val fade = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)
    val topColor by animateColorAsState(
        targetValue = palette?.top ?: PlayerBackgroundFallback.copy(alpha = 0.92f),
        animationSpec = fade,
        label = "bgTop",
    )
    val middleColor by animateColorAsState(
        targetValue = palette?.middle ?: PlayerBackgroundFallback.copy(alpha = 0.55f),
        animationSpec = fade,
        label = "bgMiddle",
    )
    val bottomColor by animateColorAsState(
        targetValue = palette?.bottom ?: Color.Black,
        animationSpec = fade,
        label = "bgBottom",
    )
    val flowState = rememberDynamicFlowBackgroundState()

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Crossfade(
            targetState = flowMode,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "playerBgMode",
        ) { flow ->
            if (flow) {
                DynamicFlowBackground(
                    state = flowState,
                    artwork = remember(coverBitmap) { coverBitmap?.asAndroidBitmap() },
                    animate = playing,
                    modifier = Modifier.fillMaxSize(),
                    dark = flowDark,
                )
            } else {
                CoverBlurLayer(
                    coverBitmap = coverBitmap,
                    advancedMaterial = advancedMaterial,
                    topColor = topColor,
                    middleColor = middleColor,
                    bottomColor = bottomColor,
                )
            }
        }
    }
}

@Composable
private fun CoverBlurLayer(
    coverBitmap: ImageBitmap?,
    advancedMaterial: Boolean,
    topColor: Color,
    middleColor: Color,
    bottomColor: Color,
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Transparent)
        drawContent()
    }
    val soft = advancedMaterial && coverBitmap != null
    val topAlpha = if (soft) 0.82f else 1f
    val middleAlpha = if (soft) 0.78f else 1f
    val bottomAlpha = if (soft) 0.86f else 1f

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .layerBackdrop(backdrop),
        ) {
            Crossfade(
                targetState = coverBitmap,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "playerBgCover",
            ) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().scale(1.2f),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0xFF6E6E73),
                                    1f to Color(0xFF1C1C1E),
                                )
                            ),
                    )
                }
            }
        }

        if (advancedMaterial) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = { blur(48f.dp.toPx()) },
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to topColor.copy(alpha = topAlpha),
                        0.42f to middleColor.copy(alpha = middleAlpha),
                        1f to bottomColor.copy(alpha = bottomAlpha),
                    )
                ),
        )
    }
}
