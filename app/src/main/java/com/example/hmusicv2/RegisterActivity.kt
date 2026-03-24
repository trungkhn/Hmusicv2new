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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.widget.LinearLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    // 👇 THÊM: Quản lý "Sổ hộ khẩu" Database
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        // 👇 THÊM: Trỏ tới ngăn tủ "Users" trên Database
        // ✅ Sửa lại cho đúng địa chỉ nhà nè:
        database = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        val btnBackRegister = findViewById<ImageView>(R.id.btnBackRegister)
        val btnRegister = findViewById<CardView>(R.id.btnRegister)
        val tvToLogin = findViewById<LinearLayout>(R.id.tvToLogin)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)

        btnBackRegister.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nhập đủ thông tin nha em!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu ít nhất 6 ký tự nha!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Tạo tài khoản Auth trước
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userProfile = User(userId, name, email)

                        if (userId != null) {
                            // Thêm dòng Log này để em kiểm tra trong Logcat xem nó có chạy vào đây không
                            println("DEBUG: Dang ghi vao Database voi ID: $userId")

                            database.child("Users").child(userId).setValue(userProfile)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finishAffinity()
                                    } else {
                                        // NẾU LỖI Ở ĐÂY: App sẽ báo rõ lỗi gì (thường là Permission Denied)
                                        Toast.makeText(this, "Lỗi lưu DB: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        // NẾU LỖI Ở ĐÂY: Email đã tồn tại
                        Toast.makeText(this, "Lỗi Auth: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}