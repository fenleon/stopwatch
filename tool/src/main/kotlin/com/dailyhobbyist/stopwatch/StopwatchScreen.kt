package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.compose.material3.Text
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.rememberLightHapticClick
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.verticalGridUnitsAsDp

@InitialScreen
class StopwatchScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, StopwatchViewModel>(sealedActivity) {

    override val viewModelClass: Class<StopwatchViewModel>
        get() = StopwatchViewModel::class.java

    override fun createViewModel(): StopwatchViewModel {
        return StopwatchViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val isRunning by viewModel.isRunning.collectAsState()
        val elapsed by viewModel.elapsedMs.collectAsState()
        val laps by viewModel.laps.collectAsState()
        val elapsedState = viewModel.elapsedMs.collectAsState()

        val focusRequester = remember { FocusRequester() }
        val configuration = LocalConfiguration.current
        // the timer grows with the long side in landscape
        val landscapeScale = max(
            1f,
            configuration.screenWidthDp.toFloat() / configuration.screenHeightDp,
        )
        val haptic = rememberLightHapticClick()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    // External controls: volume rocker drives the watch
                    // (down = start/stop, up = lap). Keys are consumed so
                    // they never adjust the ringer.
                    .onPreviewKeyEvent { event ->
                        // Act on key-UP and swallow every key-DOWN (press and
                        // auto-repeats): holding the rocker must not
                        // machine-gun start/stop/lap.
                        if (event.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.VolumeDown -> {
                                haptic()
                                viewModel.startStop()
                                true
                            }
                            Key.VolumeUp -> {
                                // running: lap; stopped: reset — silent at 0
                                if (elapsed > 0L || laps.isNotEmpty()) {
                                    haptic()
                                    if (isRunning) viewModel.lap() else viewModel.reset()
                                }
                                true
                            }
                            else -> false
                        }
                    },
            ) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                LightTopBar(
                    // top-bar text actions render at the Button size
                    // (native Calendar EDIT spec)
                    textVariant = LightTextVariant.Button,
                    rightButton = LightBarButton.Text("HISTORY") {
                        navigateTo(screenFactory = { sealed -> HistoryScreen(sealed) })
                    },
                )

                // ---- big time display, centred like the LightOS Timer ----
                // Tap = start/stop, double-tap = lap. The toggle fires after a
                // short fuse so the second tap of a double can cancel it and
                // lap instead — the watch never visibly pauses mid-double-tap.
                val scope = rememberCoroutineScope()
                val pendingToggle = remember { mutableStateOf<Job?>(null) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val pending = pendingToggle.value
                                    if (pending?.isActive == true) {
                                        // second tap of a double: light tick + lap
                                        pending.cancel()
                                        pendingToggle.value = null
                                        haptic()
                                        viewModel.lap()
                                    } else {
                                        // every tap buzzes immediately (chats'
                                        // double-tap feel); the toggle itself
                                        // waits on the fuse so a double-tap
                                        // never shows a paused frame
                                        haptic()
                                        pendingToggle.value = scope.launch {
                                            delay(200L)
                                            viewModel.startStop()
                                        }
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    FixedWidthTime(
                        text = formatTime(elapsed),
                        style = LightThemeTokens.typography.title,
                        scale = landscapeScale,
                    )
                }

                // ---- laps: exactly three rows visible, scrollbar only past that ----
                // the live row reads the State itself, so the 30 fps ticker
                // doesn't recompose the list — only that one row
                LapList(
                    elapsedState = elapsedState,
                    isRunning = isRunning,
                    laps = laps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                )

                // ---- controls ----
                // LAP sits centred between START/STOP and the right edge:
                // centres land at ~18% · 50% · 75% of the screen width.
                val hasTime = elapsed > 0L || laps.isNotEmpty()
                // vertical grid units: the bar keeps its portrait height in
                // landscape (the width-based grid inflates there)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1f.verticalGridUnitsAsDp())
                        .height(4f.verticalGridUnitsAsDp())
                        .padding(start = 2f.gridUnitsAsDp()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(0.26f), contentAlignment = Alignment.Center) {
                        if (hasTime) BarButton("RESET") { viewModel.reset() }
                    }
                    Box(Modifier.weight(0.415f), contentAlignment = Alignment.Center) {
                        BarButton(if (isRunning) "STOP" else "START") { viewModel.startStop() }
                    }
                    Box(Modifier.weight(0.20f), contentAlignment = Alignment.Center) {
                        if (isRunning) BarButton("LAP") { viewModel.lap() }
                    }
                    Spacer(Modifier.weight(0.125f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom bar button — same Button-variant text and full bar height as the
// SDK's LightBottomBar, but freely positionable.
// ---------------------------------------------------------------------------

@Composable
private fun BarButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(4f.verticalGridUnitsAsDp())
            .lightClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = label, variant = LightTextVariant.Button, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Lap list
// ---------------------------------------------------------------------------

@Composable
private fun LapList(
    elapsedState: State<Long>,
    isRunning: Boolean,
    laps: List<Long>,
    modifier: Modifier = Modifier,
) {
    // splits[i] = duration of lap i
    val splits = laps.mapIndexed { i, total ->
        if (i == 0) total else total - laps[i - 1]
    }

    val listState = rememberLazyListState()
    // a new lap always snaps the list back to the newest row at the top
    LaunchedEffect(laps.size) {
        if (laps.isNotEmpty()) listState.scrollToItem(0)
    }

    // viewport = exactly three measured rows, so all three always fit
    // regardless of device density
    val style = scaledCopyStyle()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rowHeightDp = remember(style) {
        with(density) {
            (measurer.measure("0", style).size.height + 20.dp.toPx()).toDp()
        }
    }
    val gridUnitDp = LocalConfiguration.current.screenWidthDp / 27f

    LightLazyScrollView(
        modifier = modifier.height(rowHeightDp * 3 + 1.dp),
        listState = listState,
        scrollBarPosition = LightScrollBarPosition.Inside,
        uniformItemHeightGridUnits = rowHeightDp.value / gridUnitDp,
    ) {
        // the lap in progress — freezes while stopped, counting while running
        if (laps.isNotEmpty()) {
            item(key = "live") {
                val elapsed = elapsedState.value
                LapRow(
                    label = "Lap ${laps.size + 1}",
                    split = elapsed - (laps.lastOrNull() ?: 0L),
                    total = elapsed,
                )
            }
        }
        // recorded laps, newest first
        items(
            items = laps.indices.reversed().toList(),
            key = { it },
        ) { i ->
            LapRow(
                label = "Lap ${i + 1}",
                split = splits[i],
                total = laps[i],
            )
        }
    }
}

@Composable
private fun LapRow(
    label: String,
    split: Long,
    total: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // start inset for the grid margin; end inset keeps rows clear of
            // the right-edge scrollbar
            .padding(start = 28.dp, end = 3f.gridUnitsAsDp(), top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Copy,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // lap split and running total — same size, each anchored in its own column
        TimeCell(text = compactTime(split))
        Spacer(modifier = Modifier.width(14.dp))
        TimeCell(text = compactTime(total))
    }
}

// ---------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------

/** 00:00.00 → MM:SS.hh, growing to H:MM:SS.hh past an hour. */
internal fun formatTime(ms: Long): String {
    val clamped = if (ms < 0) 0L else ms
    val h = clamped / 3_600_000
    val m = (clamped % 3_600_000) / 60_000
    val s = (clamped % 60_000) / 1_000
    val c = (clamped % 1_000) / 10
    return if (h > 0) {
        "%d:%02d:%02d.%02d".format(h, m, s, c)
    } else {
        "%02d:%02d.%02d".format(m, s, c)
    }
}
