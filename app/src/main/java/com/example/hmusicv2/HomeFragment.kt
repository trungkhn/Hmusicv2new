package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        val btnAvatar = view.findViewById<CardView>(R.id.cvAvatar)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)

        // Cài đặt danh sách Nhạc chính (Trượt dọc)
        val rvSongs = view.findViewById<RecyclerView>(R.id.rvSongs)
        rvSongs.layoutManager = LinearLayoutManager(requireContext())

        // Cài đặt danh sách Lịch Sử (Trượt ngang)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val database = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // --- NHIỆM VỤ 1: HIỆN TÊN CHÀO MỪNG ---
            database.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
                val nameFromDB = snapshot.child("name").value?.toString()
                if (nameFromDB != null && nameFromDB != "null" && nameFromDB.isNotEmpty()) {
                    tvUserName.text = nameFromDB
                } else {
                    tvUserName.text = "Đang tải..."
                }
            }.addOnFailureListener { tvUserName.text = "Đang tải..." }

            // --- NHIỆM VỤ THÊM MỚI: TẢI LỊCH SỬ NGHE GẦN ĐÂY ---
            database.child("Users").child(userId).child("History").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyList = mutableListOf<Song>()
                    for (songSnap in snapshot.children) {
                        val song = songSnap.getValue(Song::class.java)
                        if (song != null) historyList.add(song)
                    }
                    // Đảo ngược danh sách để bài vừa nghe xong nằm lên đầu tiên
                    historyList.reverse()
                    rvHistory.adapter = HistoryAdapter(historyList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            tvUserName.text = "Khách"
        }

        // --- NHIỆM VỤ 2: ĐỔ NHẠC TỪ FIREBASE LÊN DANH SÁCH CHÍNH ---
        database.child("Songs").get().addOnSuccessListener { snapshot ->
            val songList = mutableListOf<Song>()
            for (songSnapshot in snapshot.children) {
                val song = songSnapshot.getValue(Song::class.java)
                if (song != null) songList.add(song)
            }
            rvSongs.adapter = SongAdapter(songList)
        }

        btnAvatar.setOnClickListener {
            startActivity(Intent(requireContext(), PlayerActivity::class.java))
        }

        return view
    }
}