package moe.megurine.music.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.megurine.music.ui.effects.glass.AuroraBackground
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import moe.megurine.music.ui.effects.miuix.rememberSquircleShape
import moe.megurine.music.ui.player.BottomBarAreaHeight
import moe.megurine.music.ui.player.MiniPlayerToNavBarSpacing
import moe.megurine.music.ui.utils.bottomContentFade
import moe.megurine.music.ui.utils.horizontalContentPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Recording
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

@Composable
fun AboutAppScreen(onBack: () -> Unit, backdrop: LayerBackdrop) {
    val context = LocalContext.current

    @Suppress("DEPRECATION")
    val versionName = rememberSafeVersion(context)
    val horizontalPadding = horizontalContentPadding()
    val auroraBackdrop = rememberLayerBackdrop()
    val scheme = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerSpace = if (headerHeightPx > 0) {
        with(density) { headerHeightPx.toDp() }
    } else {
        216.dp
    }
    val collapseSpace = headerSpace + 96.dp
    val collapsePx = with(density) { collapseSpace.toPx() }
    val scrollProgress by remember(listState, collapsePx) {
        derivedStateOf {
            val scrolled = if (listState.firstVisibleItemIndex > 0) {
                collapsePx
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
            (scrolled / collapsePx).coerceIn(0f, 1f)
        }
    }
    val collapseProgress by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "aboutHeaderCollapse",
    )

    DetailPage(
        title = "关于",
        onBack = onBack,
        backdrop = backdrop,

        compactTitle = true,
        autoHideTitle = true,
        hideWallpaper = true,
        activeBackdrop = auroraBackdrop,

        listState = listState,
        listModifier = Modifier.bottomContentFade(
            startFromBottom = MiniPlayerToNavBarSpacing + BottomBarAreaHeight + 16.dp,
            bottomAlpha = 0.12f,
        ),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(auroraBackdrop),
            ) {
                AuroraBackground(modifier = Modifier.fillMaxSize())
            }
        },
        header = {},
        overlay = {
            val versionT = ((collapseProgress - 0.05f) / 0.25f).coerceIn(0f, 1f)
            val titleT = ((collapseProgress - 0.30f) / 0.25f).coerceIn(0f, 1f)
            val iconT = ((collapseProgress - 0.55f) / 0.27f).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp)
                    .onSizeChanged { headerHeightPx = it.height },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                AboutLogoIcon(
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(1f - iconT)
                        .graphicsLayer {
                            scaleX = 1f - iconT * 0.07f
                            scaleY = 1f - iconT * 0.07f
                        },
                    backdrop = auroraBackdrop,
                )
                Spacer(Modifier.height(18.dp))
                AboutLogoTitle(
                    text = "寻音",
                    backdrop = auroraBackdrop,
                    modifier = Modifier
                        .alpha(1f - titleT)
                        .graphicsLayer {
                            scaleX = 1f - titleT * 0.07f
                            scaleY = 1f - titleT * 0.07f
                        },
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "版本 $versionName",
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(1f - versionT)
                        .graphicsLayer {
                            scaleX = 1f - versionT * 0.07f
                            scaleY = 1f - versionT * 0.07f
                        },
                )
            }
        },
        list = {

            item { Spacer(Modifier.height(collapseSpace)) }
            item {
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    AboutSection(title = "项目", backdrop = auroraBackdrop) {
                        ArrowPreference(
                            title = "开源许可",
                            summary = "Apache-2.0",
                            onClick = {
                                openUrl(context, "https://www.apache.org/licenses/LICENSE-2.0.html")
                            },
                        )
                        ArrowPreference(
                            title = "项目主页",
                            onClick = {
                                openUrl(context, "https://github.com/misaka10902/Megurine")
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    AboutSection(title = "框架", backdrop = auroraBackdrop) {
                        ArrowPreference(
                            title = "Compose Miuix",
                            summary = "界面基础框架",
                            onClick = {
                                openUrl(context, "https://compose-miuix-ui.github.io/miuix/")
                            },
                        )
                        ArrowPreference(
                            title = "AndroidX Media3",
                            summary = "音频播放基础框架",
                            onClick = {
                                openUrl(context, "https://developer.android.com/media/media3")
                            },
                        )
                        ArrowPreference(
                            title = "OkHttp3",
                            summary = "云端服务器连接框架",
                            onClick = {
                                openUrl(context, "https://okhttp.org/")
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    AboutSection(title = "致谢", backdrop = auroraBackdrop) {
                        ArrowPreference(
                            title = "GD Studio",
                            summary = "音源服务提供",
                            onClick = {
                                openUrl(context, "https://music.gdstudio.org/")
                            },
                        )
                        ArrowPreference(
                            title = "LISTEN.moe",
                            summary = "电台模式服务提供",
                            onClick = {
                                openUrl(context, "https://listen.moe/")
                            },
                        )
                        ArrowPreference(
                            title = "Melox",
                            summary = "UX细节方案提供",
                            onClick = {
                                openUrl(context, "https://github.com/Inefy-03/Melox")
                            },
                        )
                        ArrowPreference(
                            title = "Otter Music",
                            summary = "API适配方案参考",
                            onClick = {
                                openUrl(context, "https://otter-music.pages.dev/")
                            },
                        )
                        ArrowPreference(
                            title = "Nexio Scheduler",
                            summary = "项目初期界面构建参考",
                            onClick = {
                                openUrl(context, "https://haozai000.github.io/NexioSchedule/")
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun rememberLogoBlend(): List<BlendColorEntry> {
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    return remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
            )
        }
    }
}

@Composable
private fun AboutLogoIcon(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val advanced = LocalAdvancedMaterialEnabled.current
    if (advanced) {
        Image(
            imageVector = MiuixIcons.Glass.Recording,
            contentDescription = null,
            modifier = modifier.textureBlur(
                backdrop = backdrop,
                shape = RoundedCornerShape(24.dp),
                blurRadius = 150f,
                colors = BlurDefaults.blurColors(blendColors = rememberLogoBlend()),
                contentBlendMode = BlendMode.DstIn,
            ),
        )
    } else {
        Image(
            imageVector = MiuixIcons.Glass.Recording,
            contentDescription = null,
            modifier = modifier.alpha(0.4f),
            colorFilter = ColorFilter.tint(Color.Gray),
        )
    }
}

@Composable
private fun AboutLogoTitle(
    text: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val advanced = LocalAdvancedMaterialEnabled.current
    val logoBlend = rememberLogoBlend()
    Text(
        text = text,
        fontSize = 35.sp,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = modifier.then(
            if (advanced) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(16.dp),
                    blurRadius = 150f,
                    colors = BlurDefaults.blurColors(blendColors = logoBlend),
                    contentBlendMode = BlendMode.DstIn,
                )
            } else {
                Modifier
            },
        ),
    )
}

@Composable
private fun AboutSection(
    title: String,
    backdrop: LayerBackdrop,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val advanced = LocalAdvancedMaterialEnabled.current
    val shape = rememberSquircleShape(20.dp)

    val tint = scheme.surfaceContainer.copy(alpha = 0.55f)

    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(text = title, modifier = Modifier.offset(x = (-15).dp))
        Card(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (advanced) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = { blur(40f) },
                            onDrawSurface = { drawRect(color = tint) },
                        )
                    } else {
                        Modifier.background(color = tint, shape = shape)
                    }
                ),
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = scheme.onSurfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
private fun rememberSafeVersion(context: Context): String {
    return remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
