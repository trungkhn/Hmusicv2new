package com.example.hmusicv2

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OfflineMusicActivity : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var rvOfflineSongs: RecyclerView
    private lateinit var etSearchOffline: EditText
    private lateinit var tvCount: TextView
    private lateinit var dbHelper: DatabaseHelper

    // Dùng 2 danh sách: 1 gốc, 1 để hiển thị khi tìm kiếm
    private var fullOfflineList = ArrayList<Song>()
    private var displayList = ArrayList<Song>()
    private lateinit var adapter: LikedSongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_music)

        // --- RADAR MẠNG ---
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                super.onAvailable(network)
                runOnUiThread {
                    Toast.makeText(this@OfflineMusicActivity, "Đã khôi phục mạng! Đang về trang chủ...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@OfflineMusicActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
        var request = android.net.NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // --- ÁNH XẠ VIEW ---
        var btnBack = findViewById<ImageView>(R.id.btnBackOffline)
        var btnPlayAll = findViewById<CardView>(R.id.btnPlayAllOffline)
        tvCount = findViewById<TextView>(R.id.tvOfflineCount)
        rvOfflineSongs = findViewById(R.id.rvOfflineSongs)
        etSearchOffline = findViewById(R.id.etSearchOffline)

        // --- LẤY DỮ LIỆU TỪ SQLITE ---
        dbHelper = DatabaseHelper(this)
        fullOfflineList = dbHelper.getAllOfflineSongs()
        displayList.addAll(fullOfflineList) // Ban đầu chưa tìm kiếm thì hiển thị tất cả

        tvCount.text = "${fullOfflineList.size} BÀI HÁT • KHẢ DỤNG"
        if (fullOfflineList.isEmpty()) {
            Toast.makeText(this, "Bạn chưa tải bài hát nào!", Toast.LENGTH_SHORT).show()
        }

        // --- CÀI ĐẶT RECYCLERVIEW ---
        rvOfflineSongs.layoutManager = LinearLayoutManager(this)
        adapter = LikedSongAdapter(
            songList = displayList,
            onSongClick = { _, position ->
                playOfflineMusic(position)
            },
            onUnlikeClick = { _, _ ->  // 👉 CHỈ CẦN THÊM "_, _ ->" VÀO ĐÂY LÀ HẾT LỖI
                Toast.makeText(this, "Bài này đã nằm an toàn trong máy!", Toast.LENGTH_SHORT).show()
            },
            // GẮN SỰ KIỆN BẤM GIỮ Ở ĐÂY
            onSongLongClick = { song, position ->
                showOptionsMenu(song, position)
            }
        )
        rvOfflineSongs.adapter = adapter

        // --- TÍNH NĂNG TÌM KIẾM ---
        etSearchOffline.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSongs(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- SỰ KIỆN NÚT BẤM ---
        btnPlayAll.setOnClickListener {
            if (displayList.isNotEmpty()) {
                playOfflineMusic(0)
            } else {
                Toast.makeText(this, "Chưa có bài nào để phát!", Toast.LENGTH_SHORT).show()
            }
        }
        btnBack.setOnClickListener { finish() }
    }

    // --- HÀM TÌM KIẾM ---
    private fun filterSongs(query: String) {
        displayList.clear()
        if (query.isEmpty()) {
            displayList.addAll(fullOfflineList)
        } else {
            var lowerCaseQuery = query.lowercase()
            for (song in fullOfflineList) {
                if (song.title?.lowercase()?.contains(lowerCaseQuery) == true ||
                    song.artist?.lowercase()?.contains(lowerCaseQuery) == true) {
                    displayList.add(song)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    // --- HỘP THOẠI KHI BẤM GIỮ (ĐỔI TÊN / XÓA) ---
    // --- HỘP THOẠI KHI BẤM GIỮ (BOTTOM SHEET XỊN XÒ) ---
    private fun showOptionsMenu(song: Song, position: Int) {
        // Gọi cái layout_offline_song_options của má ra nè:
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_offline_song_options, null)
        dialog.setContentView(view)

        // Phép thuật làm trong suốt cái nền góc vuông, để lộ ra cái góc bo tròn 32dp của má
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = d.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Ánh xạ 2 cái nút bấm trong file XML mới
        val btnEdit = view.findViewById<android.widget.LinearLayout>(R.id.btnEditSong)
        val btnDelete = view.findViewById<android.widget.LinearLayout>(R.id.btnDeleteSong)

        // Xử lý sự kiện bấm nút Đổi Tên
        btnEdit.setOnClickListener {
            dialog.dismiss()
            showRenameDialog(song, position)
        }

        // Xử lý sự kiện bấm nút Xóa
        btnDelete.setOnClickListener {
            dialog.dismiss()
            confirmDeleteSong(song, position)
        }

        dialog.show() // Lệnh quan trọng nhất: Hiện cái bảng lên!
    }
    // --- HÀM ĐỔI TÊN ---
    private fun showRenameDialog(song: Song, position: Int) {
        var editText = EditText(this)
        editText.setText(song.title)
        editText.setPadding(50, 40, 50, 40)

        android.app.AlertDialog.Builder(this)
            .setTitle("Đổi tên bài hát")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                var newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    // Cập nhật Database
                    dbHelper.updateOfflineSongTitle(song.id ?: "", newTitle)

                    // Cập nhật danh sách hiển thị
                    song.title = newTitle

                    // Cập nhật danh sách gốc để khi tìm kiếm không bị mất tên mới
                    var indexInFullList = fullOfflineList.indexOfFirst { it.id == song.id }
                    if (indexInFullList != -1) fullOfflineList[indexInFullList].title = newTitle

                    adapter.notifyItemChanged(position)
                    Toast.makeText(this, "Đã đổi tên thành công!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // --- HÀM XÓA BÀI HÁT ---
    private fun confirmDeleteSong(song: Song, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc chắn muốn xóa bài '${song.title}' khỏi bộ nhớ máy?")
            .setPositiveButton("Xóa") { _, _ ->
                // 1. Xóa Database
                dbHelper.deleteOfflineSong(song.id ?: "")
                // 2. Xóa file vật lý
                var file = java.io.File(song.audio ?: "")
                if (file.exists()) file.delete()

                // 3. Xóa khỏi danh sách
                displayList.removeAt(position)
                var indexInFullList = fullOfflineList.indexOfFirst { it.id == song.id }
                if (indexInFullList != -1) fullOfflineList.removeAt(indexInFullList)

                adapter.notifyItemRemoved(position)
                tvCount.text = "${fullOfflineList.size} BÀI HÁT • NGOẠI TUYẾN"
                Toast.makeText(this, "Đã xóa bài hát khỏi máy!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // --- PHÁT NHẠC (Sử dụng danh sách đang hiển thị) ---
    private fun playOfflineMusic(position: Int) {
        MyMediaPlayer.currentPlaylist = displayList // Lấy danh sách đã lọc (nếu đang tìm kiếm)
        MyMediaPlayer.currentIndex = position

        var intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("SONG_LIST", displayList as java.io.Serializable)
        intent.putExtra("SONG_POSITION", position)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_up, R.anim.scale_out_back)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
    }
}