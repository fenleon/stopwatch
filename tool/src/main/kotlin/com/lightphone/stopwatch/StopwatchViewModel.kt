package com.lightphone.stopwatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * All stopwatch state lives here.
 *
 * Timekeeping is anchor-based: while running we store the wall-clock moment
 * the watch was (re)started plus any previously accumulated time. Elapsed is
 * always recomputed from the clock, so leaving the app — or even rebooting
 * the phone — never loses time or drifts.
 */
class StopwatchViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {

    // ---- persisted state ----
    private var startedAt: Long = 0L        // wall-clock ms when last (re)started
    private var accumulated: Long = 0L      // ms accumulated across pauses
    private var runStartedWall: Long = 0L   // wall-clock ms of the very first start this run

    val isRunning = MutableStateFlow(false)
    val elapsedMs = MutableStateFlow(0L)

    /** Cumulative total (ms) at the moment each lap was recorded. */
    val laps = MutableStateFlow<List<Long>>(emptyList())

    private var loaded = false
    private var ticker: Job? = null

    companion object {
        /** 99:59.99 — the display cap; the watch auto-stops there. */
        private const val MAX_ELAPSED_MS = 99L * 60_000 + 59_999
        private const val MAX_LAPS = 99
    }

    // ---- keys ----
    private val keyRunning = booleanPreferencesKey("sw_running")
    private val keyStartedAt = longPreferencesKey("sw_started_at")
    private val keyAccumulated = longPreferencesKey("sw_accumulated")
    private val keyLaps = stringPreferencesKey("sw_laps")
    private val keyRunStartedWall = longPreferencesKey("sw_run_started_wall")

    // ---- lifecycle ----

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        viewModelScope.launch {
            if (!loaded) {
                load()
                loaded = true
            }
            refreshElapsed()
            if (isRunning.value) startTicker()
        }
    }

    override fun onScreenHide(screen: SimpleLightScreen<Unit>) {
        super.onScreenHide(screen)
        stopTicker()
        persist()
    }

    override fun onAppPause() {
        super.onAppPause()
        stopTicker()
        persist()
    }

    // ---- actions ----

    fun startStop() {
        if (isRunning.value) {
            // stop: bank the elapsed time
            accumulated += now() - startedAt
            isRunning.value = false
            stopTicker()
            refreshElapsed()
        } else {
            // start / resume
            if (runStartedWall == 0L) runStartedWall = now()
            startedAt = now()
            isRunning.value = true
            startTicker()
        }
        persist()
    }

    fun lap() {
        if (!isRunning.value) return
        if (laps.value.size >= MAX_LAPS) return
        refreshElapsed()
        laps.value = laps.value + elapsedMs.value
        persist()
    }

    fun reset() {
        stopTicker()

        // Bank any running time so this run is what gets saved.
        if (isRunning.value) {
            accumulated += now() - startedAt
            isRunning.value = false
        }

        // Save this run to history (if it actually recorded any time).
        val total = accumulated
        var runLaps = laps.value
        // the final segment after the last LAP press counts as the last lap,
        // so the detail panel shows the complete breakdown
        if (total > 0L && (runLaps.isEmpty() || total > runLaps.last())) {
            runLaps = runLaps + total
        }
        val startWall = if (runStartedWall != 0L) runStartedWall else now()
        if (total > 0L) {
            val session = StopwatchSession(
                id = System.currentTimeMillis(),
                startedAtWall = startWall,
                totalMs = total,
                laps = runLaps,
            )
            viewModelScope.launch {
                StopwatchHistory.add(dataStore, session)
            }
        }

        startedAt = 0L
        accumulated = 0L
        runStartedWall = 0L
        laps.value = emptyList()
        elapsedMs.value = 0L
        persist()
    }

    // ---- internals ----

    private fun now() = System.currentTimeMillis()

    private fun refreshElapsed() {
        val computed =
            if (isRunning.value) accumulated + (now() - startedAt)
            else accumulated
        if (computed >= MAX_ELAPSED_MS) {
            // display cap reached: clamp and auto-stop
            if (isRunning.value) {
                accumulated = MAX_ELAPSED_MS
                isRunning.value = false
                startedAt = 0L
                stopTicker()
                persist()
            }
            elapsedMs.value = MAX_ELAPSED_MS
        } else {
            elapsedMs.value = computed
        }
    }

    private fun startTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (isActive) {
                refreshElapsed()
                delay(33L) // ~30 updates/sec, smooth centiseconds
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private suspend fun load() {
        val prefs = dataStore.data.first()
        isRunning.value = prefs[keyRunning] ?: false
        startedAt = prefs[keyStartedAt] ?: 0L
        accumulated = prefs[keyAccumulated] ?: 0L
        runStartedWall = prefs[keyRunStartedWall] ?: 0L
        laps.value = prefs[keyLaps]?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { i -> arr.getLong(i) }
            }.getOrDefault(emptyList())
        } ?: emptyList()

        // Sanity: if the saved anchor is in the future (clock changed), re-anchor.
        if (isRunning.value && startedAt > now()) {
            startedAt = now()
        }
    }

    private fun persist() {
        val running = isRunning.value
        val started = startedAt
        val acc = accumulated
        val runStart = runStartedWall
        val lapJson = JSONArray().also { arr ->
            laps.value.forEach { arr.put(it) }
        }.toString()

        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[keyRunning] = running
                prefs[keyStartedAt] = started
                prefs[keyAccumulated] = acc
                prefs[keyRunStartedWall] = runStart
                prefs[keyLaps] = lapJson
            }
        }
    }
}
