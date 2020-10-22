package com.halilibo.composetube

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.ExperimentalLazyDsl
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.ui.tooling.preview.Preview
import com.google.gson.Gson
import com.halilibo.composetube.ui.ComposeTubeTheme
import com.halilibo.composevideoplayer.MediaPlaybackControls
import com.halilibo.composevideoplayer.VideoPlayer
import com.halilibo.composevideoplayer.VideoPlayerSource

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Composable
fun getVideoList(): List<Video> {
    val context = ContextAmbient.current
    return remember(context) {
        val content = context.resources.openRawResource(R.raw.videos)
            .bufferedReader()
            .use { it.readText() }

        Gson().fromJson(content, VideoList::class.java).videos
    }
}

@OptIn(ExperimentalLazyDsl::class)
@Composable
fun App() {
    val videoList = getVideoList()

    var selectedVideoState by savedInstanceState { videoList[0] }
    var controlsEnabled by savedInstanceState { true }

    var mediaPlaybackControls by remember { mutableStateOf(MediaPlaybackControls()) }
    val context = ContextAmbient.current

    onActive {
        (context as? FragmentActivity)?.lifecycle?.addObserver(object: LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
//                mediaPlaybackControls.pause()
            }
        })
    }

    ComposeTubeTheme {
        // TODO
        var videoSize by savedInstanceState { 0 to 0 }
        val scrollState = rememberLazyListState()

        controlsEnabled = scrollState.firstVisibleItemScrollOffset == 0

        val (videoWidth, videoHeight) = videoSize
        val yScale = with(DensityAmbient.current) {
            (videoHeight - scrollState.firstVisibleItemScrollOffset.toFloat())
                .coerceAtLeast(80.dp.toPx()) / videoHeight
        }

        mediaPlaybackControls = VideoPlayer(
            source = VideoPlayerSource.Network(selectedVideoState.sources.first()),
            backgroundColor = Color.Transparent,
            controlsEnabled = controlsEnabled,
            modifier = Modifier.onGloballyPositioned {
                videoSize = it.size.width to it.size.height
            }
                .drawLayer(
                    scaleY = yScale, scaleX = yScale,
                    translationX = -(videoWidth * (1 - yScale) / 2) + 24 * (1 - yScale),
                    translationY = -(videoHeight * (1 - yScale) / 2) + 24 * (1 - yScale)
                )
                .zIndex(1000f)
        )

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxHeight()
        ) {
            item {
                with(DensityAmbient.current) {
                    Box(
                        modifier = Modifier.preferredSize(
                            videoWidth.toDp(),
                            videoHeight.toDp()
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    selectedVideoState.description,
                    style = TextStyle(
                        fontSize = TextUnit.Companion.Sp(14)
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(items = videoList) { video ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(video.title)

                    Button(onClick = {
                        selectedVideoState = video
                    }) {
                        Text("Play")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    App()
}