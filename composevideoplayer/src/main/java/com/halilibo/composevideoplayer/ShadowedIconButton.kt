package com.halilibo.composevideoplayer

import androidx.compose.foundation.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorAsset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun ShadowedIcon(
    icon: VectorAsset,
    iconSize: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier + modifier) {
        Icon(
                asset = icon.copy(defaultWidth = iconSize, defaultHeight = iconSize),
                tint = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.offset(2.dp, 2.dp).then(modifier)
        )
        Icon(
                asset = icon.copy(defaultWidth = iconSize, defaultHeight = iconSize),
                modifier = Modifier + modifier
        )
    }
}