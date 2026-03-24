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
import kotlin.random.Random

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

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnPlayPause = findViewById<CardView>(R.id.btnPlayPause)
        val btnNext = findViewById<ImageView>(R.id.btnNext)
        val btnPrev = findViewById<ImageView>(R.id.btnPrev)
        val btnUpNext = findViewById<ImageView>(R.id.btnUpNext)
        val cvLyricsContainer = findViewById<CardView>(R.id.cvLyricsContainer)
        val btnLyricsView = findViewById<ImageView>(R.id.btnLyricsView)

        // 2. Nhận dữ liệu bài hát từ màn hình trước
        val receivedSongs = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song>
        if (receivedSongs != null) {
            songList = receivedSongs
        }
        currentPosition = intent.getIntExtra("SONG_POSITION", 0)

        // 3. Khởi chạy bài hát
        loadSong()

        // 4. Bắt sự kiện Click các nút chức năng
        btnBack.setOnClickListener { finish() }

        btnPlayPause.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }
        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { prevSong() }

        // Mở Danh sách phát (Up Next)
        btnUpNext.setOnClickListener { showUpNextBottomSheet() }

        // Mở Lyrics đầy đủ (Bấm vào khối card hoặc nút phóng to đều được)
        cvLyricsContainer.setOnClickListener { showLyricsBottomSheet() }
        btnLyricsView.setOnClickListener { showLyricsBottomSheet() }

        // Bật/Tắt Trộn bài (Shuffle)
        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            // Đổi màu hồng nếu bật, xám nếu tắt
            btnShuffle.setColorFilter(if (isShuffle) Color.parseColor("#FF2D55") else Color.parseColor("#8E8E93"))
            Toast.makeText(this, if(isShuffle) "Đã bật trộn bài" else "Đã tắt trộn bài", Toast.LENGTH_SHORT).show()
        }

        // Thả tim (Yêu thích)
        btnLike.setOnClickListener { toggleLikeStatus() }

        // Xử lý kéo thanh Seekbar
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

        // Cập nhật giao diện
        tvTitleBig.text = song.title ?: "Unknown Title"
        tvArtistBig.text = song.artist ?: "Unknown Artist"
        song.cover?.let {
            Glide.with(this).load(it).placeholder(R.drawable.ic_launcher_background).into(ivCoverBig)
        }

        // Kiểm tra trạng thái Yêu thích
        checkLikeStatus(song)

        // Tải lời bài hát
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

        // Xử lý Audio
        if (MyMediaPlayer.currentIndex == currentPosition) {
            isPlaying = mediaPlayer.isPlaying
            ivPlayIcon.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else R.drawable.ic_play_modern)
            seekBar.max = mediaPlayer.duration
            updateSeekBar()
            return
        }

        MyMediaPlayer.currentPlaylist = songList
        MyMediaPlayer.currentIndex = currentPosition
        mediaPlayer.reset()

        song.audio?.let {
            mediaPlayer.setDataSource(it)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                seekBar.max = mp.duration
                tvTotalTime.text = createTimeLabel(mp.duration)
                playMusic()
                updateSeekBar()
            }

            // --- ĐOẠN CODE THÊM MỚI ---
            // Lắng nghe sự kiện khi MediaPlayer phát xong bài hiện tại
            mediaPlayer.setOnCompletionListener {
                nextSong() // Tự động gọi hàm chuyển bài
            }
            // --------------------------
        }
    }
    private fun playMusic() {
        mediaPlayer.start()
        isPlaying = true
        ivPlayIcon.setImageResource(android.R.drawable.ic_media_pause) // Dùng icon pause mặc định của Android hoặc icon riêng của bạn
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        isPlaying = false
        ivPlayIcon.setImageResource(R.drawable.ic_play_modern)
    }

    private fun nextSong() {
        if (isShuffle && songList.size > 1) {
            // Random nhưng tránh bốc trùng lại bài đang nghe
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("LikedSongs").child(songId)

        if (isLiked) {
            dbRef.removeValue().addOnSuccessListener {
                isLiked = false
                btnLike.setImageResource(R.drawable.ic_heart_outline) // Nhớ đảm bảo bạn có icon outline này
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
        }
    }

    private fun showUpNextBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_up_next_bottom_sheet, null)

        val rvUpNext = view.findViewById<RecyclerView>(R.id.rvUpNext)
        rvUpNext.layoutManager = LinearLayoutManager(this)
        rvUpNext.adapter = SongAdapter(songList)

        dialog.setContentView(view)

        // TẮT NỀN MẶC ĐỊNH ĐỂ HIỆN BO GÓC (Bắt buộc phải có)
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }
    private fun showLyricsBottomSheet() {
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

    // Các hàm phụ trợ thời gian và lyrics giữ nguyên
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

                // Hiển thị Lyrics chạy
                if (parsedLyrics.isNotEmpty()) {
                    var idx = -1
                    for (i in parsedLyrics.indices) {
                        if (currentMs >= parsedLyrics[i].first) idx = i else break
                    }
                    if (idx != -1) {
                        val curr = parsedLyrics[idx].second
                        // Tô hồng dòng lyric hiện tại
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
}