package com.halilibo.composevideoplayer

import androidx.animation.FloatPropKey
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.animation.tween
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.stateFor
import androidx.ui.animation.transition
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.drawOpacity
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.layout.*
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.IconButton
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Pause
import androidx.ui.material.icons.filled.PlayArrow
import androidx.ui.material.icons.filled.Restore
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import com.halilibo.composevideoplayer.PlaybackState
import com.halilibo.composevideoplayer.util.getDurationString
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
object MediaControlButtons {

    private const val HIDDEN = "hidden"
    private const val VISIBLE = "visible"

    private val alpha = FloatPropKey()
    private val transitionDef by lazy {
        transitionDefinition {
            state(HIDDEN) {
                this[alpha] = 0f
            }
            state(VISIBLE) {
                this[alpha] = 1f
            }

            transition(fromState = HIDDEN, toState = VISIBLE) {
                alpha using tween(
                        durationMillis = 250,
                        easing = LinearEasing
                )
            }

            transition(fromState = VISIBLE, toState = HIDDEN) {
                alpha using tween(
                        durationMillis = 250,
                        easing = LinearEasing
                )
            }
        }
    }

    @Composable
    operator fun invoke(modifier: Modifier = Modifier) {
        val controller = VideoPlayerControllerAmbient.current

        val controlsEnabled by controller.collect { controlsEnabled }

        // Dictates the direction of appear animation.
        // If controlsVisible is true, appear animation needs to be triggered.
        val controlsVisible by controller.collect { controlsVisible }

        // When controls are not visible anymore we should remove them from UI tree
        // Controls by default should always be on screen.
        // Only when disappear animation finishes, controls can be freely cleared from the tree.
        val (controlsExistOnUITree, setControlsExistOnUITree) = stateFor(controlsVisible) { true }

        val appearTransition = transition(
                transitionDef,
                initState = HIDDEN,
                toState = if(controlsVisible) VISIBLE else HIDDEN,
                onStateChangeFinished = {
                    setControlsExistOnUITree(it == VISIBLE)
                }
        )

        if (controlsEnabled && controlsExistOnUITree) {
            Content(modifier = Modifier
                    .drawOpacity(appearTransition[alpha])
                    .drawBackground(Color.Black.copy(alpha = appearTransition[alpha]*0.6f))
                    + modifier)
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val controller = VideoPlayerControllerAmbient.current

        Stack(modifier = Modifier + modifier) {

            Box(modifier = Modifier.gravity(Alignment.Center).fillMaxSize().clickable(indication = null) {
                controller.hideControls()
            })
            PositionAndDurationNumbers(modifier = Modifier.gravity(Alignment.BottomCenter))
            PlayPauseButton(modifier = Modifier.gravity(Alignment.Center))
        }
    }
}

@ExperimentalCoroutinesApi
@Composable
fun PositionAndDurationNumbers(
        modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val pos by controller.collect { currentPosition }
    val dur by controller.collect { duration }

    Column(modifier = Modifier + modifier) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Text(getDurationString(pos, false),
                    style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f,2f))
                    ))
            Box(modifier = Modifier.weight(1f))
            Text(
                    getDurationString(dur, false),
                    style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f,2f))
                    )
            )
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    val isPlaying by controller.collect { isPlaying }
    val playbackState by controller.collect { playbackState }

    IconButton(
            onClick = { controller.playPauseToggle() },
            modifier = Modifier + modifier
    ) {
        if (isPlaying) {
            ShadowedIcon(icon = Icons.Filled.Pause)
        } else {
            when (playbackState) {
                PlaybackState.ENDED -> {
                    ShadowedIcon(icon = Icons.Filled.Restore)
                }
                PlaybackState.BUFFERING -> {
                    CircularProgressIndicator()
                }
                else -> {
                    ShadowedIcon(icon = Icons.Filled.PlayArrow)
                }
            }
        }
    }
}