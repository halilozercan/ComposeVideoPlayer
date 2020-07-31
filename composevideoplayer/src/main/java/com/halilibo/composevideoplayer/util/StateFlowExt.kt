package com.halilibo.composevideoplayer.util

import com.halilibo.composevideoplayer.VideoPlayerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
fun MutableStateFlow<VideoPlayerState>.set(block: VideoPlayerState.() -> VideoPlayerState) {
    this.value = this.value.block()
}