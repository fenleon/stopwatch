package com.lightphone.stopwatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * One completed stopwatch run.
 *
 * @param startedAtWall wall-clock ms when the run began (used for the date/time label)
 * @param totalMs       total elapsed time of the run
 * @param laps          cumulative total (ms) at each recorded lap
 */
data class StopwatchSession(
    val id: Long,
    val startedAtWall: Long,
    val totalMs: Long,
    val laps: List<Long>,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startedAtWall", startedAtWall)
        put("totalMs", totalMs)
        put("laps", JSONArray().also { arr -> laps.forEach { arr.put(it) } })
    }

    companion object {
        fun fromJson(o: JSONObject): StopwatchSession {
            val arr = o.optJSONArray("laps") ?: JSONArray()
            return StopwatchSession(
                id = o.optLong("id"),
                startedAtWall = o.optLong("startedAtWall"),
                totalMs = o.optLong("totalMs"),
                laps = List(arr.length()) { i -> arr.getLong(i) },
            )
        }
    }
}

/**
 * Shared read/write for saved sessions. Both the stopwatch screen (which
 * appends a session on reset) and the history screens (which read and can
 * delete) go through this single key so they never disagree.
 */
object StopwatchHistory {
    private val keyHistory = stringPreferencesKey("sw_history")
    private const val MAX_SESSIONS = 200

    suspend fun load(dataStore: DataStore<Preferences>): List<StopwatchSession> {
        val raw = dataStore.data.first()[keyHistory] ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> StopwatchSession.fromJson(arr.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    /** Newest first. */
    suspend fun loadSorted(dataStore: DataStore<Preferences>): List<StopwatchSession> =
        load(dataStore).sortedByDescending { it.startedAtWall }

    suspend fun add(dataStore: DataStore<Preferences>, session: StopwatchSession) {
        val current = load(dataStore)
        val updated = (current + session).takeLast(MAX_SESSIONS)
        write(dataStore, updated)
    }

    suspend fun delete(dataStore: DataStore<Preferences>, id: Long) {
        val updated = load(dataStore).filterNot { it.id == id }
        write(dataStore, updated)
    }

    suspend fun clear(dataStore: DataStore<Preferences>) {
        write(dataStore, emptyList())
    }

    private suspend fun write(
        dataStore: DataStore<Preferences>,
        sessions: List<StopwatchSession>,
    ) {
        val json = JSONArray().also { arr ->
            sessions.forEach { arr.put(it.toJson()) }
        }.toString()
        dataStore.edit { it[keyHistory] = json }
    }
}
