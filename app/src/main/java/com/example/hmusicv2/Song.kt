package com.example.hmusicv2

import java.io.Serializable // Nhớ có dòng import này nha

data class Song(
    var id: String? = "",
    var title: String? = "",
    var artist: String? = "",
    var cover: String? = null,
    var audio: String? = "",
    var lyrics: String? = null
) : Serializable