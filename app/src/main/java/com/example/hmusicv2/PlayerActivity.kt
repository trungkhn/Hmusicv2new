package com.example.hmusicv2

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // Ánh xạ nút Back và gán lệnh thoát màn hình
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}