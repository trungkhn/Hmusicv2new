package com.example.hmusicv2

import java.io.Serializable // Nhớ có dòng import này nha

data class Song(
    val id: String? = "",
    val title: String? = "",
    val artist: String? = "",
    val cover: String? = null,
    val audio: String? = "",
    var lyrics: String? = null
) : Serializable