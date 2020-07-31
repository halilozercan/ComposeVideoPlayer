package com.halilibo.composetube
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.*
import androidx.ui.foundation.Box
import androidx.ui.foundation.ScrollableColumn
import androidx.ui.foundation.Text
import androidx.ui.foundation.rememberScrollState
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.text.TextStyle
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.IntSize
import androidx.ui.unit.TextUnit
import androidx.ui.unit.dp
import com.google.gson.Gson
import com.halilibo.composetube.ui.ComposeTubeTheme
import com.halilibo.composevideoplayer.MediaPlaybackControls
import com.halilibo.composevideoplayer.VideoPlayer
import com.halilibo.composevideoplayer.VideoPlayerSource

class MainActivity : AppCompatActivity() {

    private var mediaPlaybackControls: MediaPlaybackControls? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val videoList = getVideoList()

            val (videoState, setVideoState) = state { videoList[0] }
            val (controlsEnabled, setControlsEnabled) = state { true }

            ComposeTubeTheme {
                val scrollState = rememberScrollState()
                var videoSize by state { IntSize(3000,3000) }

                setControlsEnabled(scrollState.value == 0f)

                val yScale = with(DensityAmbient.current) {
                    (videoSize.height - scrollState.value).coerceAtLeast(80.dp.toPx()) / videoSize.height
                }

                mediaPlaybackControls = VideoPlayer(
                    VideoPlayerSource.Network(videoState.sources.first()),
                    Color.Transparent,
                    controlsEnabled = controlsEnabled,
                    modifier = Modifier.onPositioned {
                        videoSize = it.size
                    }
                        .drawLayer(scaleY = yScale, scaleX = yScale,
                            translationX = - (videoSize.width * (1-yScale) / 2 ) + 24*(1-yScale),
                            translationY = - (videoSize.height * (1-yScale) / 2 ) + 24*(1-yScale)
                        )
                        .zIndex(1000f)
                )


                ScrollableColumn(
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    with(DensityAmbient.current) {
                        Box(modifier = Modifier.preferredSize(videoSize.width.toDp(), videoSize.height.toDp()))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(videoState.description,
                        style = TextStyle(
                            fontSize = TextUnit.Companion.Sp(14)
                        ),
                        modifier = Modifier.padding(16.dp)
                    )

                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        videoList.forEach {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalGravity = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(it.title)

                                Button(onClick = {
                                    setVideoState(it)
                                    scrollState.smoothScrollTo(0f)
                                }) {
                                    Text("Play")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlaybackControls?.pause()
    }

    fun getVideoList(): List<Video> {
        val content = resources.openRawResource(R.raw.videos)
            .bufferedReader()
            .use { it.readText() }

        return Gson().fromJson(content, VideoList::class.java).videos
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Text("Default")
}