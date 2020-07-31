package com.halilibo.composevideoplayer

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.ui.core.Modifier
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.height
import androidx.ui.layout.width
import androidx.ui.unit.dp

@Composable
fun ProgressIndicator(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current
    val progress by controller.collect { currentPosition }
    val secondaryProgress by controller.collect { secondaryProgress }
    val max by controller.collect { duration }
    val controlsVisible by controller.collect { controlsVisible }
    val controlsEnabled by controller.collect { controlsEnabled }
    val videoSize by controller.collect { videoSize }

    SeekBar(
        progress = progress,
        max = max,
        enabled = controlsVisible && controlsEnabled,
        onSeek = {
            controller.previewSeekTo(it)
        },
        onSeekStopped = {
            controller.seekTo(it)
        },
        secondaryProgress = secondaryProgress,
        seekerPopup = @Composable() {
            PlayerSurface(modifier = Modifier
                .height(48.dp)
                .width(48.dp * videoSize.width / videoSize.height)
                .drawBackground(Color.DarkGray)
            ) {
                controller.previewPlayerViewAvailable(it)
            }
        },
        modifier = modifier
    )
}