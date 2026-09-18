package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter

class SessionDetailScreen(
    sealedActivity: SealedLightActivity,
    private val session: StopwatchSession,
) : LightScreen<Unit, SessionDetailViewModel>(sealedActivity) {

    override val viewModelClass: Class<SessionDetailViewModel>
        get() = SessionDetailViewModel::class.java

    override fun createViewModel(): SessionDetailViewModel {
        return SessionDetailViewModel(lightContext.dataStore, session)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        // splits[i] = duration of lap i within the run
        val laps = session.laps
        val splits = laps.mapIndexed { i, total ->
            if (i == 0) total else total - laps[i - 1]
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(
                        formatSessionDate(session.startedAtWall) +
                            " " + formatSessionTime(session.startedAtWall),
                    ),
                )

                // total time header — tight top/bottom buffers so six lap
                // rows fit without a scrollbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LightText(
                        text = formatTime(session.totalMs),
                        variant = LightTextVariant.Subtitle,
                    )
                }

                if (laps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No laps recorded for this run.",
                            variant = LightTextVariant.Fine,
                            align = TextAlign.Center,
                        )
                    }
                } else {
                    // viewport = exactly six measured rows (any density):
                    // a 6-lap run never scrolls, the rail only appears at 7+
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        scrollBarPosition = LightScrollBarPosition.Inside,
                        uniformItemHeightGridUnits = rowHeightDp.value / gridUnitDp,
                    ) {
                        items(items = laps.indices.toList(), key = { it }) { i ->
                            LapDetailRow(
                                label = "Lap ${i + 1}",
                                split = splits[i],
                                total = laps[i],
                            )
                        }
                    }
                }

                // delete is immediate — confirmation lives on the history list
                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text("REMOVE") {
                            viewModel.confirmDelete { goBack() }
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun LapDetailRow(
    label: String,
    split: Long,
    total: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        TimeCell(text = compactTime(split))
        Spacer(modifier = Modifier.width(14.dp))
        TimeCell(text = compactTime(total))
    }
}
