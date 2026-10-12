package com.dailyhobbyist.stopwatch

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.thelightphone.sdk.ContextLightVibrator
import com.thelightphone.sdk.ui.LocalHapticsEnabled

@Composable
fun rememberLightHapticClick(): () -> Unit {
    val hapticsEnabled = LocalHapticsEnabled.current
    val context = LocalContext.current
    return { if (hapticsEnabled) ContextLightVibrator(context).click() }
}
