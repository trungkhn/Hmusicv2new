package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<CardView>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)
        val btnBackLogin = findViewById<ImageView>(R.id.btnBackLogin) // Ánh xạ nút Back

        // 1. Nút Back -> Chủ động mở lại trang Đăng ký
        btnBackLogin.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // Mở trang Đăng ký xong thì đóng trang Đăng nhập này lại
        }

        // 2. Bấm Đăng nhập -> Vào Trang chủ
        btnLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity() // Dọn sạch màn hình quảng cáo và đăng nhập bên dưới
        }

        // 3. Bấm chữ Đăng ký -> Mở trang Đăng ký và đóng trang này
        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}