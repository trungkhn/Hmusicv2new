package com.example.hmusicv2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private lateinit var tvProfileNameBig: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvLogoutText: TextView
    private lateinit var icLogout: ImageView
    private lateinit var btnLogoutProfile: RelativeLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // 1. Ánh xạ View cơ bản
        tvProfileNameBig = view.findViewById(R.id.tvProfileNameBig)
        tvProfileRole = view.findViewById(R.id.tvProfileRole)
        tvLogoutText = view.findViewById(R.id.tvLogoutText)
        icLogout = view.findViewById(R.id.icLogout)
        btnLogoutProfile = view.findViewById(R.id.btnLogoutProfile)

        // 2. Kích hoạt logic kiểm tra người dùng (truyền view vào để tìm các icon mới)
        checkUserStatus(view)

        return view
    }

    private fun checkUserStatus(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Ánh xạ các linh kiện mới thêm trong XML
        val cvEditAvatarBadge = view.findViewById<View>(R.id.cvEditAvatarBadge)
        val ivEditNamePen = view.findViewById<ImageView>(R.id.ivEditNamePen)
        val layoutChangeAvatar = view.findViewById<View>(R.id.layoutChangeAvatar)
        val ivProfileAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)

        if (currentUser != null) {
            // ==========================================
            // TRẠNG THÁI 1: THÀNH VIÊN (ĐÃ ĐĂNG NHẬP)
            // ==========================================
            tvProfileRole.visibility = View.VISIBLE

            // Hiện đồ nghề chỉnh sửa
            cvEditAvatarBadge?.visibility = View.VISIBLE
            ivEditNamePen?.visibility = View.VISIBLE
            layoutChangeAvatar?.isClickable = true

            tvLogoutText.text = "Đăng xuất"
            tvLogoutText.setTextColor(Color.parseColor("#FF3B30")) // Chữ Đỏ iOS
            icLogout.setColorFilter(Color.parseColor("#FF3B30"))
            icLogout.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)

            // Tải tên từ Firebase Server Châu Á
            val database = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
            database.child("Users").child(currentUser.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    tvProfileNameBig.text = snapshot.child("name").value.toString()
                }
            }

            // Xử lý sự kiện bấm nút Đăng xuất
            btnLogoutProfile.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            // Xử lý sự kiện bấm Cây bút -> Hiện bảng đổi tên
            ivEditNamePen?.setOnClickListener {
                showEditNameDialog()
            }

        } else {
            // ==========================================
            // TRẠNG THÁI 2: KHÁCH HÀNG (CHƯA ĐĂNG NHẬP)
            // ==========================================
            tvProfileNameBig.text = "Tài Khoản Khách"
            tvProfileRole.visibility = View.GONE // Ẩn chữ "Thành viên..."

            // Giấu sạch đồ nghề chỉnh sửa đi
            cvEditAvatarBadge?.visibility = View.GONE
            ivEditNamePen?.visibility = View.GONE
            layoutChangeAvatar?.isClickable = false

            // Set ảnh mặc định
            ivProfileAvatar?.setImageResource(android.R.drawable.ic_menu_gallery)

            tvLogoutText.text = "Đăng nhập"
            tvLogoutText.setTextColor(Color.parseColor("#007AFF")) // Chữ Xanh Dương iOS
            icLogout.setColorFilter(Color.parseColor("#007AFF"))
            icLogout.setImageResource(android.R.drawable.ic_menu_revert) // Icon quay lại

            // Xử lý sự kiện bấm nút Đăng nhập
            btnLogoutProfile.setOnClickListener {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // ==========================================
    // HÀM HIỂN THỊ BẢNG ĐỔI TÊN (Giữ lại từ code cũ của em)
    // ==========================================
    private fun showEditNameDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null)
        val etName = view.findViewById<EditText>(R.id.etEditProfileName)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelEdit)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveName)

        // Điền sẵn tên cũ vào cho dễ sửa
        etName.setText(tvProfileNameBig.text.toString())

        val dialog = AlertDialog.Builder(requireContext())
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
            Toast.makeText(requireContext(), "Cập nhật tên thành công!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi cập nhật: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}