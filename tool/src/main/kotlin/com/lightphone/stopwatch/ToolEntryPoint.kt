package com.lightphone.stopwatch

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(
        serverData: StateFlow<LightServerData?>,
    ) {
        // Stopwatch is fully offline — nothing to initialize.
    }

    override suspend fun onPushNotification(
        data: ByteArray,
    ) {
        // No push notifications used.
    }
}
