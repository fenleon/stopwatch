package com.dailyhobbyist.stopwatch

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.thelightphone.sdk.LightActivity
import com.thelightphone.sdk.ui.LocalHapticsEnabled

@Composable
fun rememberLightHapticClick(): () -> Unit {
    val hapticsEnabled = LocalHapticsEnabled.current
    val activity = LocalActivity.current as? LightActivity
    return { if (hapticsEnabled) activity?.vibrator?.click() }
}
