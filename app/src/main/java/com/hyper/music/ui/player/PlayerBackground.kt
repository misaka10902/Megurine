package com.hyper.music.ui.player

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.effects.glass.AuroraBackground
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private val PlayerBackgroundFallback = Color(0xFF6E6E73)

@Composable
internal fun PlayerBackground(
    expandProgress: Float,
    flowMode: Boolean,
    flowDark: Boolean,
    coverBitmap: ImageBitmap?,
    palette: AlbumPalette?,
    advancedMaterial: Boolean,
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

    val tint = remember(palette, flowDark) { palette?.flowTint(flowDark) }
    val flowFallback = if (flowDark) Color(0xFF101014) else Color(0xFFF5F5FA)
    val tintBase by animateColorAsState(
        targetValue = tint?.base?.copy(alpha = 1f) ?: flowFallback,
        animationSpec = fade,
        label = "flowBase",
    )
    val tintA by animateColorAsState(
        targetValue = tint?.points?.get(0) ?: Color.Transparent,
        animationSpec = fade,
        label = "flowPoint0",
    )
    val tintB by animateColorAsState(
        targetValue = tint?.points?.get(1) ?: Color.Transparent,
        animationSpec = fade,
        label = "flowPoint1",
    )
    val tintC by animateColorAsState(
        targetValue = tint?.points?.get(2) ?: Color.Transparent,
        animationSpec = fade,
        label = "flowPoint2",
    )
    val tintD by animateColorAsState(
        targetValue = tint?.points?.get(3) ?: Color.Transparent,
        animationSpec = fade,
        label = "flowPoint3",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = expandProgress.coerceIn(0f, 1f) },
    ) {
        Crossfade(
            targetState = flowMode,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "playerBgMode",
        ) { flow ->
            if (flow) {
                FlowLayer(
                    dark = flowDark,
                    tints = if (tint != null) listOf(tintA, tintB, tintC, tintD) else null,
                    base = tintBase,
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
private fun FlowLayer(
    dark: Boolean,
    tints: List<Color>?,
    base: Color?,
) {
    val surface = base?.copy(alpha = 1f) ?: if (dark) Color(0xFF101014) else Color(0xFFF5F5FA)
    Box(Modifier.fillMaxSize().background(surface)) {
        AuroraBackground(
            modifier = Modifier.fillMaxSize(),
            darkMode = dark,
            tints = tints,
            baseColor = surface,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to if (dark) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                        0.45f to Color.Transparent,
                        1f to if (dark) Color.Black.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.12f),
                    )
                ),
        )
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
