package com.halilibo.composevideoplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun PlayerSurface(
    modifier: Modifier = Modifier,
    onPlayerViewAvailable: (PlayerView) -> Unit = {}
) {

    AndroidView(
        viewBlock = { context ->
            PlayerView(context).apply {
                useController = false
            }
        },
        modifier = modifier,
        update = {
            onPlayerViewAvailable(it)
        }
    )
}