package com.halilibo.composevideoplayer

import android.os.Parcelable
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.doubleTapGestureFilter
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import com.halilibo.composevideoplayer.util.getDurationString
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.abs

@Composable
fun MediaControlGestures(
    modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val controlsEnabled by controller.collect { controlsEnabled }
    val gesturesEnabled by controller.collect { gesturesEnabled }
    val controlsVisible by controller.collect { controlsVisible }

    if (controlsEnabled && !controlsVisible && gesturesEnabled) {
        Box(modifier = modifier) {
            GestureBox()
            QuickSeekAnimation()
            DraggingProgressOverlay(modifier = modifier)
        }
    }

}

@Composable
fun GestureBox(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    var boxSize: IntSize = IntSize.Zero

    val dragObserver = object : DragObserver {
        var wasPlaying: Boolean = true
        var totalOffset = Offset.Zero
        var diffTime = -1f

        var duration: Long = 0
        var currentPosition: Long = 0

        // When this job completes, it seeks to desired position.
        // It gets cancelled if delay does not complete
        var seekJob: Job? = null

        fun resetState() {
            totalOffset = Offset.Zero
            controller.setDraggingProgress(null)
        }

        override fun onStart(downPosition: Offset) {
            wasPlaying = controller.currentState { isPlaying }
            controller.pause()

            currentPosition = controller.currentState { currentPosition }
            duration = controller.currentState { duration }

            resetState()
        }

        override fun onStop(velocity: Offset) {
            if (wasPlaying) controller.play()
            resetState()
        }

        override fun onDrag(dragDistance: Offset): Offset {
            seekJob?.cancel()

            totalOffset += dragDistance

            val diff = totalOffset.x

            diffTime = if (duration <= 60_000) {
                duration.toFloat() * diff / boxSize.width.toFloat()
            } else {
                60_000.toFloat() * diff / boxSize.width.toFloat()
            }

            var finalTime = currentPosition + diffTime
            if (finalTime < 0) {
                finalTime = 0f
            } else if (finalTime > duration) {
                finalTime = duration.toFloat()
            }
            diffTime = finalTime - currentPosition

            controller.setDraggingProgress(
                DraggingProgress(
                    finalTime = finalTime,
                    diffTime = diffTime
                )
            )

            seekJob = CoroutineScope(Dispatchers.Main).launch {
                delay(200)

                controller.seekTo(finalTime.toLong())
            }

            return dragDistance
        }
    }

    Row(modifier = Modifier.fillMaxSize()
        // TODO
        .onGloballyPositioned { boxSize = it.size }
        .dragGestureFilter(
            dragObserver = dragObserver,
            canDrag = {
                it.name == "LEFT" || it.name == "RIGHT"
            }
        )
            + modifier) {

        val commonModifier = Modifier.fillMaxHeight()
            .tapGestureFilter {
                controller.showControls()
            }
        Box(
            modifier = commonModifier
                .weight(2f)
                .doubleTapGestureFilter {
                    controller.quickSeekRewind()
                }
        )

        // Center where double tap does not exist
        Box(
            modifier = commonModifier
                .weight(1f)
        )

        Box(
            modifier = commonModifier
                .weight(2f)
                .doubleTapGestureFilter {
                    controller.quickSeekForward()
                }
        )
    }
}

@Composable
fun QuickSeekAnimation(
    modifier: Modifier = Modifier
) {
    val controller = VideoPlayerControllerAmbient.current

    val state by controller.collect { quickSeekAction }

    Row(modifier = Modifier + modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (state.direction == QuickSeekDirection.Rewind) {
                val transitionState = transition(
                    definition = transitionDef,
                    initState = "start",
                    toState = "end",
                    onStateChangeFinished = {
                        controller.setQuickSeekAction(QuickSeekAction.none())
                    }
                )

                val realAlpha = 1 - abs(1 - transitionState[alpha])
                ShadowedIcon(
                    Icons.Filled.FastRewind,
                    modifier = Modifier
                        .drawLayer(alpha = realAlpha)
                        .align(Alignment.Center)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (state.direction == QuickSeekDirection.Forward) {
                val transitionState = transition(
                    definition = transitionDef,
                    initState = "start",
                    toState = "end",
                    onStateChangeFinished = {
                        controller.setQuickSeekAction(QuickSeekAction.none())
                    }
                )

                val realAlpha = 1 - abs(1 - transitionState[alpha])
                ShadowedIcon(
                    Icons.Filled.FastForward,
                    modifier = Modifier
                        .drawLayer(alpha = realAlpha)
                        .gravity(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun DraggingProgressOverlay(modifier: Modifier = Modifier) {
    val controller = VideoPlayerControllerAmbient.current

    val draggingProgress by controller.collect { draggingProgress }

    val draggingProgressValue = draggingProgress

    if (draggingProgressValue != null) {
        Box(modifier = Modifier + modifier) {
            Text(
                draggingProgressValue.progressText,
                fontSize = TextUnit.Companion.Sp(26),
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        blurRadius = 8f,
                        offset = Offset(2f, 2f)
                    )
                ),
                modifier = Modifier.gravity(Alignment.Center)
            )
        }
    }

}

private val alpha = FloatPropKey()
private val transitionDef = transitionDefinition<String> {
    state("start") {
        this[alpha] = 0f
    }
    state("end") {
        this[alpha] = 2f
    }

    transition(fromState = "start", toState = "end") {
        alpha using tween(
            durationMillis = 500,
            easing = LinearEasing
        )
    }

    snapTransition("end" to "start")
}

@Parcelize
data class DraggingProgress(
    val finalTime: Float,
    val diffTime: Float
) : Parcelable {
    val progressText: String
        get() = "${getDurationString(finalTime.toLong(), false)} " +
                "[${if (diffTime < 0) "-" else "+"}${
                    getDurationString(
                        abs(diffTime.toLong()),
                        false
                    )
                }]"
}

enum class QuickSeekDirection {
    None,
    Rewind,
    Forward
}

@Parcelize
data class QuickSeekAction(
    val direction: QuickSeekDirection
) : Parcelable {
    // Each action is unique
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(direction)
    }

    companion object {
        fun none() = QuickSeekAction(QuickSeekDirection.None)
        fun forward() = QuickSeekAction(QuickSeekDirection.Forward)
        fun rewind() = QuickSeekAction(QuickSeekDirection.Rewind)
    }
}