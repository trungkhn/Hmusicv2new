package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var rlMiniPlayer: RelativeLayout
    private lateinit var tvMiniSongTitle: TextView
    private lateinit var tvMiniArtist: TextView
    private lateinit var btnMiniPause: ImageView
    private lateinit var btnMiniNext: ImageView
    private lateinit var imgMiniAlbum: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        rlMiniPlayer = findViewById(R.id.rlMiniPlayer)
        tvMiniSongTitle = findViewById(R.id.tvMiniSongTitle)
        tvMiniArtist = findViewById(R.id.tvMiniArtist)
        btnMiniPause = findViewById(R.id.btnMiniPause)
        btnMiniNext = findViewById(R.id.btnMiniNext)
        imgMiniAlbum = findViewById(R.id.imgMiniAlbum)

        btnMiniPause.setOnClickListener {
            val mediaPlayer = MyMediaPlayer.getMediaPlayer()
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                btnMiniPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer.start()
                btnMiniPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnMiniNext.setOnClickListener {
            playNextSong()
        }

        rlMiniPlayer.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            // Cần ép kiểu as java.io.Serializable để tránh lỗi truyền dữ liệu
            intent.putExtra("SONG_LIST", MyMediaPlayer.currentPlaylist as java.io.Serializable)
            intent.putExtra("SONG_POSITION", MyMediaPlayer.currentIndex)
            startActivity(intent)
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_library -> LibraryFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayer()
    }

    private fun updateMiniPlayer() {
        if (MyMediaPlayer.currentIndex != -1 && MyMediaPlayer.currentPlaylist.isNotEmpty()) {
            rlMiniPlayer.visibility = View.VISIBLE
            val currentSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]

            if (!currentSong.cover.isNullOrEmpty()) {
                Glide.with(this)
                    .load(currentSong.cover)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(imgMiniAlbum)
            } else {
                imgMiniAlbum.setImageResource(R.drawable.ic_default_avatar)
            }

            tvMiniSongTitle.text = currentSong.title
            tvMiniArtist.text = currentSong.artist

            val mediaPlayer = MyMediaPlayer.getMediaPlayer()
            if (mediaPlayer.isPlaying) {
                btnMiniPause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                btnMiniPause.setImageResource(android.R.drawable.ic_media_play)
            }
        } else {
            rlMiniPlayer.visibility = View.GONE
        }
    }

    private fun playNextSong() {
        if (MyMediaPlayer.currentPlaylist.isEmpty()) return

        if (MyMediaPlayer.currentIndex < MyMediaPlayer.currentPlaylist.size - 1) {
            MyMediaPlayer.currentIndex++
        } else {
            MyMediaPlayer.currentIndex = 0
        }

        val nextSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        val mediaPlayer = MyMediaPlayer.getMediaPlayer()

        mediaPlayer.reset()

        val audioUrl = nextSong.audio
        if (audioUrl != null && audioUrl.startsWith("http")) {
            mediaPlayer.setDataSource(audioUrl)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                it.start()
                updateMiniPlayer()
            }
        }
        updateMiniPlayer()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }
}