package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 1. Tìm đúng cái Avatar góc trái trên cùng (đã có ID là cvAvatar)
        val btnAvatar = view.findViewById<CardView>(R.id.cvAvatar)

        // 2. Gắn lệnh: Bấm vào Avatar thì mở màn hình Đĩa Than
        btnAvatar.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}