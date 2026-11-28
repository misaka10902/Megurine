package moe.megurine.music.ui.effects.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import moe.megurine.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin



@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val surface = MiuixTheme.colorScheme.surface
    val darkTarget = if (isAppDarkTheme()) 1f else 0f
    val mix by animateFloatAsState(
        targetValue = darkTarget,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "auroraDarkMix",
    )


    val colorStage = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var target = floor(colorStage.value) + 1f
        while (isActive) {
            val period = AuroraPresets.Light.colorInterpPeriod +
                (AuroraPresets.Dark.colorInterpPeriod - AuroraPresets.Light.colorInterpPeriod) * mix
            delay((period * 500).toLong())
            colorStage.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 35f))
            target += 1f
        }
    }


    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val origin = withFrameNanos { it }
        var last = origin
        val minDelta = 1_000_000_000L / 60L
        while (isActive) {
            val now = withFrameNanos { it }
            if (now - last < minDelta) continue
            last = now
            clock.floatValue = (now - origin) / 1_000_000_000f
        }
    }

    val palette = remember { FloatArray(16) }

    Canvas(modifier = modifier) {
        drawRect(surface)
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        blendPalette(AuroraPresets.Light, AuroraPresets.Dark, colorStage.value, mix, palette)

        val time = clock.floatValue
        val points = AuroraPresets.Light.points
        val offset = AuroraPresets.Light.pointOffset +
            (AuroraPresets.Dark.pointOffset - AuroraPresets.Light.pointOffset) * mix
        val scale = minOf(w, h)

        for (i in 0 until 4) {
            val srcX = points[i * 3]
            val srcY = points[i * 3 + 1]
            val animX = srcX + sin(time + srcY) * offset
            val animY = srcY + cos(time + animX) * offset
            val center = Offset(animX * w, (1f - animY) * h)
            val radius = points[i * 3 + 2] * 1.12f * scale
            val base = Color(
                red = palette[i * 4],
                green = palette[i * 4 + 1],
                blue = palette[i * 4 + 2],
                alpha = palette[i * 4 + 3],
            )

            val tint = base.copy(alpha = base.alpha * 0.62f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to tint,
                        0.45f to tint.copy(alpha = tint.alpha * 0.92f),
                        1f to tint.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

private class AuroraPreset(
    val points: FloatArray,
    val colors1: FloatArray,
    val colors2: FloatArray,
    val colors3: FloatArray,
    val colorInterpPeriod: Float,
    val pointOffset: Float,
)

private object AuroraPresets {
    val Light = AuroraPreset(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f, 0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f, 0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f,
        ),
        colorInterpPeriod = 5.0f,
        pointOffset = 0.2f,
    )

    val Dark = AuroraPreset(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f,
        ),
        colorInterpPeriod = 8.0f,
        pointOffset = 0.4f,
    )
}


private fun paletteOf(preset: AuroraPreset, index: Int): FloatArray = when (index.mod(4)) {
    1 -> preset.colors1
    3 -> preset.colors3
    else -> preset.colors2
}


private fun blendPalette(
    light: AuroraPreset,
    dark: AuroraPreset,
    stage: Float,
    mix: Float,
    out: FloatArray,
) {
    val base = floor(stage).toInt()
    val fraction = stage - base
    val lightStart = paletteOf(light, base)
    val lightEnd = paletteOf(light, base + 1)
    val darkStart = paletteOf(dark, base)
    val darkEnd = paletteOf(dark, base + 1)
    for (i in 0 until 16) {
        val l = lightStart[i] + (lightEnd[i] - lightStart[i]) * fraction
        val d = darkStart[i] + (darkEnd[i] - darkStart[i]) * fraction
        out[i] = l + (d - l) * mix
    }
}
