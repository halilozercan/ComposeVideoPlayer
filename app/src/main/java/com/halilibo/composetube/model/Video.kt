package com.halilibo.composetube.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Video(
    val description: String,
    val sources: List<String>,
    val subtitle: String,
    val title: String,
    val thumb: String
): Parcelable

@Parcelize
data class VideoList(
    val videos: List<Video>
): Parcelable