package com.example.hmusicv2

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LibraryFragment : Fragment() {

    private lateinit var rvLibrary: RecyclerView
    private lateinit var btnAddPlaylist: ImageView

    // Dữ liệu
    private var playlistList = mutableListOf<Playlist>() // Sử dụng class Playlist mới
    private var likedSongList = mutableListOf<Song>()

    // Tab hiện tại
    private var currentTab = "ALL" // "ALL", "PLAYLISTS", "LIKED"

    // Nút lọc
    private lateinit var cvFilterAll: CardView
    private lateinit var tvFilterAll: TextView
    private lateinit var cvFilterPlaylists: CardView
    private lateinit var tvFilterPlaylists: TextView
    private lateinit var cvFilterLiked: CardView
    private lateinit var tvFilterLiked: TextView

    // Cấu hình Firebase
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val dbRefPlaylists by lazy {
        FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId ?: "").child("Playlists")
    }
    private val dbRefLikedSongs by lazy {
        FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId ?: "").child("LikedSongs")
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        rvLibrary = view.findViewById(R.id.rvLibrary) // Sửa lại ID cho đúng file giao diện mới
        rvLibrary.layoutManager = LinearLayoutManager(requireContext())

        btnAddPlaylist = view.findViewById(R.id.btnAddPlaylist)

        // 1. Ánh xạ các nút lọc
        cvFilterAll = view.findViewById(R.id.cvFilterAll)
        tvFilterAll = view.findViewById(R.id.tvFilterAll)
        cvFilterPlaylists = view.findViewById(R.id.cvFilterPlaylists)
        tvFilterPlaylists = view.findViewById(R.id.tvFilterPlaylists)
        cvFilterLiked = view.findViewById(R.id.cvFilterLiked)
        tvFilterLiked = view.findViewById(R.id.tvFilterLiked)

        // 2. Bắt sự kiện tạo Playlist mới
        btnAddPlaylist.setOnClickListener {
            showAddOrEditPlaylistDialog()
        }

        // 3. Tải dữ liệu Firebase
        loadPlaylistsFromFirebase()
        loadLikedSongsFromFirebase()

        // 4. Cài đặt sự kiện chuyển Tab
        setupTabClickListeners()

        return view
    }

    private fun setupTabClickListeners() {
        cvFilterAll.setOnClickListener {
            currentTab = "ALL"
            updateFilterUI(cvFilterAll, tvFilterAll)
            refreshRecyclerView()
        }

        cvFilterPlaylists.setOnClickListener {
            currentTab = "PLAYLISTS"
            updateFilterUI(cvFilterPlaylists, tvFilterPlaylists)
            refreshRecyclerView()
        }

        cvFilterLiked.setOnClickListener {
            currentTab = "LIKED"
            updateFilterUI(cvFilterLiked, tvFilterLiked)
            refreshRecyclerView()
        }
    }

    // Đổi màu nút chuẩn giao diện Sáng
    private fun updateFilterUI(selectedCard: CardView, selectedText: TextView) {
        // Reset tất cả về màu Trắng/Xám
        cvFilterAll.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        tvFilterAll.setTextColor(Color.parseColor("#1D1D1F"))
        cvFilterPlaylists.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        tvFilterPlaylists.setTextColor(Color.parseColor("#1D1D1F"))
        cvFilterLiked.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        tvFilterLiked.setTextColor(Color.parseColor("#1D1D1F"))

        // Nút được chọn thành màu Hồng đỏ iOS
        selectedCard.setCardBackgroundColor(Color.parseColor("#FF2D55"))
        selectedText.setTextColor(Color.WHITE)
    }

    private fun refreshRecyclerView() {
        if (currentTab == "ALL" || currentTab == "PLAYLISTS") {
            // 1. Tạo một danh sách hiển thị tạm thời
            val displayList = mutableListOf<Playlist>()

            // 2. Nếu đang ở tab "Tất cả", nhét thêm mục "Bài hát đã thích" lên đầu tiên
            if (currentTab == "ALL") {
                displayList.add(
                    Playlist(
                        id = "LIKED_SONGS_SPECIAL_ID", // Đặt 1 ID đặc biệt để nhận diện
                        name = "Bài hát đã thích",
                        cover = "",
                        songCount = likedSongList.size // Lấy số lượng bài hát thực tế
                    )
                )
            }
            if (currentTab == "ALL") {
                // Nhét thêm mục Nhạc đã tải
                displayList.add(
                    Playlist(
                        id = "OFFLINE_SONGS_SPECIAL_ID",
                        name = "Nhạc đã tải",
                        cover = "",
                        songCount = DatabaseHelper(requireContext()).getAllOfflineSongs().size // Đếm số bài trong máy
                    )
                )
            }
            // 3. Đổ các Playlist bình thường mà bạn tạo vào nối tiếp phía sau
            displayList.addAll(playlistList)

            val adapter = LibraryPlaylistAdapter(
                playlistList = displayList,
                onPlaylistClick = { playlist ->
                    if (playlist.id == "LIKED_SONGS_SPECIAL_ID") {
                        // CHỨC NĂNG CHUYỂN TAB: Nếu bấm vào "Bài hát đã thích", tự động nhảy sang tab Liked
                        currentTab = "LIKED"
                        updateFilterUI(cvFilterLiked, tvFilterLiked) // Đổi màu nút trên cùng
                        refreshRecyclerView()
                    }
                    else if (playlist.id == "OFFLINE_SONGS_SPECIAL_ID") {
                        // MỞ MÀN HÌNH NHẠC OFFLINE
                        val intent = Intent(requireContext(), OfflineMusicActivity::class.java)
                        startActivity(intent)
                    }
                    else {
                        // Bấm vào Playlist bình thường -> Mở trang Chi tiết Playlist
                        val intent = Intent(requireContext(), PlaylistDetailActivity::class.java)
                        intent.putExtra("PLAYLIST_ID", playlist.id)
                        intent.putExtra("PLAYLIST_NAME", playlist.name)
                        intent.putExtra("PLAYLIST_COVER", playlist.cover)
                        startActivity(intent)

                        // THÊM DÒNG NÀY:
                        activity?.overridePendingTransition(R.anim.slide_in_up, android.R.anim.fade_out)
                    }
                },
                onPlaylistLongClick = { playlist ->
                    // CHẶN SỬA/XÓA: Không cho phép sửa hay xóa mục "Đã thích" của hệ thống
                    if (playlist.id != "LIKED_SONGS_SPECIAL_ID") {
                        showMoreOptionsBottomSheet(playlist)
                    }

                }
            )
            rvLibrary.adapter = adapter

        } else if (currentTab == "LIKED") {
            val adapter = LikedSongAdapter(
                songList = likedSongList,
                onSongClick = { song, position ->
                    MyMediaPlayer.currentPlaylist = ArrayList(likedSongList)
                    MyMediaPlayer.currentIndex = position

                    val intent = Intent(requireContext(), PlayerActivity::class.java)
                    startActivity(intent)

                    // THÊM DÒNG NÀY (Nhớ có chữ activity?. vì đây là Fragment)
                    activity?.overridePendingTransition(R.anim.slide_in_up, R.anim.scale_out_back)
                },
                onUnlikeClick = { song, position ->  // 👉 THÊM CHỮ ", position" VÀO ĐÂY NÈ
                    // Xóa bài hát khỏi nhánh LikedSongs trên Firebase
                    if (userId != null && song.id != null) {
                        dbRefLikedSongs.child(song.id!!).removeValue().addOnSuccessListener {
                            Toast.makeText(requireContext(), "Đã bỏ thích", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            rvLibrary.adapter = adapter
        }
    }
    private fun loadPlaylistsFromFirebase() {
        if (userId == null) return
        dbRefPlaylists.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playlistList.clear()
                for (playSnapshot in snapshot.children) {
                    val id = playSnapshot.key
                    val name = playSnapshot.child("name").value?.toString() ?: "Không tên"
                    // Nếu bạn lưu ảnh cover trên Firebase thì lấy ở đây
                    val coverUrl = playSnapshot.child("cover").value?.toString()
                    val songCount = playSnapshot.child("songCount").value.toString().toIntOrNull() ?: 0

                    playlistList.add(Playlist(id, name, coverUrl, songCount))
                }
                if(currentTab == "ALL" || currentTab == "PLAYLISTS") refreshRecyclerView()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadLikedSongsFromFirebase() {
        if (userId == null) return
        dbRefLikedSongs.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                likedSongList.clear()
                for (songSnapshot in snapshot.children) {
                    val song = songSnapshot.getValue(Song::class.java)
                    if (song != null) {
                        likedSongList.add(song)
                    }
                }
                if(currentTab == "LIKED") refreshRecyclerView()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- CÁC HÀM XỬ LÝ THÊM/SỬA/XÓA ---

    private fun showAddOrEditPlaylistDialog(playlistToEdit: Playlist? = null) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_playlist)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialog.findViewById<EditText>(R.id.etPlaylistName)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.btnSave)

        if (playlistToEdit != null) {
            tvTitle.text = "Đổi tên danh sách"

            etName.setText(playlistToEdit.name)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (playlistToEdit == null) {
                // Tạo mới
                val newId = dbRefPlaylists.push().key ?: return@setOnClickListener
                val newPlaylist = Playlist(id = newId, name = newName, songCount = 0)
                dbRefPlaylists.child(newId).setValue(newPlaylist).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Đã tạo Playlist!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Sửa tên
                dbRefPlaylists.child(playlistToEdit.id!!).child("name").setValue(newName).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Đã cập nhật!", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showMoreOptionsBottomSheet(playlist: Playlist) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_playlist_more_bottom_sheet, null)
        dialog.setContentView(view)

        // Xóa nền vuông
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }

        view.findViewById<LinearLayout>(R.id.btnEditPlaylist).setOnClickListener {
            dialog.dismiss()
            showAddOrEditPlaylistDialog(playlist)
        }

        view.findViewById<LinearLayout>(R.id.btnDeletePlaylist).setOnClickListener {
            dbRefPlaylists.child(playlist.id!!).removeValue().addOnSuccessListener {
                Toast.makeText(requireContext(), "Đã xóa danh sách phát", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}