package com.example.hmusicv2

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import java.io.*

class PlayerActivity : AppCompatActivity() {

    private lateinit var tvSyncLyrics: TextView
    private val parsedLyrics = mutableListOf<Pair<Int, String>>()

    private var mediaPlayer: MediaPlayer = MyMediaPlayer.getMediaPlayer()
    private var isPlaying = false
    private var isShuffle = false
    private var isLiked = false

    private var runnable: Runnable? = null
    private var handler = Handler(Looper.getMainLooper())

    private var songList: ArrayList<Song> = ArrayList()
    private var currentPosition: Int = 0

    // Các thành phần UI
    private lateinit var tvTitleBig: TextView
    private lateinit var tvArtistBig: TextView
    private lateinit var ivCoverBig: ImageView
    private lateinit var ivPlayIcon: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnLike: ImageView
    private lateinit var btnShuffle: ImageView
    private lateinit var btnDownload: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // 1. Ánh xạ View
        tvSyncLyrics = findViewById(R.id.tvSyncLyrics)
        tvTitleBig = findViewById(R.id.tvTitleBig)
        tvArtistBig = findViewById(R.id.tvArtistBig)
        ivCoverBig = findViewById(R.id.ivCoverBig)
        ivPlayIcon = findViewById(R.id.ivPlayIcon)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnLike = findViewById(R.id.btnLike)
        btnShuffle = findViewById(R.id.btnShuffle)
        val btnAdd = findViewById<ImageView>(R.id.btnAdd)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnPlayPause = findViewById<CardView>(R.id.btnPlayPause)
        val btnNext = findViewById<ImageView>(R.id.btnNext)
        val btnPrev = findViewById<ImageView>(R.id.btnPrev)
        val btnUpNext = findViewById<ImageView>(R.id.btnUpNext)
        val cvLyricsContainer = findViewById<CardView>(R.id.cvLyricsContainer)
        val btnLyricsView = findViewById<ImageView>(R.id.btnLyricsView)
        btnDownload = findViewById(R.id.btnDownload)

        // 2. Nhận dữ liệu bài hát từ màn hình trước
        val receivedSongs = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song>
        if (receivedSongs != null) {
            songList = receivedSongs
        }
        currentPosition = intent.getIntExtra("SONG_POSITION", 0)

        // Bắt sự kiện mở bảng Thêm vào Playlist
        btnAdd.setOnClickListener { showAddToPlaylistBottomSheet() }

        // Logic bấm nút Download
        btnDownload.setOnClickListener {
            if (songList.isEmpty()) return@setOnClickListener
            val currentSong = songList[currentPosition]
            val dbHelper = DatabaseHelper(this)
            val localPath = dbHelper.getLocalPath(currentSong.id ?: "")

            if (localPath != null && java.io.File(localPath).exists()) {
                Toast.makeText(this, "Bài hát này đã có sẵn trong máy!", Toast.LENGTH_SHORT).show()
            } else {
                downloadAndSaveSong(currentSong)
            }
        }

        // 3. Khởi chạy bài hát
        loadSong()

        // 4. Bắt sự kiện Click các nút chức năng
        btnBack.setOnClickListener { finish() }
        btnPlayPause.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }
        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { prevSong() }
        btnUpNext.setOnClickListener { showUpNextBottomSheet() }
        cvLyricsContainer.setOnClickListener { showLyricsBottomSheet() }
        btnLyricsView.setOnClickListener { showLyricsBottomSheet() }

        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            btnShuffle.setColorFilter(if (isShuffle) Color.parseColor("#FF2D55") else Color.parseColor("#8E8E93"))
            Toast.makeText(this, if (isShuffle) "Đã bật trộn bài" else "Đã tắt trộn bài", Toast.LENGTH_SHORT).show()
        }

        btnLike.setOnClickListener { toggleLikeStatus() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    tvCurrentTime.text = createTimeLabel(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun loadSong() {
        if (songList.isEmpty()) return
        val song = songList[currentPosition]

        // 1. Cập nhật giao diện
        tvTitleBig.text = song.title ?: "Unknown Title"
        tvArtistBig.text = song.artist ?: "Unknown Artist"
        song.cover?.let {
            Glide.with(this).load(it).placeholder(R.drawable.ic_launcher_background).into(ivCoverBig)
        }

        checkLikeStatus(song)
        checkAndUpdateDownloadButton(song) // Kích hoạt đổi màu nút tải

        // 2. Tải lời bài hát từ Firebase
        tvSyncLyrics.text = "HMUSIC..."
        parsedLyrics.clear()
        val songId = song.id
        if (songId != null) {
            FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference.child("Songs").get().addOnSuccessListener { snapshot ->
                    for (songSnap in snapshot.children) {
                        if (songSnap.child("id").value?.toString() == songId) {
                            val freshLyrics = songSnap.child("lyrics").value?.toString()
                            song.lyrics = freshLyrics
                            if (!freshLyrics.isNullOrEmpty()) parseLyrics(freshLyrics)
                            break
                        }
                    }
                }
        }

        // 3. Xử lý Audio (Tránh phát lại nếu đang hát đúng bài này)
        if (MyMediaPlayer.currentIndex == currentPosition && mediaPlayer.isPlaying) {
            isPlaying = true
            ivPlayIcon.setImageResource(android.R.drawable.ic_media_pause)
            seekBar.max = mediaPlayer.duration
            updateSeekBar()
            return
        }

        // Cập nhật biến toàn cục
        MyMediaPlayer.currentPlaylist = songList
        MyMediaPlayer.currentIndex = currentPosition
        mediaPlayer.reset()

        // --- KIỂM TRA MẠNG VÀ OFFLINE ---
        val dbHelper = DatabaseHelper(this)
        val localPath = dbHelper.getLocalPath(song.id ?: "")
        val audioSource = if (localPath != null && java.io.File(localPath).exists()) {
            localPath // Hát Offline
        } else {
            song.audio // Hát Online
        }

        if (!audioSource.isNullOrEmpty()) {
            try {
                mediaPlayer.setDataSource(audioSource)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    seekBar.max = mp.duration
                    tvTotalTime.text = createTimeLabel(mp.duration)
                    playMusic()
                    updateSeekBar()
                }
                // Tự động chuyển bài khi hát xong
                mediaPlayer.setOnCompletionListener {
                    nextSong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Lỗi phát nhạc: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Không tìm thấy file nhạc!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndUpdateDownloadButton(song: Song) {
        val dbHelper = DatabaseHelper(this)
        val localPath = dbHelper.getLocalPath(song.id ?: "")

        if (localPath != null && java.io.File(localPath).exists()) {
            // Đã tải: Màu xanh lá
            btnDownload.setColorFilter(Color.parseColor("#1DB954"))
        } else {
            // Chưa tải: Màu trắng (hoặc màu gốc của app bạn)
            btnDownload.setColorFilter(Color.parseColor("#8E8E93"))
        }
    }

    private fun downloadAndSaveSong(song: Song) {
        if (song.audio.isNullOrEmpty() || !song.audio!!.startsWith("http")) {
            Toast.makeText(this, "Lỗi: Không thể tải bài này!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Đang tải bài hát: ${song.title}...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = java.net.URL(song.audio)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()

                val fileName = "${song.id}.mp3"
                val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
                val outputStream = FileOutputStream(file)

                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()

                val dbHelper = DatabaseHelper(this@PlayerActivity)
                val isSaved = dbHelper.insertOfflineSong(song, file.absolutePath)

                withContext(Dispatchers.Main) {
                    if (isSaved) {
                        Toast.makeText(this@PlayerActivity, "Đã tải xong và lưu vào máy!", Toast.LENGTH_SHORT).show()
                        btnDownload.setColorFilter(Color.parseColor("#1DB954")) // Chuyển xanh lập tức
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, "Lỗi tải nhạc: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playMusic() {
        mediaPlayer.start()
        isPlaying = true
        ivPlayIcon.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        isPlaying = false
        ivPlayIcon.setImageResource(R.drawable.ic_play_modern)
    }

    private fun nextSong() {
        if (isShuffle && songList.size > 1) {
            var newPosition = currentPosition
            while (newPosition == currentPosition) {
                newPosition = kotlin.random.Random.nextInt(songList.size)
            }
            currentPosition = newPosition
        } else {
            currentPosition = if (currentPosition < songList.size - 1) currentPosition + 1 else 0
        }
        loadSong()
    }

    private fun prevSong() {
        currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
        loadSong()
    }

    private fun toggleLikeStatus() {
        if (songList.isEmpty()) return
        val song = songList[currentPosition]
        val songId = song.id ?: return

        // Bắt lỗi rành mạch ở đây
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để thêm bài hát này vào mục yêu thích", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("LikedSongs").child(songId)

        if (isLiked) {
            dbRef.removeValue().addOnSuccessListener {
                isLiked = false
                btnLike.setImageResource(R.drawable.ic_heart_outline)
            }
        } else {
            dbRef.setValue(song).addOnSuccessListener {
                isLiked = true
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                Snackbar.make(findViewById(android.R.id.content), "Đã thêm vào yêu thích", Snackbar.LENGTH_SHORT)
                    .setActionTextColor(Color.parseColor("#FF2D55")).show()
            }
        }
    }

    private fun checkLikeStatus(song: Song) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val songId = song.id
        if (songId != null && userId != null) {
            val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference.child("Users").child(userId).child("LikedSongs").child(songId)

            dbRef.get().addOnSuccessListener { snapshot ->
                isLiked = snapshot.exists()
                btnLike.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            }
        } else {
            // Nếu chưa đăng nhập thì mặc định trái tim rỗng
            isLiked = false
            btnLike.setImageResource(R.drawable.ic_heart_outline)
        }
    }

    private fun showUpNextBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_up_next_bottom_sheet, null)
        val rvUpNext = view.findViewById<RecyclerView>(R.id.rvUpNext)
        rvUpNext.layoutManager = LinearLayoutManager(this)
        rvUpNext.adapter = SongAdapter(songList)
        dialog.setContentView(view)
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun showLyricsBottomSheet() {
        if (songList.isEmpty()) return
        val song = songList[currentPosition]
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_lyrics_bottom_sheet, null)
        val tvLyrics = view.findViewById<TextView>(R.id.tvLyricsContent)
        tvLyrics.text = song.lyrics?.replace("\\n", "\n") ?: "Chưa có lời bài hát"
        dialog.setContentView(view)
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun showAddToPlaylistBottomSheet() {
        if (songList.isEmpty()) return
        val currentSong = songList[currentPosition]
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_add_to_playlist_bottom_sheet, null)
        dialog.setContentView(view)

        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }

        val btnCreateNew = view.findViewById<CardView>(R.id.btnCreateNewPlaylist)
        val rvPlaylists = view.findViewById<RecyclerView>(R.id.rvPlaylistsBottomSheet)

        btnCreateNew.setOnClickListener {
            dialog.dismiss()
            showCreatePlaylistDialog(currentSong)
        }

        // Bắt lỗi người dùng chưa đăng nhập
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để dùng chức năng Playlist!", Toast.LENGTH_SHORT).show()
            dialog.show() // Vẫn cho hiện cái bảng lên để khỏi tưởng nút bị liệt
            return
        }

        rvPlaylists.layoutManager = LinearLayoutManager(this)
        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users").child(userId).child("Playlists")

        val playlists = mutableListOf<Playlist>()
        val adapter = LibraryPlaylistAdapter(
            playlistList = playlists,
            onPlaylistClick = { playlist ->
                if (playlist.id != null) {
                    val songId = currentSong.id ?: System.currentTimeMillis().toString()
                    dbRef.child(playlist.id!!).child("songs").child(songId).setValue(currentSong)
                        .addOnSuccessListener {
                            val updates = mapOf(
                                "songCount" to playlist.songCount + 1,
                                "cover" to currentSong.cover
                            )
                            dbRef.child(playlist.id!!).updateChildren(updates)
                            Toast.makeText(this, "Đã lưu vào ${playlist.name}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                }
            },
            onPlaylistLongClick = { }
        )
        rvPlaylists.adapter = adapter

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playlists.clear()
                for (playSnapshot in snapshot.children) {
                    val id = playSnapshot.key
                    val name = playSnapshot.child("name").value?.toString() ?: "Không tên"
                    val coverUrl = playSnapshot.child("cover").value?.toString()
                    val songCount = playSnapshot.child("songCount").value.toString().toIntOrNull() ?: 0
                    playlists.add(Playlist(id, name, coverUrl, songCount))
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Đem hàm show xuống cuối cùng để chắc chắn nó luôn được gọi
        dialog.show()
    }
    private fun showCreatePlaylistDialog(songToAdd: Song) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_playlist)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        val etName = dialog.findViewById<android.widget.EditText>(R.id.etPlaylistName)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialog.findViewById<CardView>(R.id.btnSave)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users").child(userId).child("Playlists")

            val newId = dbRef.push().key ?: return@setOnClickListener
            val newPlaylist = Playlist(id = newId, name = newName, songCount = 1, cover = songToAdd.cover)

            dbRef.child(newId).setValue(newPlaylist).addOnSuccessListener {
                val songId = songToAdd.id ?: System.currentTimeMillis().toString()
                dbRef.child(newId).child("songs").child(songId).setValue(songToAdd)
                Toast.makeText(this, "Đã tạo và thêm vào Playlist!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun createTimeLabel(time: Int): String {
        val min = time / 1000 / 60
        val sec = time / 1000 % 60
        return String.format("%02d:%02d", min, sec)
    }

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)
        runnable = Runnable {
            if (mediaPlayer.isPlaying) {
                val currentMs = mediaPlayer.currentPosition
                seekBar.progress = currentMs
                tvCurrentTime.text = createTimeLabel(currentMs)

                if (parsedLyrics.isNotEmpty()) {
                    var idx = -1
                    for (i in parsedLyrics.indices) {
                        if (currentMs >= parsedLyrics[i].first) idx = i else break
                    }
                    if (idx != -1) {
                        val curr = parsedLyrics[idx].second
                        tvSyncLyrics.text = Html.fromHtml("<font color='#FF2D55'><b>$curr</b></font>", Html.FROM_HTML_MODE_LEGACY)
                    }
                }
            }
            handler.postDelayed(runnable!!, 500)
        }
        handler.postDelayed(runnable!!, 500)
    }

    private fun parseLyrics(lrc: String) {
        parsedLyrics.clear()
        val lines = lrc.replace("\\n", "\n").split("\n")
        val regex = Regex("\\[(\\d{2}):(\\d{2}).*?\\](.*)")
        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val timeMs = (match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()) * 1000
                parsedLyrics.add(Pair(timeMs, match.groupValues[3].trim()))
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.scale_in_front, R.anim.slide_out_down)
    }
}