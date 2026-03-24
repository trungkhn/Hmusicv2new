package com.example.hmusicv2

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Khai báo biến quét mạng
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private lateinit var rlMiniPlayer: RelativeLayout
    private lateinit var tvMiniSongTitle: TextView
    private lateinit var tvMiniArtist: TextView
    private lateinit var btnMiniPause: ImageView
    private lateinit var btnMiniNext: ImageView
    private lateinit var imgMiniAlbum: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ================================================================
        // GHI CHÚ BƯỚC 1: KIỂM TRA MẠNG NGAY LÚC VỪA MỞ APP (Sửa lỗi của má nè)
        // Nếu mở app mà không có mạng, đá thẳng sang Offline ngay lập tức!
        // ================================================================
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Bạn đang ngoại tuyến! Chuyển sang kho nhạc...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, OfflineMusicActivity::class.java))
            finish()
            return // GHI CHÚ: return ở đây để app không thèm chạy các dòng code bên dưới nữa
        }

        // ================================================================
        // GHI CHÚ BƯỚC 2: CÓ MẠNG THÌ MỚI ĐƯỢC LOAD GIAO DIỆN CHÍNH
        // ================================================================
        setContentView(R.layout.activity_main)

        // ================================================================
        // GHI CHÚ BƯỚC 3: GẮN RADAR NGẦM THEO DÕI NẾU ĐANG DÙNG MÀ BỊ RỚT MẠNG
        // ================================================================
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Mất kết nối! Đang chuyển sang kho Offline...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@MainActivity, OfflineMusicActivity::class.java))
                    finish()
                }
            }
        }
        val request = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // --- ÁNH XẠ VIEW GIAO DIỆN ---
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

        btnMiniNext.setOnClickListener { playNextSong() }

        // --- Mở PlayerActivity với animation slide up ---
        rlMiniPlayer.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("SONG_LIST", MyMediaPlayer.currentPlaylist as java.io.Serializable)
            intent.putExtra("SONG_POSITION", MyMediaPlayer.currentIndex)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.scale_out_back)
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

            Glide.with(this)
                .load(currentSong.cover)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(imgMiniAlbum)

            tvMiniSongTitle.text = currentSong.title
            tvMiniArtist.text = currentSong.artist

            val mediaPlayer = MyMediaPlayer.getMediaPlayer()
            btnMiniPause.setImageResource(
                if (mediaPlayer.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        } else {
            rlMiniPlayer.visibility = View.GONE
        }
    }

    private fun playNextSong() {
        if (MyMediaPlayer.currentPlaylist.isEmpty()) return

        MyMediaPlayer.currentIndex =
            if (MyMediaPlayer.currentIndex < MyMediaPlayer.currentPlaylist.size - 1) MyMediaPlayer.currentIndex + 1
            else 0

        val nextSong = MyMediaPlayer.currentPlaylist[MyMediaPlayer.currentIndex]
        val mediaPlayer = MyMediaPlayer.getMediaPlayer()
        mediaPlayer.reset()

        val dbHelper = DatabaseHelper(this)
        val localPath = dbHelper.getLocalPath(nextSong.id ?: "")
        val audioSource = if (localPath != null && java.io.File(localPath).exists()) {
            localPath
        } else {
            nextSong.audio
        }

        if (!audioSource.isNullOrEmpty()) {
            mediaPlayer.setDataSource(audioSource)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                it.start()
                updateMiniPlayer()
            }
        } else {
            updateMiniPlayer()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    // GHI CHÚ: HÀM KIỂM TRA MẠNG NHANH
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    // GHI CHÚ: HỦY RADAR KHI TẮT APP
    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {}
    }
}