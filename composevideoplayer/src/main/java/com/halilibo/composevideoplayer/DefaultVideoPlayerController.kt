package com.halilibo.composevideoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.halilibo.composevideoplayer.util.FlowDebouncer
import com.halilibo.composevideoplayer.util.set
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultVideoPlayerController(
    private val context: Context,
    private val initialState: VideoPlayerState,
    private val coroutineScope: CoroutineScope
) : VideoPlayerController {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<VideoPlayerState>
        get() = _state.asStateFlow()

    /**
     * Some properties in initial state are not applicable until player is ready.
     * These are kept in this container. Once the player is ready for the first time,
     * they are applied and removed.
     */
    private var initialStateRunner: (() -> Unit)? = {
        exoPlayer.seekTo(initialState.currentPosition)
    }

    fun <T> currentState(filter: (VideoPlayerState) -> T): T {
        return filter(_state.value)
    }

    @Composable
    fun collect(): State<VideoPlayerState> {
        return _state.collectAsState()
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T> collect(filter: VideoPlayerState.() -> T): State<T> {
        return remember(filter) {
            _state.map { it.filter() }
        }.collectAsState(
            initial = _state.value.filter()
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    var videoPlayerBackgroundColor: Int = DefaultVideoPlayerBackgroundColor.value.toInt()
        set(value) {
            field = value
            playerView?.setBackgroundColor(value)
        }

    private lateinit var source: VideoPlayerSource
    private var playerView: PlayerView? = null

    private var updateDurationAndPositionJob: Job? = null


    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            val width = videoSize.width
            val height = videoSize.height

            _state.set {
                copy(videoSize = width.toFloat() to height.toFloat())
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (PlaybackState.of(playbackState) == PlaybackState.READY) {
                initialStateRunner = initialStateRunner?.let {
                    it.invoke()
                    null
                }

                updateDurationAndPositionJob?.cancel()
                updateDurationAndPositionJob = coroutineScope.launch {
                    while (this.isActive) {
                        updateDurationAndPosition()
                        delay(250)
                    }
                }
            }

            _state.set {
                copy(
                    isPlaying = playWhenReady,
                    playbackState = PlaybackState.of(playbackState)
                )
            }
        }
    }

    /**
     * Internal exoPlayer instance
     */
    private val exoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = initialState.isPlaying
            addListener(playerListener)

        }

    /**
     * Not so efficient way of showing preview in video slider.
     */
    private val previewExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = false
        }

    private val previewSeekDebouncer = FlowDebouncer<Long>(200L)

    init {
        exoPlayer.playWhenReady = initialState.isPlaying

        coroutineScope.launch {
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
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        _state.set { copy(quickSeekAction = QuickSeekAction.forward()) }
    }

    override fun quickSeekRewind() {
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        _state.set { copy(quickSeekAction = QuickSeekAction.rewind()) }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        updateDurationAndPosition()
    }

    override fun setSource(source: VideoPlayerSource) {
        this.source = source
        if (playerView == null) {
            waitPlayerViewToPrepare.set(true)
        } else {
            prepare()
        }
    }

    fun enableGestures(isEnabled: Boolean) {
        _state.set { copy(gesturesEnabled = isEnabled) }
    }

    fun enableControls(enabled: Boolean) {
        _state.set { copy(controlsEnabled = enabled) }
    }

    fun showControls() {
        _state.set { copy(controlsVisible = true) }
    }

    fun hideControls() {
        _state.set { copy(controlsVisible = false) }
    }

    fun setDraggingProgress(draggingProgress: DraggingProgress?) {
        _state.set { copy(draggingProgress = draggingProgress) }
    }

    fun setQuickSeekAction(quickSeekAction: QuickSeekAction) {
        _state.set { copy(quickSeekAction = quickSeekAction) }
    }

    private fun updateDurationAndPosition() {
        _state.set {
            copy(
                duration = exoPlayer.duration.coerceAtLeast(0),
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
                secondaryProgress = exoPlayer.bufferedPosition.coerceAtLeast(0)
            )
        }
    }

    private fun prepare() {
        fun createVideoSource(): MediaSource {
            val baseFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent(Util.getUserAgent(context, context.packageName))
            }

            val dataSourceFactory: DefaultDataSource.Factory =
                DefaultDataSource.Factory(context, baseFactory)

            return when (val source = source) {
                is VideoPlayerSource.Raw -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(
                            MediaItem.fromUri(
                                RawResourceDataSource.buildRawResourceUri(
                                    source.resId
                                )
                            )
                        )
                }
                is VideoPlayerSource.Network -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(source.url)))
                }
            }
        }

        exoPlayer.setMediaSource(createVideoSource())
        previewExoPlayer.setMediaSource(createVideoSource())

        exoPlayer.prepare()
        previewExoPlayer.prepare()
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
        coroutineScope.launch {
            previewSeekDebouncer.put(nearestEven * 1000)
        }
    }

    override fun reset() {
        exoPlayer.stop()
        previewExoPlayer.stop()
    }
}

val DefaultVideoPlayerBackgroundColor = Color.Black