package com.mhasancse15.iptv.domain.model

data class EpgChannel(
    val id: String,
    val displayName: String,
    val icon: String = ""
)

data class EpgProgramme(
    val channelId: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String = ""
)

data class EpgProgrammeWithChannel(
    val programme: EpgProgramme,
    val channel: EpgChannel?
)