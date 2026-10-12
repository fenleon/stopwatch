package com.lightphone.stopwatch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SessionDetailViewModel(
    private val dataStore: DataStore<Preferences>,
    private val session: StopwatchSession,
) : LightViewModel<Unit>() {

    val confirmingDelete = MutableStateFlow(false)

    fun askDelete() {
        confirmingDelete.value = true
    }

    fun cancelDelete() {
        confirmingDelete.value = false
    }

    /** Deletes the session, then invokes [onDone] (used to pop back to the list). */
    fun confirmDelete(onDone: () -> Unit) {
        viewModelScope.launch {
            StopwatchHistory.delete(dataStore, session.id)
            onDone()
        }
    }
}
