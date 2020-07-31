package com.halilibo.composevideoplayer

interface MediaPlaybackControls {

    fun play()

    fun pause()

    fun playPauseToggle()

    fun quickSeekForward()

    fun quickSeekRewind()

    fun seekTo(position: Long)
}