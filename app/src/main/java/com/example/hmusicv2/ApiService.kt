package com.example.hmusicv2 // Vẫn giữ nguyên package máy em nha

import retrofit2.http.GET

interface ApiService {
    // Gọi phương thức GET để lấy danh sách nhạc từ cái đuôi "/songs"
    @GET("songs")
    suspend fun getSongs(): List<Song>
}