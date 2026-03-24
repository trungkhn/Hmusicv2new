package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import android.widget.LinearLayout

class LoginActivity : AppCompatActivity() {

    // Khai báo công cụ kiểm tra chìa khóa của Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Khởi tạo "Bác bảo vệ" Firebase
        auth = FirebaseAuth.getInstance()

        val btnLogin = findViewById<CardView>(R.id.btnLogin)
        val tvToRegister = findViewById<LinearLayout>(R.id.tvToRegister)
        val btnBackLogin = findViewById<ImageView>(R.id.btnBackLogin)

        // Ánh xạ 2 ô nhập liệu của em
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)

        // 1. Nút Back -> Về trang Đăng ký
        btnBackLogin.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 2. BẮT ĐẦU PHÙ PHÉP ĐĂNG NHẬP
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // Kiểm tra xem có nhập thiếu không
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng không để trống thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Dừng lại, bắt nhập cho xong
            }

            // Đưa thông tin cho Firebase kiểm tra
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // BÙM! Mở cửa!
                        Toast.makeText(this, "Đăng nhập thành công! Chào mừng trở lại!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finishAffinity() // Quét sạch các màn hình ở ngoài cửa
                    } else {
                        // Khóa cửa (Sai pass, chưa có tài khoản, v.v.)
                        Toast.makeText(this, "Nhập sai thông tin: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // 3. Bấm chữ Đăng ký -> Mở trang Đăng ký
        tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}