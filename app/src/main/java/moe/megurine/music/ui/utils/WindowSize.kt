package moe.megurine.music.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WindowSizeDp(val width: Dp, val height: Dp)

@Composable
@ReadOnlyComposable
fun rememberWindowSizeDp(): WindowSizeDp {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        WindowSizeDp(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
}

fun isCompactWindow(size: WindowSizeDp): Boolean =
    size.width > size.height || size.height < 600.dp



@ReadOnlyComposable
fun isLargeScreen(size: WindowSizeDp): Boolean {
    if (size.width <= 0.dp) return false
    return size.width >= 840.dp || (size.width >= 600.dp && size.height / size.width < 1.2f)
}


@Composable
@ReadOnlyComposable
fun songColumns(): Int = if (isLargeScreen(rememberWindowSizeDp())) 2 else 1


fun contentFraction(columns: Int): Float = if (columns >= 2) 0.9f else 1f


@Composable
@ReadOnlyComposable
fun horizontalContentPadding(): Dp =
    if (isLargeScreen(rememberWindowSizeDp())) 24.dp else 16.dp
