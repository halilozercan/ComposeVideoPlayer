package com.halilibo.composevideoplayer.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
fun <T> MutableStateFlow<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}