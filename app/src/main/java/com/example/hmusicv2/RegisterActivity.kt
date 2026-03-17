package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnBackRegister = findViewById<ImageView>(R.id.btnBackRegister)
        val btnRegister = findViewById<CardView>(R.id.btnRegister)
        val tvToLogin = findViewById<TextView>(R.id.tvToLogin)

        // 1. Nút Back -> Xóa trang này để lòi trang bên dưới ra (thường là Quảng cáo hoặc Đăng nhập)
        btnBackRegister.setOnClickListener {
            finish()
        }

        // 2. Bấm Đăng ký -> Vào Trang chủ
        btnRegister.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity() // Dọn sạch các màn hình bên dưới
        }

        // 3. Bấm chữ Đăng nhập -> Mở trang Đăng nhập và đóng trang này
        tvToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}