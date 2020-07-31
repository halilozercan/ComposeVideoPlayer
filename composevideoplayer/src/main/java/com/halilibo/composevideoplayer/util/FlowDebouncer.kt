package com.halilibo.composevideoplayer.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ObsoleteCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
class FlowDebouncer<T>(
    timeoutMillis: Long,
    private val context: CoroutineContext = EmptyCoroutineContext
): Flow<T>, CoroutineScope by CoroutineScope(context) {

    private val broadcastChannel: BroadcastChannel<T> = BroadcastChannel(1)
    private val flow: Flow<T> = channelFlow {
        broadcastChannel.consumeEach {
            send(it)
        }
    }.debounce(timeoutMillis)

    fun put(item: T) {
        launch {
            broadcastChannel.send(item)
        }
    }

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }

}