package com.halilibo.composevideoplayer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.halilibo.composevideoplayer.util.FlowDebouncer
import com.halilibo.composevideoplayer.util.set
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
internal class VideoPlayerController(
    private val context: Context,
    private val initialState: VideoPlayerUiState = VideoPlayerUiState(),
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main
) : MediaPlaybackControls, CoroutineScope {

    // InitialState -> init { }
    // PlayerViewReady -> playerViewReady
    // SourceReady
    // PlayerReady

    private val state = MutableStateFlow(initialState)

    /**
     * Some properties in initial state are not applicable until player is ready.
     * These are kept in this container. Once the player is ready for the first time,
     * they are executed and removed.
     */
    private var initialStateRunner: (() -> Unit)? = {
        exoPlayer.seekTo(initialState.currentPosition)
    }

    fun <T> currentState(filter: VideoPlayerUiState.() -> T): T {
        return state.value.filter()
    }

    @Composable
    fun <T> collect(filter: VideoPlayerUiState.() -> T): State<T> {
        return remember {
            state.map { it.filter() }
        }.collectAsState(
            initial = state.value.filter()
        )
    }

    var videoPlayerBackgroundColor: Int = DefaultVideoPlayerBackgroundColor.value.toInt()
        set(value) {
            field = value
            playerView?.setBackgroundColor(value)
        }

    private lateinit var source: VideoPlayerSource
    private var playerView: PlayerView? = null

    private var updateDurationAndPositionJob: Job? = null
    private val playerEventListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)

            if(PlaybackState.of(playbackState) == PlaybackState.READY) {
                initialStateRunner = initialStateRunner?.let {
                    it.invoke()
                    null
                }

                updateDurationAndPositionJob?.cancel()
                updateDurationAndPositionJob = launch {
                    while (this.isActive) {
                        updateDurationAndPosition()
                        delay(250)
                    }
                }
            }

            state.set {
                copy(
                    isPlaying = playWhenReady,
                    playbackState = PlaybackState.of(playbackState)
                )
            }
        }
    }

    private val videoListener = object : VideoListener {
        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)

            state.set {
                copy(videoSize = width.toFloat() to height.toFloat())
            }
        }
    }

    /**
     * Internal exoPlayer instance
     */
    private val exoPlayer = SimpleExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = initialState.isPlaying
            addListener(playerEventListener)
            addVideoListener(videoListener)
        }

    /**
     * Not so efficient way of showing preview in video slider.
     */
    private val previewExoPlayer = SimpleExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = false
        }

    private val previewSeekDebouncer = FlowDebouncer<Long>(200L)

    init {
        exoPlayer.playWhenReady = initialState.isPlaying

        launch {
            previewSeekDebouncer.collect { position ->
                previewExoPlayer.seekTo(position)
            }
        }
    }

    /**
     * A flag to indicate whether source is already set and waiting for
     * playerView to become available.
     */
    private val waitPlayerViewToPrepare = AtomicBoolean(false)

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override fun playPauseToggle() {
        if (exoPlayer.isPlaying) pause()
        else play()
    }

    override fun quickSeekForward() {
        if (state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        state.set { copy(quickSeekAction = QuickSeekAction.forward()) }
    }

    override fun quickSeekRewind() {
        if (state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        state.set { copy(quickSeekAction = QuickSeekAction.rewind()) }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        updateDurationAndPosition()
    }

    fun setSource(source: VideoPlayerSource) {
        this.source = source
        if (playerView == null) {
            waitPlayerViewToPrepare.set(true)
        } else {
            prepare()
        }
    }

    fun enableGestures(isEnabled: Boolean) {
        state.set { copy(gesturesEnabled = isEnabled) }
    }

    fun enableControls(enabled: Boolean) {
        state.set { copy(controlsEnabled = enabled) }
    }

    fun showControls() {
        state.set { copy(controlsVisible = true) }
    }

    fun hideControls() {
        state.set { copy(controlsVisible = false) }
    }

    fun setDraggingProgress(draggingProgress: DraggingProgress?) {
        state.set { copy(draggingProgress = draggingProgress) }
    }

    fun setQuickSeekAction(quickSeekAction: QuickSeekAction) {
        state.set { copy(quickSeekAction = quickSeekAction) }
    }

    private fun updateDurationAndPosition() {
        state.set {
            copy(
                duration = exoPlayer.duration.coerceAtLeast(0),
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
                secondaryProgress = exoPlayer.bufferedPosition.coerceAtLeast(0)
            )
        }
    }

    private fun prepare() {
        fun createVideoSource(): MediaSource {
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, context.packageName)
            )

            return when (val source = source) {
                is VideoPlayerSource.Raw -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(RawResourceDataSource.buildRawResourceUri(source.resId))
                }
                is VideoPlayerSource.Network -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(source.url))
                }
            }
        }

        exoPlayer.prepare(createVideoSource())
        previewExoPlayer.prepare(createVideoSource())
    }

    fun playerViewAvailable(playerView: PlayerView) {
        this.playerView = playerView
        playerView.player = exoPlayer
        playerView.setBackgroundColor(videoPlayerBackgroundColor)

        if (waitPlayerViewToPrepare.compareAndSet(true, false)) {
            prepare()
        }
    }

    fun previewPlayerViewAvailable(playerView: PlayerView) {
        playerView.player = previewExoPlayer
    }

    fun previewSeekTo(position: Long) {
        // position is very accurate. Thumbnail doesn't have to be.
        // Roll to the nearest "even" integer.
        val seconds = position.toInt() / 1000
        val nearestEven = (seconds - seconds.rem(2)).toLong()
        previewSeekDebouncer.put(nearestEven * 1000)
    }

    fun onDispose() {
        exoPlayer.release()
        this.cancel()
    }
}

val DefaultVideoPlayerBackgroundColor = Color.Black