package com.example.hmusicv2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PlaylistDetailActivity : AppCompatActivity() {

    private lateinit var rvSongs: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private var songList = mutableListOf<Song>()

    private var playlistId: String = ""
    private var playlistName: String = ""
    private var playlistCover: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        playlistId = intent.getStringExtra("PLAYLIST_ID") ?: ""
        playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"
        playlistCover = intent.getStringExtra("PLAYLIST_COVER")

        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvCount = findViewById<TextView>(R.id.tvDetailCount)
        val ivCover = findViewById<ImageView>(R.id.ivDetailCover)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnPlayAll = findViewById<CardView>(R.id.btnPlayAll)
        rvSongs = findViewById(R.id.rvPlaylistSongs)

        tvName.text = playlistName

        if (!playlistCover.isNullOrEmpty()) {
            Glide.with(this).load(playlistCover).into(ivCover)
        }

        rvSongs.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(songList)
        rvSongs.adapter = songAdapter

        loadSongsFromFirebase(tvCount)

        btnBack.setOnClickListener { finish() }

        btnPlayAll.setOnClickListener {
            if (songList.isNotEmpty()) {
                MyMediaPlayer.currentPlaylist = ArrayList(songList)
                MyMediaPlayer.currentIndex = 0
                val intent = Intent(this, PlayerActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Chưa có bài hát nào!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSongsFromFirebase(tvCount: TextView) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (playlistId.isEmpty()) return

        val dbRef = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("Users").child(userId).child("Playlists").child(playlistId).child("songs")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                songList.clear()
                for (songSnapshot in snapshot.children) {
                    val song = songSnapshot.getValue(Song::class.java)
                    if (song != null) {
                        songList.add(song)
                    }
                }
                songAdapter.notifyDataSetChanged()
                tvCount.text = "${songList.size} bài hát"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}