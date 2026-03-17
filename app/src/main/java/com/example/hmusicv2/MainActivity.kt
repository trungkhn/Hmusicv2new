package com.example.hmusicv2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Mặc định mở Trang Chủ (Home) khi vừa vào App
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Lắng nghe thao tác bấm vào Menu Lơ lửng
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment()) // Mở Trang chủ
                    true
                }
                R.id.nav_library -> {
                    loadFragment(LibraryFragment()) // Mở Thư viện (My Music)
                    true
                }
                // Các tab Search và Profile mình sẽ làm sau
                else -> false
            }
        }
        // Lắng nghe thao tác bấm vào Menu Lơ lửng
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment()) // Mở Trang chủ
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment()) // Mở Tìm kiếm (mới thêm)
                    true
                }
                R.id.nav_library -> {
                    loadFragment(LibraryFragment()) // Mở Thư viện
                    true
                }
                // Tab Profile (Tôi) mình sẽ làm nốt ở bước cuối
                else -> false
            }
        }
        // Lắng nghe thao tác bấm vào Menu Lơ lửng
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_library -> {
                    loadFragment(LibraryFragment())
                    true
                }
                // 👇 THÊM NÚT PROFILE VÀO ĐÂY 👇
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Hàm hỗ trợ để hoán đổi các màn hình với nhau
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }
}