package com.mhasancse15.iptv.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val category: String = "General",
    val country: String = "Bangladesh",
    val tvgId: String = "",
    val tvgName: String = "",
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long = 0L
) : Parcelable
