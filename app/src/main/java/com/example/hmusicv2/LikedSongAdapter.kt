package com.example.hmusicv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class LikedSongAdapter(
    private val songList: List<Song>,
    private val onSongClick: (Song, Int) -> Unit, // Bấm vào để phát nhạc
    private val onUnlikeClick: (Song) -> Unit     // Bấm vào trái tim để bỏ thích
) : RecyclerView.Adapter<LikedSongAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.ivLikedSongCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvLikedSongTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvLikedSongArtist)
        val btnUnlike: ImageView = itemView.findViewById(R.id.btnUnlike)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_library_liked_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        if (!song.cover.isNullOrEmpty()) {
            Glide.with(holder.itemView.context).load(song.cover).into(holder.ivCover)
        }

        // Bắt sự kiện phát nhạc
        holder.itemView.setOnClickListener {
            onSongClick(song, position)
        }

        // Bắt sự kiện Bỏ Thích
        holder.btnUnlike.setOnClickListener {
            onUnlikeClick(song)
        }
    }

    override fun getItemCount(): Int = songList.size
}