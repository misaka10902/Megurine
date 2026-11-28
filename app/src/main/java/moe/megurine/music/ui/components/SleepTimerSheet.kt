package moe.megurine.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import moe.megurine.music.music.SleepTimer
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SleepTimerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val timerState by SleepTimer.state.collectAsState()
    val active = timerState.active


    val hourPicker = remember(visible) { TimerNumberPickerState(0, 0..23) }
    val minutePicker = remember(visible) { TimerNumberPickerState(10, 0..59) }
    val secondPicker = remember(visible) { TimerNumberPickerState(0, 0..59) }
    var extendToEnd by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var startJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(visible) {
        onDispose {
            startJob?.cancel()
            hourPicker.unlock()
            minutePicker.unlock()
            secondPicker.unlock()
        }
    }

    val pickedSeconds = { hourPicker.value * 3600 + minutePicker.value * 60 + secondPicker.value }
    val remainingSeconds = (timerState.remainingMs / 1000L).toInt()
    var lastActiveSeconds by remember(visible) { mutableIntStateOf(remainingSeconds) }
    SideEffect {
        if (active) lastActiveSeconds = remainingSeconds
    }

    val collapse by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "sleepTimerCollapse",
    )
    val expandedHeight = NumberPickerDefaults.ItemHeight * 3 + 24.dp
    val collapsedHeight = 64.dp
    val cardHeight = expandedHeight + (collapsedHeight - expandedHeight) * collapse
    val pickerAlpha = (1f - collapse / 0.2f).coerceIn(0f, 1f)
    val overlayAlpha = 1f - pickerAlpha
    val selectedFraction = ((1f - collapse) / 0.75f).coerceIn(0f, 1f)

    HyperSheet(visible = visible, onDismiss = onDismiss, title = "睡眠定时") {
        Column(
            modifier = Modifier
                .fillMaxWidth()

                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(GlassShape(14.dp))
                    .background(scheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .requiredHeight(expandedHeight)
                        .padding(12.dp)
                        .graphicsLayer { alpha = pickerAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimerNumberPicker(
                        state = hourPicker,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        enabled = !active,
                    )
                    TimerSeparator()
                    TimerNumberPicker(
                        state = minutePicker,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        enabled = !active,
                    )
                    TimerSeparator()
                    TimerNumberPicker(
                        state = secondPicker,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        enabled = !active,
                    )
                }
                val expandedTimeWidth = maxWidth - 24.dp
                val compactTimeWidth = minOf(168.dp, expandedTimeWidth)
                val rowCompactness = ((collapse - 0.2f) / 0.8f).coerceIn(0f, 1f)
                val timeRowModifier = Modifier
                    .align(Alignment.Center)
                    .width(expandedTimeWidth + (compactTimeWidth - expandedTimeWidth) * rowCompactness)
                TimerTimeRow(
                    seconds = when {
                        active -> remainingSeconds
                        lastActiveSeconds > 0 -> lastActiveSeconds
                        else -> pickedSeconds()
                    },
                    modifier = timeRowModifier.graphicsLayer {
                        alpha = overlayAlpha * if (active) 1f else 1f - selectedFraction
                    },
                )
                if (!active) {
                    TimerTimeRow(
                        seconds = pickedSeconds(),
                        modifier = timeRowModifier.graphicsLayer {
                            alpha = overlayAlpha * selectedFraction
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassShape(14.dp))
                    .background(scheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "延长到单首歌曲播完",
                    color = scheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = extendToEnd, onCheckedChange = { extendToEnd = it })
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                text = if (active) "停止" else "开始",
                onClick = {
                    if (active) {
                        startJob?.cancel()
                        hourPicker.unlock()
                        minutePicker.unlock()
                        secondPicker.unlock()
                        SleepTimer.cancel()
                    } else {
                        if (hourPicker.inputLocked) return@TextButton
                        val hours = hourPicker.captureForStart()
                        val minutes = minutePicker.captureForStart()
                        val seconds = secondPicker.captureForStart()
                        if (hours * 3600 + minutes * 60 + seconds <= 0) {
                            hourPicker.unlock()
                            minutePicker.unlock()
                            secondPicker.unlock()
                            return@TextButton
                        }
                        startJob = scope.launch {
                            var started = false
                            try {
                                coroutineScope {
                                    val h = async { hourPicker.alignForStart() }
                                    val m = async { minutePicker.alignForStart() }
                                    val s = async { secondPicker.alignForStart() }
                                    h.await()
                                    m.await()
                                    s.await()
                                }
                                SleepTimer.start(hours, minutes, seconds, extendToEnd)
                                started = true
                            } finally {
                                if (!started) {
                                    hourPicker.unlock()
                                    minutePicker.unlock()
                                    secondPicker.unlock()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun RowScope.TimerDigit(value: Int) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(NumberPickerDefaults.ItemHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = value.toString().padStart(2, '0'), style = TimerDigitTextStyle)
    }
}

@Composable
private fun TimerSeparator() {
    Box(
        modifier = Modifier
            .width(12.dp)
            .height(NumberPickerDefaults.ItemHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = ":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimerTimeRow(
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerDigit(seconds / 3600)
        TimerSeparator()
        TimerDigit(seconds / 60 % 60)
        TimerSeparator()
        TimerDigit(seconds % 60)
    }
}
