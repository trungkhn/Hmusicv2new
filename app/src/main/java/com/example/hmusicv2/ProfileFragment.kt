package com.example.hmusicv2

import android.content.Intent // Chị thêm dòng này để máy hiểu chữ Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Bắt sự kiện bấm nút Đăng xuất
        val btnLogout = view.findViewById<CardView>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            // Chuyển về màn hình Login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            // Đóng toàn bộ ứng dụng chính lại
            requireActivity().finish()
        }

        return view // Đã bổ sung dòng chốt hạ quan trọng này
    }
}