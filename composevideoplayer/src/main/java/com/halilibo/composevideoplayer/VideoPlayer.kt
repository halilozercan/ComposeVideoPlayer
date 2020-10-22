package com.halilibo.composevideoplayer

import androidx.compose.*
import androidx.compose.foundation.AmbientContentColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.SaverScope
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ContextAmbient
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal val VideoPlayerControllerAmbient = ambientOf<VideoPlayerController> { error("VideoPlayerController is not initialized") }

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun VideoPlayer(
    source: VideoPlayerSource,
    backgroundColor: Color = Color.Black,
    controlsEnabled: Boolean = true,
    controlsVisible: Boolean = true,
    gesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier
): MediaPlaybackControls {
    val context = ContextAmbient.current
    val controller = rememberSavedInstanceState(
        saver = object: Saver<VideoPlayerController, VideoPlayerUiState> {
            override fun restore(value: VideoPlayerUiState): VideoPlayerController? {
                return VideoPlayerController(
                    context = context,
                    initialState = value
                )
            }

            override fun SaverScope.save(value: VideoPlayerController): VideoPlayerUiState? {
                return value.currentState { this }
            }
        }) {

        VideoPlayerController(context)
    }

    onCommit(source) {
        controller.setSource(source)
    }

    onCommit(controlsEnabled) {
        controller.enableControls(controlsEnabled)
    }

    onCommit(gesturesEnabled, controlsVisible) {
        controller.enableGestures(gesturesEnabled)
        if(controlsVisible) controller.showControls() else controller.hideControls()
    }

    onCommit(backgroundColor) {
        controller.videoPlayerBackgroundColor = backgroundColor.value.toInt()
    }

    Providers(
            AmbientContentColor provides Color.White,
            VideoPlayerControllerAmbient provides controller
    ) {
        val videoSize by controller.collect { videoSize }

        Box(modifier = Modifier.fillMaxWidth()
                .background(color = backgroundColor)
                .aspectRatio(videoSize.first / videoSize.second)
            .then(modifier)) {

            PlayerSurface(modifier = Modifier.align(Alignment.Center)) {
                controller.playerViewAvailable(it)
            }

            MediaControlGestures(modifier = Modifier.matchParentSize())
            MediaControlButtons(modifier = Modifier.matchParentSize())
            ProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    onDispose {
        controller.onDispose()
    }

    return controller
}