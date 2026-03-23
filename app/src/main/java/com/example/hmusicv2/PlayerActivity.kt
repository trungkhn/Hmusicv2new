package com.example.hmusicv2

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PlayerActivity : AppCompatActivity() {

    private lateinit var tvSyncLyrics: TextView
    private val parsedLyrics = mutableListOf<Pair<Int, String>>()

    private var mediaPlayer: MediaPlayer = MyMediaPlayer.getMediaPlayer()
    private var isPlaying = false
    private var isShuffle = false

    private var runnable: Runnable? = null
    private var handler = Handler(Looper.getMainLooper())

    private var songList: ArrayList<Song> = ArrayList()
    private var currentPosition: Int = 0

    private lateinit var tvTitleBig: TextView
    private lateinit var tvArtistBig: TextView
    private lateinit var ivCoverBig: ImageView
    private lateinit var ivPlayIcon: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnLike: ImageView
    private lateinit var btnShuffle: ImageView
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // 1. Ánh xạ View
        tvSyncLyrics = findViewById(R.id.tvSyncLyrics)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        tvTitleBig = findViewById(R.id.tvTitleBig)
        tvArtistBig = findViewById(R.id.tvArtistBig)
        ivCoverBig = findViewById(R.id.ivCoverBig)
        ivPlayIcon = findViewById(R.id.ivPlayIcon)
        val btnPlayPause = findViewById<CardView>(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnLike = findViewById(R.id.btnLike)
        btnShuffle = findViewById(R.id.btnShuffle)

        val btnNext = findViewById<ImageView>(R.id.btnNext)
        val btnPrev = findViewById<ImageView>(R.id.btnPrev)
        val btnLyrics = findViewById<ImageView>(R.id.btnLyricsView)

        // 2. Click Listeners
        btnBack.setOnClickListener { finish() }
        btnLyrics.setOnClickListener { showLyricsBottomSheet() }

        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            btnShuffle.setColorFilter(if (isShuffle) Color.parseColor("#FF2D55") else Color.parseColor("#8e8e93"))
            Toast.makeText(this, if(isShuffle) "Bật xáo trộn" else "Tắt xáo trộn", Toast.LENGTH_SHORT).show()
        }

        // 3. Nhận dữ liệu bài hát (Sửa lỗi ép kiểu)
        val receivedSongs = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song>
        if (receivedSongs != null) {
            songList = receivedSongs
        }
        currentPosition = intent.getIntExtra("SONG_POSITION", 0)

        // 4. Xử lý Trái tim
        btnLike.setOnClickListener {
            if (songList.isEmpty()) return@setOnClickListener
            val song = songList[currentPosition]
            val songId = song.id ?: return@setOnClickListener
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

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

        loadSong()

        btnPlayPause.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }
        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { prevSong() }

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

        tvTitleBig.text = song.title ?: "Unknown Title"
        tvArtistBig.text = song.artist ?: "Unknown Artist"

        // Load ảnh bìa
        song.cover?.let {
            Glide.with(this).load(it).placeholder(R.drawable.ic_launcher_background).into(ivCoverBig)
        }

        // FIX LỖI NGÔI SAO: Kiểm tra Like ngay khi load bài
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val songId = song.id
        if (songId != null && userId != null) {
            val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference.child("Users").child(userId).child("LikedSongs").child(songId)

            dbRef.get().addOnSuccessListener { snapshot ->
                isLiked = snapshot.exists()
                // Gán đúng icon trái tim, KHÔNG gán android ngôi sao
                btnLike.setImageResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            }
        }

        // Tải lời bài hát
        tvSyncLyrics.text = "♪\nĐang tải...\n♪"
        parsedLyrics.clear()
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

        // Đồng bộ trạng thái chơi nhạc
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
        currentPosition = if (currentPosition < songList.size - 1) currentPosition + 1 else 0
        loadSong()
    }

    private fun prevSong() {
        currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
        loadSong()
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

                // Hiển thị Lyrics chạy
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

    private fun showLyricsBottomSheet() {
        val song = songList[currentPosition]
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_lyrics_bottom_sheet, null)
        val tvLyrics = view.findViewById<TextView>(R.id.tvLyricsContent)
        tvLyrics.text = song.lyrics?.replace("\\n", "\n") ?: "Chưa có lời bài hát"
        dialog.setContentView(view)
        dialog.show()
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

    private fun showPlaylistBottomSheet() {
        // Hàm này bạn giữ nguyên logic cũ của mình nhé
    }

    private fun createNewPlaylistAndAddSong(name: String) { }
    private fun showCreatePlaylistDialog() { }
}