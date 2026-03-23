package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private lateinit var tvProfileNameBig: TextView
    private lateinit var itemEditProfile: RelativeLayout
    private lateinit var btnLogoutProfile: RelativeLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvProfileNameBig = view.findViewById(R.id.tvProfileNameBig)
        itemEditProfile = view.findViewById(R.id.itemEditProfile)
        btnLogoutProfile = view.findViewById(R.id.btnLogoutProfile)

        loadCurrentName()

        // 1. Khi bấm vào dòng "Chỉnh sửa tên" -> Hiện Dialog nhập tên
        itemEditProfile.setOnClickListener {
            showEditNameDialog()
        }

        // 2. Khi bấm vào dòng "Đăng xuất"
        btnLogoutProfile.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private fun loadCurrentName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("name")

        dbRef.get().addOnSuccessListener { snapshot ->
            val name = snapshot.value?.toString()
            tvProfileNameBig.text = if (name != null && name != "null") name else "Dũng sĩ Hmusic"
        }
    }

    // --- PHÉP THUẬT GIẤU Ô NHẬP TÊN VÀO ĐÂY ---
    private fun showEditNameDialog() {
        // 1. Nạp đúng cái file em vừa gửi
        val view = layoutInflater.inflate(R.layout.dialog_edit_name, null)

        // 2. Tìm đúng ID em đã đặt trong XML
        val etName = view.findViewById<EditText>(R.id.etEditProfileName)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelEdit)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveName)

        // Điền sẵn tên cũ vào cho dễ sửa
        etName.setText(tvProfileNameBig.text.toString())

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        // Làm nền trong suốt để thấy bo góc của dialog_background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateProfileName(newName) // Gọi hàm lưu lên Firebase
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Tên không được để trống!", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun updateProfileName(newName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("name")

        dbRef.setValue(newName).addOnSuccessListener {
            tvProfileNameBig.text = newName
            Toast.makeText(requireContext(), "Đã cập nhật tên!", Toast.LENGTH_SHORT).show()
        }
    }
}