package com.example.hmusicv2

import android.media.MediaPlayer

object MyMediaPlayer {
    var instance: MediaPlayer? = null

    // 👇 THÊM 2 BIẾN NÀY ĐỂ NHỚ BÀI ĐANG HÁT 👇
    var currentPlaylist: ArrayList<Song> = ArrayList()
    var currentIndex: Int = -1

    fun getMediaPlayer(): MediaPlayer {
        if (instance == null) {
            instance = MediaPlayer()
        }
        return instance!!
    }
}