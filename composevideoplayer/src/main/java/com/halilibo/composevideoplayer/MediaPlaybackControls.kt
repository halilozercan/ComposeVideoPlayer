package com.halilibo.composevideoplayer

interface MediaPlaybackControls {

    fun play()

    fun pause()

    fun playPauseToggle()

    fun quickSeekForward()

    fun quickSeekRewind()

    fun seekTo(position: Long)

    companion object {
        operator fun invoke(): MediaPlaybackControls = object: MediaPlaybackControls {
            override fun play() { }

            override fun pause() { }

            override fun playPauseToggle() { }

            override fun quickSeekForward() { }

            override fun quickSeekRewind() { }

            override fun seekTo(position: Long) { }

        }
    }
}