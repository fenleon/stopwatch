package com.dailyhobbyist.stopwatch

/**
 * Upstream-compatible stand-in for the fork-only
 * `com.thelightphone.sdk.ui.rememberLightHapticClick`.
 *
 * Pristine upstream light-sdk exposes no tool-facing way to fire a haptic
 * click (the activity is sealed away and the plugin bans LocalContext /
 * LocalActivity / reflection), so this degrades to a no-op. Touch haptics on
 * buttons still come from `lightClickable`, which consults
 * `LocalHapticsEnabled` internally. Once the upstream PR lands
 * (`rememberLightHapticClick` in sdk/ui, sibling of LightClickable.kt), swap
 * the body back to the real implementation.
 */
fun rememberLightHapticClick(): () -> Unit = {}
