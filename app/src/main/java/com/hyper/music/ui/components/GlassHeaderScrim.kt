package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GlassHeaderScrim(
    backdrop: Backdrop,
    advanced: Boolean,
    strength: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    if (advanced) {
        val blendColors = remember(scheme.surface) {
            listOf(BlendColorEntry(scheme.surface.copy(alpha = 0.3f)))
        }
        val colors = BlurDefaults.blurColors(blendColors = blendColors)
        Box(
            modifier = modifier
                .graphicsLayer { alpha = strength }
                .progressiveTextureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                    blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                    colors = colors,
                    enabled = strength > 0.01f,
                ),
        )
    } else {
        Box(
            modifier = modifier
                .graphicsLayer { alpha = strength }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to scheme.surface.copy(alpha = 0.94f),
                            0.5f to scheme.surface.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    )
                },
        )
    }
}
