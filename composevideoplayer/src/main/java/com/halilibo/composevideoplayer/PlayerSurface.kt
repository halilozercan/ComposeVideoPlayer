package com.halilibo.composevideoplayer

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun PlayerSurface(
        modifier: Modifier = Modifier,
        onPlayerViewAvailable: (PlayerView) -> Unit = {}
) {

    AndroidView(resId = R.layout.surface, modifier = Modifier + modifier) { layout ->
        layout.findViewById<PlayerView>(R.id.player_view).let { playerView ->
            onPlayerViewAvailable(playerView)
        }
    }
}