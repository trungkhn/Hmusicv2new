package com.example.hmusicv2

import android.graphics.Color
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

class LibraryFragment : Fragment() {

    private lateinit var rvLibrary: RecyclerView

    // 2 Kho chứa dữ liệu riêng biệt
    private var playlistList = mutableListOf<LibraryPlaylist>()
    private var likedSongList = mutableListOf<Song>()

    // Đánh dấu xem người dùng đang đứng ở Tab nào
    private var currentTab = "ALL" // "ALL", "PLAYLISTS", hoặc "LIKED"

    // Các nút bấm
    private lateinit var cvFilterAll: CardView
    private lateinit var tvFilterAll: TextView
    private lateinit var cvFilterPlaylists: CardView
    private lateinit var tvFilterPlaylists: TextView
    private lateinit var cvFilterLiked: CardView
    private lateinit var tvFilterLiked: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        rvLibrary = view.findViewById(R.id.rvLikedSongs)
        rvLibrary.layoutManager = LinearLayoutManager(requireContext())

        // 1. Ánh xạ các nút
        cvFilterAll = view.findViewById(R.id.cvFilterAll)
        tvFilterAll = view.findViewById(R.id.tvFilterAll)
        cvFilterPlaylists = view.findViewById(R.id.cvFilterPlaylists)
        tvFilterPlaylists = view.findViewById(R.id.tvFilterPlaylists)
        cvFilterLiked = view.findViewById(R.id.cvFilterLiked)
        tvFilterLiked = view.findViewById(R.id.tvFilterLiked)

        // 2. Chạy lên Firebase tải ĐỒNG THỜI cả 2 kho về
        loadPlaylistsFromFirebase()
        loadLikedSongsFromFirebase()

        // 3. Sự kiện bấm nút "All" (Tạm thời cho hiện Playlists)
        cvFilterAll.setOnClickListener {
            currentTab = "ALL"
            updateFilterUI(cvFilterAll, tvFilterAll)
            refreshRecyclerView()
        }

        // 4. Sự kiện bấm nút "Playlists"
        cvFilterPlaylists.setOnClickListener {
            currentTab = "PLAYLISTS"
            updateFilterUI(cvFilterPlaylists, tvFilterPlaylists)
            refreshRecyclerView()
        }

        // 5. Sự kiện bấm nút "Liked Songs"
        cvFilterLiked.setOnClickListener {
            currentTab = "LIKED"
            updateFilterUI(cvFilterLiked, tvFilterLiked)
            refreshRecyclerView()
        }

        return view
    }

    // Hàm đổi màu nút: Nút nào được chọn thì Xanh, còn lại Xám
    private fun updateFilterUI(selectedCard: CardView, selectedText: TextView) {
        cvFilterAll.setCardBackgroundColor(Color.parseColor("#282828"))
        tvFilterAll.setTextColor(Color.WHITE)
        cvFilterPlaylists.setCardBackgroundColor(Color.parseColor("#282828"))
        tvFilterPlaylists.setTextColor(Color.WHITE)
        cvFilterLiked.setCardBackgroundColor(Color.parseColor("#282828"))
        tvFilterLiked.setTextColor(Color.WHITE)

        selectedCard.setCardBackgroundColor(Color.parseColor("#1DB954"))
        selectedText.setTextColor(Color.BLACK)
    }

    // Hàm quyết định xem nhét Adapter nào vào RecyclerView
    private fun refreshRecyclerView() {
        if (currentTab == "ALL" || currentTab == "PLAYLISTS") {
            // Nhét Adapter của Playlist
            val adapter = LibraryPlaylistAdapter(playlistList) { playlist ->
                showDeleteDialog(playlist.name)
            }
            rvLibrary.adapter = adapter
        } else if (currentTab == "LIKED") {
            // Nhét Adapter của Bài hát (cái mà mình dùng ở Home/Search)
            val adapter = SongAdapter(likedSongList)
            rvLibrary.adapter = adapter
        }
    }

    private fun loadPlaylistsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("Playlists")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playlistList.clear()
                for (playSnapshot in snapshot.children) {
                    val playlistName = playSnapshot.child("info").child("name").value?.toString() ?: playSnapshot.key ?: "Không tên"
                    val songsSnapshot = playSnapshot.child("songs")
                    val songCount = songsSnapshot.childrenCount.toInt()

                    var coverUrl: String? = null
                    if (songCount > 0) {
                        coverUrl = songsSnapshot.children.first().child("cover").value?.toString()
                    }
                    playlistList.add(LibraryPlaylist(playlistName, songCount, coverUrl))
                }
                refreshRecyclerView() // Có dữ liệu mới thì tự động cập nhật lại màn hình
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLikedSongsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("LikedSongs")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                likedSongList.clear()
                for (songSnapshot in snapshot.children) {
                    val song = songSnapshot.getValue(Song::class.java)
                    if (song != null) {
                        likedSongList.add(song)
                    }
                }
                refreshRecyclerView() // Có dữ liệu mới thì tự động cập nhật lại màn hình
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDeleteDialog(playlistName: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Xóa Playlist")
        builder.setMessage("Bạn có chắc chắn muốn xóa danh sách phát '$playlistName' không?")

        builder.setPositiveButton("Xóa") { dialog, _ ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .reference.child("Users").child(userId).child("Playlists").child(playlistName)

                dbRef.removeValue().addOnSuccessListener {
                    android.widget.Toast.makeText(requireContext(), "Đã xóa $playlistName", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}