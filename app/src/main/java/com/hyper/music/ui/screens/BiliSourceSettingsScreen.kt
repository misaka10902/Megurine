package com.hyper.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.MusicLibrary
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BiliSourceSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    var showKeywordDialog by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(20.dp)

    DetailPage(
        title = "B站音源设置",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    colors = cardGlassColors(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "提示",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        BiliTipText("本应用默认采用网易云作为主播放源，B站音源作为补充，以应对部分歌曲无版权的情况")
                        BiliTipText("B站音源歌词来自网易云同名音源，可能会出现歌词不同步的情况，还请谅解")
                        BiliTipText("B站音源歌曲不支持下载")
                        BiliTipText("您可以通过下面的“搜索关键词”设置自定义搜索B站歌曲时的附加项，提高搜索精度")
                        BiliTipText("虽然可以通过登录B站账号、获取字幕的方式获取B站音源的歌词，但为保障用户隐私，本应用不提供任何登录功能，以后也不会提供")
                    }
                }

                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                    insideMargin = PaddingValues(0.dp),
                    colors = cardGlassColors(),
                ) {
                    SwitchPreference(
                        title = "启用",
                        checked = settings.biliEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setBiliEnabled(it)
                        }
                    )
                    ArrowPreference(
                        title = "搜索关键词",
                        summary = settings.biliKeywords,
                        enabled = settings.biliEnabled,
                        onClick = { showKeywordDialog = true },
                    )
                }
            }
        }
    }

    BiliKeywordDialog(
        show = showKeywordDialog,
        current = settings.biliKeywords,
        backdrop = backdrop,
        onConfirm = { MusicLibrary.setBiliKeywords(it) },
        onDismiss = { showKeywordDialog = false },
    )
}

@Composable
private fun BiliTipText(text: String) {
    Text(
        text = "•  $text",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MiuixTheme.colorScheme.onSurface,
    )
}
