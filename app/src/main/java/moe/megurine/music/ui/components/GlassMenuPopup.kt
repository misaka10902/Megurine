package moe.megurine.music.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import moe.megurine.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

internal data class GlassMenuEntry(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
internal fun GlassMenuPopup(
    expanded: Boolean,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    items: List<GlassMenuEntry>,
) {
    val mounted = remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) {
            mounted.value = true
        } else if (mounted.value) {
            delay(320)
            mounted.value = false
        }
    }
    if (!mounted.value) return

    PopupLayout(
        visible = mounted,
        enableWindowDim = false,
        enableBackHandler = false,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        renderInRootScaffold = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassMenuTransformContent(
                expanded = expanded,
                anchor = anchor,
                anchorContent = anchorContent,
                backdrop = backdrop,
                onDismiss = onDismiss,
                items = items,
            )
        }
    }
}

@Composable
private fun BoxScope.GlassMenuTransformContent(
    expanded: Boolean,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    items: List<GlassMenuEntry>,
) {
    val haptic = LocalHapticFeedback.current
    val show = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        show.value = true
    }
    LaunchedEffect(expanded) {
        if (!expanded) show.value = false
    }

    GlassTransformPopup(
        show = show.value,
        onDismissRequest = onDismiss,
        anchor = anchor,
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
        anchorContent = anchorContent,
    ) {
        items.forEach { item ->
            GlassPopupItem(
                text = item.label,
                icon = item.icon,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDismiss()
                    item.onClick()
                },
            )
        }
    }
}
