package com.dailyhobbyist.stopwatch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.isSpecified
import androidx.compose.material3.Text
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp

/**
 * Right-anchored fixed-width cell for a time value. Every row's split/total
 * columns share the same width (sized for the widest compact format), so the
 * columns stay anchored no matter how many digits a value has.
 */
@Composable
internal fun TimeCell(text: String, modifier: Modifier = Modifier) {
    val raw = LightThemeTokens.typography.copy
    val style = scaledTimeStyle(raw).copy(fontFeatureSettings = "tnum")
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // widest reachable format: the 99:59 display cap means H:MM:SS.hh
    // never occurs — sizing for it starved every label on the LP3
    val cellWidth = remember(style) {
        with(density) { measurer.measure("88:88.88", style).size.width.toDp() }
    }
    // single Text node — one per-cell composable keeps long lists smooth
    // (tnum tabular digits keep the columns steady like the slots did)
    Box(modifier.width(cellWidth), contentAlignment = Alignment.CenterEnd) {
        Text(text = text, style = style, maxLines = 1)
    }
}

@Composable
internal fun scaledCopyStyle(): TextStyle = scaledTimeStyle(LightThemeTokens.typography.copy)

internal @Composable fun scaledTimeStyle(style: TextStyle, scale: Float = 1f): TextStyle = style.copy(
    fontSize = (style.fontSize.value * scale).designVerticalPxToSp(),
    lineHeight = if (style.lineHeight.isSpecified) {
        (style.lineHeight.value * scale).designVerticalPxToSp()
    } else style.lineHeight,
    letterSpacing = if (style.letterSpacing.isSpecified) {
        (style.letterSpacing.value * scale).designVerticalPxToSp()
    } else style.letterSpacing,
    color = LightThemeTokens.colors.content,
)

// ---------------------------------------------------------------------------
// Fixed-width time text — every digit sits in an equal slot so the display
// doesn't shift around as numbers change (Akkurat digits vary in width).
// ---------------------------------------------------------------------------

@Composable
internal fun FixedWidthTime(
    text: String,
    style: TextStyle,
    scale: Float = 1f,
) {
    // Scale the token style the same way LightText does (design px → sp),
    // then measure and render with that one style so slots line up exactly.
    val scaled = scaledTimeStyle(style, scale)

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val digitWidth = remember(scaled) {
        val w = (0..9).maxOf { d ->
            measurer.measure(d.toString(), scaled).size.width
        }
        with(density) { w.toDp() }
    }
    val sepWidth = remember(scaled) {
        val w = maxOf(
            measurer.measure(":", scaled).size.width,
            measurer.measure(".", scaled).size.width,
        )
        with(density) { w.toDp() }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEach { ch ->
            val slot = if (ch.isDigit()) digitWidth else sepWidth
            Box(
                modifier = Modifier.width(slot),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ch.toString(),
                    style = scaled,
                    maxLines = 1,
                )
            }
        }
    }
}
