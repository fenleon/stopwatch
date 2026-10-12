package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

class HistoryScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HistoryViewModel>(sealedActivity) {

    override val viewModelClass: Class<HistoryViewModel>
        get() = HistoryViewModel::class.java

    override fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val sessions by viewModel.sessions.collectAsState()
        // one X confirm active at a time, across the whole list
        var confirmingId by remember { mutableStateOf<Long?>(null) }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("History"),
                )

                if (sessions == null) {
                    // first load — keep the previous frame instead of
                    // flashing the empty state
                } else if (sessions!!.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No sessions saved yet",
                            variant = LightTextVariant.Subheading,
                            align = TextAlign.Center,
                        )
                    }
                } else {
                    LightLazyScrollView(
                        modifier = Modifier.fillMaxWidth(),
                        scrollBarPosition = LightScrollBarPosition.Inside,
                        uniformItemHeightGridUnits = 3.83f,
                    ) {
                        items(items = sessions!!, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                confirming = confirmingId == session.id,
                                onConfirmChange = { confirmingId = if (it) session.id else null },
                                onClick = {
                                    confirmingId = null
                                    navigateTo(screenFactory = { sealed ->
                                        SessionDetailScreen(sealed, session)
                                    })
                                },
                                onDelete = { viewModel.delete(session) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: StopwatchSession,
    confirming: Boolean,
    onConfirmChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val confirmingDelete = confirming

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable { onClick() }
            .padding(start = 28.dp, end = 3f.gridUnitsAsDp(), top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(
                text = formatSessionDate(session.startedAtWall),
                variant = LightTextVariant.Detail,
            )
            // while confirming the laps count makes way so CANCEL REMOVE
            // fit on the time line (the time itself stays)
            LightText(
                text = formatSessionTime(session.startedAtWall) +
                    if (!confirmingDelete) " · " + lapCountLabel(session.laps.size) else "",
                variant = LightTextVariant.Fine,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (confirmingDelete) {
            LightText(
                text = "CANCEL",
                variant = LightTextVariant.Fine,
                modifier = Modifier.lightClickable { onConfirmChange(false) },
            )
            Spacer(modifier = Modifier.width(12.dp))
            LightText(
                text = "REMOVE",
                variant = LightTextVariant.Fine,
                modifier = Modifier.lightClickable {
                    onConfirmChange(false)
                    onDelete()
                },
            )
        } else {
            TimeCell(text = lapTime(session.totalMs))
            Spacer(modifier = Modifier.width(12.dp))
            // centred on the time line only: a bottom-aligned strip the
            // height of that line, X centred inside it. The start offset
            // centres the X between the times column and the scrollbar rail.
            Box(
                modifier = Modifier
                    .offset(x = 0.6f.gridUnitsAsDp(), y = -0.2f.gridUnitsAsDp())
                    .height(1.5f.gridUnitsAsDp()),
                contentAlignment = Alignment.Center,
            ) {
                LightIcon(
                    icon = LightIcons.CLOSE,
                    size = 1.5f,
                    modifier = Modifier.lightClickable { onConfirmChange(true) },
                )
            }
        }
    }
}
