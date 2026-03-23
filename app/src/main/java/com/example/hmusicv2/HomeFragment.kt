package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // --- 1. ÁNH XẠ CÁC VIEW ---
        val btnAvatar = view.findViewById<CardView>(R.id.cvAvatar)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val imgHero = view.findViewById<ImageView>(R.id.imgHero)

        // Ánh xạ 3 ImageView nền mới thêm trong XML
        val imgFavBackground = view.findViewById<ImageView>(R.id.imgFavBackground)
        val imgDailyBackground = view.findViewById<ImageView>(R.id.imgDailyBackground)
        val imgDiscoveryBackground = view.findViewById<ImageView>(R.id.imgDiscoveryBackground)

        // --- 2. TẢI HÌNH ẢNH BẰNG GLIDE ---
        // Tải ảnh Hero (Đã thêm centerCrop để ảnh không bị méo)
        Glide.with(this)
            .load("https://i.pinimg.com/736x/d5/2d/dd/d52ddde5cfbce41713e96e497e8ef61f.jpg")
            .centerCrop()
            .into(imgHero)

        // Các URL Pinterest bạn đã chọn
        val favoriteImageUrl = "https://i.pinimg.com/1200x/5f/19/dd/5f19dd19601610d4c7cc7e9da5345328.jpg"
        val discoveryImageUrl = "https://i.pinimg.com/1200x/bd/26/93/bd2693589a2a1a40def142d09506a0af.jpg"
        val dailyImageUrl = "https://i.pinimg.com/736x/6d/99/e8/6d99e8b51c0c4bb1a0e1a21d106fa9f0.jpg"

        // Đổ ảnh vào Favorites
        imgFavBackground?.let {
            it.visibility = View.VISIBLE
            Glide.with(this).load(favoriteImageUrl).centerCrop().into(it)
        }

        // Đổ ảnh vào Daily Mix
        imgDailyBackground?.let {
            Glide.with(this).load(dailyImageUrl).centerCrop().into(it)
        }

        // Đổ ảnh vào Discovery
        imgDiscoveryBackground?.let {
            Glide.with(this).load(discoveryImageUrl).centerCrop().into(it)
        }

        // --- 3. CÀI ĐẶT RECYCLERVIEW ---
        // Cài đặt danh sách Nhạc chính (Trượt dọc)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvSongs)
        rvSongs.layoutManager = LinearLayoutManager(requireContext())

        // Cài đặt danh sách Lịch Sử (Trượt ngang)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // --- 4. FIREBASE DATA ---
        val database = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // Lấy tên người dùng
            database.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
                val nameFromDB = snapshot.child("name").value?.toString()
                if (nameFromDB != null && nameFromDB != "null" && nameFromDB.isNotEmpty()) {
                    tvUserName.text = nameFromDB
                } else {
                    tvUserName.text = "Đang tải..."
                }
            }.addOnFailureListener { tvUserName.text = "Đang tải..." }

            // Tải lịch sử nghe
            database.child("Users").child(userId).child("History").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyList = ArrayList<Song>() // Khuyên dùng ArrayList để truyền dữ liệu an toàn
                    for (songSnap in snapshot.children) {
                        val song = songSnap.getValue(Song::class.java)
                        if (song != null) historyList.add(song)
                    }
                    historyList.reverse()
                    rvHistory.adapter = HistoryAdapter(historyList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            tvUserName.text = "Khách"
        }

        // Đổ nhạc chính từ Firebase
        database.child("Songs").get().addOnSuccessListener { snapshot ->
            val songList = ArrayList<Song>()
            for (songSnapshot in snapshot.children) {
                val song = songSnapshot.getValue(Song::class.java)
                if (song != null) songList.add(song)
            }
            rvSongs.adapter = SongAdapter(songList)
        }

        // Mở PlayerActivity khi bấm Avatar
        btnAvatar.setOnClickListener {
            startActivity(Intent(requireContext(), PlayerActivity::class.java))
        }

        return view
    }
}