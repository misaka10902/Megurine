package moe.megurine.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassNavigationRail
import top.yukonga.miuix.kmp.glass.GlassNavigationRailState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.icon.extended.Search as ThickSearch
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.icon.glass.OuterScreenApps
import top.yukonga.miuix.kmp.icon.glass.SettingsFill


private const val TAB_COUNT = 4



@Composable
internal fun HyperNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    state: GlassNavigationRailState,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {

    val tabs = remember {
        listOf(
            MiuixIcons.Recording to "探索",
            MiuixIcons.Glass.Item to "歌单",
            MiuixIcons.WorldClock to "历史",
            MiuixIcons.Glass.SettingsFill to "设置",
            MiuixIcons.ThickSearch to "搜索",
        )
    }
    val secondary = remember {
        listOf(
            MiuixIcons.FavoritesFill to "收藏",
            MiuixIcons.Glass.OuterScreenApps to "音乐库",
        )
    }
    val items = remember(tabs) {
        tabs.map { (icon, label) -> GlassNavigationItem(icon = icon, label = label) }
    }
    val secondaryItems = remember(secondary) {
        secondary.map { (icon, label) -> GlassNavigationItem(icon = icon, label = label) }
    }
    GlassNavigationRail(
        items = items,
        secondaryItems = secondaryItems,
        selectedIndex = selectedTab,
        onSelect = { index ->
            when {
                index < TAB_COUNT -> onTabSelected(index)
                index == TAB_COUNT -> onOpenSearch()
                index == tabs.size -> onOpenFavorites()
                else -> onOpenLibrary()
            }
        },
        backdrop = backdrop,
        state = state,
        modifier = modifier,
    )
}
