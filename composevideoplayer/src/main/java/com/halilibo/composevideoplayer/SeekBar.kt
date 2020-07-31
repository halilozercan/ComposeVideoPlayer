package com.halilibo.composevideoplayer

import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragSlopExceededGestureFilter
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.gesture.rawDragGestureFilter
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Text
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.layout.*
import androidx.ui.material.LinearProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.text.TextStyle
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import com.halilibo.composevideoplayer.util.getDurationString

@Composable
fun SeekBar(
    progress: Long,
    max: Long,
    enabled: Boolean,
    secondaryProgress: Long? = null,
    onSeek: (progress: Long) -> Unit = {},
    onSeekStarted: (startedProgress: Long) -> Unit = {},
    onSeekStopped: (stoppedProgress: Long) -> Unit = {},
    seekerPopup: @Composable() () -> Unit = {},
    showSeekerDuration: Boolean = true,
    color: Color = MaterialTheme.colors.primary,
    secondaryColor: Color = Color.White.copy(alpha = 0.6f),
    modifier: Modifier = Modifier
) {
    var boxSize by state { IntSize(1, 1) }

    var onGoingDrag by state { false }

    val percentage = progress.coerceAtMost(max).toFloat() / max.coerceAtLeast(1L).toFloat()

    var indicatorOffsetStateByPercentage by stateFor(onGoingDrag) {
        Offset(percentage * boxSize.width.toFloat(), 0f)
    }

    if(!onGoingDrag) {
        indicatorOffsetStateByPercentage = Offset(percentage * boxSize.width.toFloat(), 0f)
    }

    var indicatorOffsetStateByDrag by state { Offset.Zero }

    val finalIndicatorOffsetState = (indicatorOffsetStateByDrag + indicatorOffsetStateByPercentage).let {
        it.copy(x = it.x.coerceIn(0f, boxSize.width.toFloat()))
    }

    val indicatorSize = if (onGoingDrag) { 24.dp } else { 16.dp }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier + Modifier.offset(y = indicatorSize / 2)
    ) {

        // SEEK POPUP
        if (onGoingDrag) {
            var popupSize by state { IntSize(0, 0) }

            val popupSeekerOffsetX = (finalIndicatorOffsetState.x - popupSize.width / 2)
                .coerceIn(0f, (boxSize.width - popupSize.width).toFloat())

            val popupSeekerOffsetXDp = with(DensityAmbient.current) { popupSeekerOffsetX.toDp() }

            Column(
                horizontalGravity = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = popupSeekerOffsetXDp)
                    .drawLayer(alpha = if(popupSize == IntSize.Zero) 0f else 1f)
                    .onPositioned {
                        if(popupSize != it.size) {
                            popupSize = it.size
                        }
                    }
            ) {
                val indicatorProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max

                Box(modifier = Modifier.drawShadow(4.dp)) {
                    seekerPopup()
                }

                Spacer(modifier = Modifier.height(8.dp))

                if(showSeekerDuration) {
                    Text(getDurationString(indicatorProgress.toLong(), false),
                        style = TextStyle(shadow = Shadow(
                            blurRadius = 8f,
                            offset = Offset(2f, 2f))
                        ))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Stack(modifier = Modifier.preferredHeight(indicatorSize)) {
            // SECONDARY PROGRESS
            if (secondaryProgress != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().gravity(Alignment.Center)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.weight(1f),
                        progress = secondaryProgress.coerceAtMost(max).toFloat() / max.coerceAtLeast(1L).toFloat(),
                        color = secondaryColor
                    )
                }
            }

            // SEEK INDICATOR
            if (enabled) {
                val (offsetDpX, offsetDpY) = with(DensityAmbient.current) {
                    (finalIndicatorOffsetState.x).toDp() - indicatorSize / 2 to (finalIndicatorOffsetState.y).toDp()
                }

                val dragObserver = object : DragObserver {
                    override fun onStart(downPosition: Offset) {
                        indicatorOffsetStateByDrag = Offset(
                            x = (downPosition.x - indicatorOffsetStateByPercentage.x),
                            y = indicatorOffsetStateByDrag.y
                        )
                        onGoingDrag = true
                        onSeekStarted(progress)

                        val currentProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                        onSeek(currentProgress.toLong())
                    }

                    override fun onDrag(dragDistance: Offset): Offset {
                        indicatorOffsetStateByDrag = Offset(
                            x = (indicatorOffsetStateByDrag.x + dragDistance.x / 2), // TODO: What the hell?
                            y = indicatorOffsetStateByDrag.y
                        )

                        val currentProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                        onSeek(currentProgress.toLong())

                        return super.onDrag(dragDistance)
                    }

                    override fun onStop(velocity: Offset) {
                        val newProgress = (finalIndicatorOffsetState.x / boxSize.width.toFloat()) * max
                        onSeekStopped(newProgress.toLong())
                        indicatorOffsetStateByDrag = Offset.Zero
                        onGoingDrag = false

                        super.onStop(velocity)
                    }
                }

                Row(modifier = Modifier.matchParentSize()
                    .dragGestureWithPressFilter(
                        dragObserver = dragObserver,
                        startDragImmediately = true,
                        orientation = Orientation.Horizontal
                    )
                ) {

                    Indicator(
                        modifier = Modifier
                            .offset(x = offsetDpX, y = offsetDpY)
                            .preferredSize(indicatorSize)
                    )
                }
            }

            // MAIN PROGRESS
            Row(
                modifier = Modifier.fillMaxWidth().gravity(Alignment.Center)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.weight(1f).onPositioned {
                        if (boxSize != it.size) {
                            boxSize = it.size
                        }
                    },
                    progress = percentage,
                    color = color
                )
            }
        }
    }
}

fun Modifier.dragGestureWithPressFilter(
    dragObserver: DragObserver,
    orientation: Orientation,
    startDragImmediately: Boolean = false
): Modifier = composed {
    val glue = remember { TouchSlopDragGestureDetectorGlue() }
    glue.touchSlopDragObserver = dragObserver

    rawDragGestureFilter(
        glue.rawDragObserver,
        glue::enabledOrStarted,
        orientation
    )
        .dragSlopExceededGestureFilter(glue::enableDrag, orientation = orientation)
        .pressIndicatorGestureFilter(
            onStart = glue::startDrag,
            onStop = { glue.rawDragObserver.onStop(Offset.Zero) },
            enabled = startDragImmediately
        )
}

/**
 * Glues together the logic of RawDragGestureDetector, TouchSlopExceededGestureDetector, and
 * InterruptFlingGestureDetector.
 */
private class TouchSlopDragGestureDetectorGlue {

    lateinit var touchSlopDragObserver: DragObserver
    var started = false
    var enabled = false
    val enabledOrStarted
        get() = started || enabled

    fun enableDrag() {
        enabled = true
    }

    fun startDrag(downPosition: Offset) {
        started = true
        touchSlopDragObserver.onStart(downPosition)
    }

    val rawDragObserver: DragObserver =
        object : DragObserver {
            override fun onStart(downPosition: Offset) {
                if (!started) {
                    touchSlopDragObserver.onStart(downPosition)
                }
            }

            override fun onDrag(dragDistance: Offset): Offset {
                return touchSlopDragObserver.onDrag(dragDistance)
            }

            override fun onStop(velocity: Offset) {
                started = false
                enabled = false
                touchSlopDragObserver.onStop(velocity)
            }

            override fun onCancel() {
                started = false
                enabled = false
                touchSlopDragObserver.onCancel()
            }
        }
}

@Composable
fun Indicator(
    color: Color = MaterialTheme.colors.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = Modifier + modifier) {
        val radius = size.height / 2
        drawCircle(color, radius)
    }
}