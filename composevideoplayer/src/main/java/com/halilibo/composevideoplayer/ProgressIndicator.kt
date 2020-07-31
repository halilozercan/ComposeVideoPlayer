package com.halilibo.composevideoplayer

import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.compose.*
import androidx.lifecycle.lifecycleScope
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LifecycleOwnerAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.gestures.draggable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.PinDrop
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import androidx.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

@Composable
fun ProgressIndicator(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current
    val progress by controller.currentPosition.collectAsState()
    val secondaryProgress by controller.secondaryProgress.collectAsState()
    val max by controller.duration.collectAsState()
    val controlsVisible by controller.controlsVisible.combine(controller.controlsEnabled) { visible, enabled ->
        visible && enabled
    }.collectAsState(false)

    val videoSize by controller.videoSize.collectAsState()

    SeekBar(
        progress = progress,
        max = max,
        enabled = controlsVisible,
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