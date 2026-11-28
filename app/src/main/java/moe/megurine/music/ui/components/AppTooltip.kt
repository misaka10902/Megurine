package moe.megurine.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text

private val TooltipBackground = Color(0xFF2E2E2E)
private val TooltipTextColor = Color(0xFFFFFFFF)

@Composable
fun AppTooltip(
    lines: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
    maxWidth: Dp = 340.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .widthIn(max = maxWidth)
            .background(TooltipBackground, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                color = TooltipTextColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
        }
        lines.forEachIndexed { index, line ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            Text(
                text = line,
                color = TooltipTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
