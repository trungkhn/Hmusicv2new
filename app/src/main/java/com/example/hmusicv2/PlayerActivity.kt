package com.example.hmusicv2

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PlayerActivity : AppCompatActivity() {

    private lateinit var tvSyncLyrics: TextView
    private val parsedLyrics = mutableListOf<Pair<Int, String>>()

    private var mediaPlayer: MediaPlayer = MyMediaPlayer.getMediaPlayer()
    private var isPlaying = false

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
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        tvSyncLyrics = findViewById(R.id.tvSyncLyrics)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        tvTitleBig = findViewById<TextView>(R.id.tvTitleBig)
        tvArtistBig = findViewById<TextView>(R.id.tvArtistBig)
        ivCoverBig = findViewById<ImageView>(R.id.ivCoverBig)
        ivPlayIcon = findViewById<ImageView>(R.id.ivPlayIcon)
        val btnPlayPause = findViewById<CardView>(R.id.btnPlayPause)
        seekBar = findViewById<SeekBar>(R.id.seekBar)
        tvCurrentTime = findViewById<TextView>(R.id.tvCurrentTime)
        tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)
        btnLike = findViewById<ImageView>(R.id.btnLike)

        val btnNext = findViewById<ImageView>(R.id.btnNext)
        val btnPrev = findViewById<ImageView>(R.id.btnPrev)
        val btnLyrics = findViewById<ImageView>(R.id.btnLyricsView)

        btnLyrics.setOnClickListener { showLyricsBottomSheet() }
        btnBack.setOnClickListener { finish() }

        @Suppress("UNCHECKED_CAST")
        songList = intent.getSerializableExtra("SONG_LIST") as ArrayList<Song>
        currentPosition = intent.getIntExtra("SONG_POSITION", 0)

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
                    btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                }
            } else {
                dbRef.setValue(song).addOnSuccessListener {
                    isLiked = true
                    btnLike.setImageResource(android.R.drawable.btn_star_big_on)

                    val snackbar = com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Đã thêm vào mục bài hát yêu thích",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction("THAY ĐỔI") { showPlaylistBottomSheet() }
                    snackbar.setActionTextColor(android.graphics.Color.parseColor("#1DB954"))
                    snackbar.show()
                }
            }
        }

        loadSong()

        btnPlayPause.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        btnNext.setOnClickListener {
            if (currentPosition < songList.size - 1) currentPosition++ else currentPosition = 0
            loadSong()
        }

        btnPrev.setOnClickListener {
            if (currentPosition > 0) currentPosition-- else currentPosition = songList.size - 1
            loadSong()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    tvCurrentTime.text = createTimeLabel(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadSong() {
        if (songList.isEmpty()) return
        val song = songList[currentPosition]

        saveSongToHistory(song)

        tvTitleBig.text = song.title
        tvArtistBig.text = song.artist

        if (song.cover != null) {
            Glide.with(this).load(song.cover).into(ivCoverBig)
        }

        // --- ĐOẠN MA THUẬT: QUÉT TOÀN BỘ KHO ĐỂ TÌM ĐÚNG LỜI BÀI HÁT ---
        tvSyncLyrics.text = "♪\nĐang tải lời bài hát...\n♪"
        parsedLyrics.clear()

        val songId = song.id
        if (songId != null) {
            val dbSongsRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference.child("Songs")

            dbSongsRef.get().addOnSuccessListener { snapshot ->
                var foundLyrics = false
                // Lục tung từng thư mục để tìm ID khớp nhau
                for (songSnap in snapshot.children) {
                    if (songSnap.child("id").value?.toString() == songId) {
                        val freshLyrics = songSnap.child("lyrics").value?.toString()

                        // Cất lời vào biến song để xíu nữa mở Bảng trượt lên xài ké
                        song.lyrics = freshLyrics

                        if (!freshLyrics.isNullOrEmpty() && freshLyrics.contains("[")) {
                            parseLyrics(freshLyrics)
                            foundLyrics = true
                        }
                        break
                    }
                }

                if (!foundLyrics) {
                    parsedLyrics.clear()
                    tvSyncLyrics.text = "♪\nChưa có lời bài hát\n♪"
                }
            }.addOnFailureListener {
                tvSyncLyrics.text = "♪\nLỗi mạng, không tải được lời\n♪"
            }
        }
        // ----------------------------------------------------

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (songId != null && userId != null) {
            val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference.child("Users").child(userId).child("LikedSongs").child(songId)

            dbRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    isLiked = true
                    btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                } else {
                    isLiked = false
                    btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                }
            }
        }

        if (MyMediaPlayer.currentIndex == currentPosition) {
            try {
                isPlaying = mediaPlayer.isPlaying
                ivPlayIcon.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)

                seekBar.max = mediaPlayer.duration
                tvTotalTime.text = createTimeLabel(mediaPlayer.duration)
                seekBar.progress = mediaPlayer.currentPosition
                tvCurrentTime.text = createTimeLabel(mediaPlayer.currentPosition)

                updateSeekBar()
                return
            } catch (e: Exception) {
            }
        }

        MyMediaPlayer.currentPlaylist = songList
        MyMediaPlayer.currentIndex = currentPosition
        mediaPlayer.reset()

        seekBar.progress = 0
        tvCurrentTime.text = "00:00"
        tvTotalTime.text = "00:00"

        val audioUrl = song.audio
        if (audioUrl != null && audioUrl.startsWith("http")) {
            mediaPlayer.setDataSource(audioUrl)
            mediaPlayer.prepareAsync()

            mediaPlayer.setOnPreparedListener {
                seekBar.max = it.duration
                tvTotalTime.text = createTimeLabel(it.duration)
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
        ivPlayIcon.setImageResource(android.R.drawable.ic_media_play)
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
                    var currentIndex = -1
                    for (i in parsedLyrics.indices) {
                        if (currentMs >= parsedLyrics[i].first) {
                            currentIndex = i
                        } else break
                    }

                    if (currentIndex != -1) {
                        val prev = if (currentIndex > 0) parsedLyrics[currentIndex - 1].second else ""
                        val curr = parsedLyrics[currentIndex].second
                        val next = if (currentIndex < parsedLyrics.size - 1) parsedLyrics[currentIndex + 1].second else ""

                        val htmlText = "<font color='#666666'>$prev</font><br>" +
                                "<font color='#1DB954'><b>$curr</b></font><br>" +
                                "<font color='#666666'>$next</font>"

                        tvSyncLyrics.text = android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
                    }
                }
            }
            runnable?.let { handler.postDelayed(it, 300) }
        }
        runnable?.let { handler.postDelayed(it, 300) }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun showPlaylistBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)

        val btnCreateNewPlaylist = view.findViewById<android.widget.LinearLayout>(R.id.btnCreateNewPlaylist)
        val rvPlaylists = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPlaylistsBottomSheet)

        rvPlaylists.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        btnCreateNewPlaylist.setOnClickListener {
            bottomSheetDialog.dismiss()
            showCreatePlaylistDialog()
        }

        if (songList.isEmpty()) return
        val currentSong = songList[currentPosition]
        val songId = currentSong.id ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("Playlists")

        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val playlists = mutableListOf<PlaylistState>()

                for (playlistSnapshot in snapshot.children) {
                    val playlistName = playlistSnapshot.key ?: continue
                    val hasSong = playlistSnapshot.child("songs").hasChild(songId)
                    playlists.add(PlaylistState(playlistName, hasSong))
                }

                val adapter = PlaylistAdapter(playlists) { clickedPlaylist ->
                    val playlistSongRef = dbRef.child(clickedPlaylist.name).child("songs").child(songId)

                    if (clickedPlaylist.isAdded) {
                        playlistSongRef.removeValue().addOnSuccessListener {
                            android.widget.Toast.makeText(this@PlayerActivity, "Đã xóa khỏi ${clickedPlaylist.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        playlistSongRef.setValue(currentSong).addOnSuccessListener {
                            android.widget.Toast.makeText(this@PlayerActivity, "Đã thêm vào ${clickedPlaylist.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                rvPlaylists.adapter = adapter
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showCreatePlaylistDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_create_playlist, null)
        val etPlaylistName = view.findViewById<android.widget.EditText>(R.id.etPlaylistName)
        val btnCancel = view.findViewById<android.widget.TextView>(R.id.btnCancelDialog)
        val btnCreate = view.findViewById<android.widget.TextView>(R.id.btnCreateDialog)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(view)
            .create()

        btnCreate.setOnClickListener {
            val playlistName = etPlaylistName.text.toString().trim()
            if (playlistName.isNotEmpty()) {
                createNewPlaylistAndAddSong(playlistName)
                dialog.dismiss()
            } else {
                android.widget.Toast.makeText(this, "Vui lòng nhập tên danh sách phát", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        etPlaylistName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnCreate.performClick()
                return@setOnEditorActionListener true
            }
            false
        }
        dialog.show()
    }

    private fun createNewPlaylistAndAddSong(playlistName: String) {
        if (songList.isEmpty()) return
        val currentSong = songList[currentPosition]
        val songId = currentSong.id ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("Playlists").child(playlistName)

        dbRef.child("info").child("name").setValue(playlistName)
        dbRef.child("songs").child(songId).setValue(currentSong).addOnSuccessListener {
            android.widget.Toast.makeText(this, "Đã tạo và thêm vào $playlistName", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSongToHistory(song: Song) {
        val songId = song.id ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("History").child(songId)
        dbRef.setValue(song)
    }

    private fun showLyricsBottomSheet() {
        if (songList.isEmpty()) return
        val currentSong = songList[currentPosition]

        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_lyrics_bottom_sheet, null)
        val tvLyricsContent = view.findViewById<android.widget.TextView>(R.id.tvLyricsContent)

        // Không cần gọi lên Firebase nữa vì ở hàm loadSong đã lấy sẵn rồi!
        if (!currentSong.lyrics.isNullOrEmpty()) {
            tvLyricsContent.text = currentSong.lyrics!!.replace("\\n", "\n")
        } else {
            tvLyricsContent.text = "Chưa có lời bài hát cho bài này..."
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.show()
    }

    private fun parseLyrics(lrc: String) {
        parsedLyrics.clear()
        val regex = Regex("\\[(\\d{2}):(\\d{2}).*?\\](.*)")
        val lines = lrc.replace("\\n", "\n").split("\n")

        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toInt()
                val sec = match.groupValues[2].toInt()
                val text = match.groupValues[3].trim()

                val timeMs = (min * 60 + sec) * 1000
                parsedLyrics.add(Pair(timeMs, text))
            }
        }

        if (parsedLyrics.isEmpty()) {
            tvSyncLyrics.text = "♪\nLỗi định dạng lời bài hát\n♪"
        }
    }
}