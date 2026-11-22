package com.hyper.music.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal data class FlowTint(
    val base: Color,
    val points: List<Color>,
)

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(h, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f)),
        )
    )
}

private fun Color.hsvTriple(): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(toArgb(), it) }

internal fun AlbumPalette.flowTint(dark: Boolean): FlowTint {
    val sampled = if (flow.size >= 4) flow.take(4) else listOf(dominant, secondary, accent, top)
    val hsv = sampled.map { it.hsvTriple() }
    val baseHue = hsv[0][0]
    val baseSaturation = hsv[0][1].coerceAtLeast(0.24f)

    return if (dark) {
        FlowTint(
            base = hsvColor(baseHue, baseSaturation * 0.55f, 0.10f),
            points = listOf(
                hsvColor(hsv[0][0], hsv[0][1].coerceAtLeast(0.32f), 0.62f).copy(alpha = 0.68f),
                hsvColor(hsv[1][0], hsv[1][1].coerceAtLeast(0.24f), 0.50f).copy(alpha = 0.60f),
                hsvColor(hsv[2][0], hsv[2][1].coerceAtLeast(0.28f), 0.66f).copy(alpha = 0.48f),
                hsvColor(hsv[3][0], hsv[3][1].coerceAtLeast(0.22f), 0.38f).copy(alpha = 0.56f),
            ),
        )
    } else {
        FlowTint(
            base = hsvColor(baseHue, baseSaturation * 0.12f, 0.99f),
            points = listOf(
                hsvColor(hsv[0][0], hsv[0][1].coerceAtLeast(0.34f), 1.00f).copy(alpha = 0.92f),
                hsvColor(hsv[1][0], hsv[1][1].coerceAtLeast(0.28f), 0.98f).copy(alpha = 0.86f),
                hsvColor(hsv[2][0], hsv[2][1].coerceAtLeast(0.30f), 1.00f).copy(alpha = 0.76f),
                hsvColor(hsv[3][0], hsv[3][1].coerceAtLeast(0.24f), 0.95f).copy(alpha = 0.70f),
            ),
        )
    }
}
