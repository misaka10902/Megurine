package moe.megurine.music.ui.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference



@Composable
fun OverlayDropdownMenu(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    OverlayDropdownMenu(
        entries = remember(entry) { listOf(entry) },
        title = title,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        backdrop = backdrop,
        onExpandedChange = onExpandedChange,
    )
}

@Composable
fun OverlayDropdownMenu(
    entries: List<DropdownEntry>,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val items = remember(entries) { entries.filter { it.enabled }.flatMap { it.items } }
    val labels = remember(items) { items.map { it.text } }
    val selectedIndex = remember(items) { items.indexOfFirst { it.selected } }

    GlassDropdownPreference(
        onSelectedIndexChange = { index -> items.getOrNull(index)?.onClick?.invoke() },
        items = labels,
        selectedIndex = selectedIndex,
        title = title,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        summary = summary,
        onExpandedChange = onExpandedChange,
    )
}
