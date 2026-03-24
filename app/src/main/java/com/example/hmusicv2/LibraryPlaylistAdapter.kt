package com.example.hmusicv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class LibraryPlaylistAdapter(
    private val playlistList: List<Playlist>,
    private val onPlaylistClick: (Playlist) -> Unit,      // Sự kiện: Chạm nhẹ
    private val onPlaylistLongClick: (Playlist) -> Unit   // Sự kiện: Bấm giữ
) : RecyclerView.Adapter<LibraryPlaylistAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.ivLibPlaylistCover)
        val tvName: TextView = itemView.findViewById(R.id.tvLibPlaylistName)
        val tvCount: TextView = itemView.findViewById(R.id.tvLibSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_library_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlistList[position]

        holder.tvName.text = playlist.name
        holder.tvCount.text = "${playlist.songCount} bài hát"

        // PHÂN BIỆT MỤC "ĐÃ THÍCH" VÀ "PLAYLIST BÌNH THƯỜNG"
        if (playlist.id == "LIKED_SONGS_SPECIAL_ID") {
            // 1. Giao diện cho mục Đã thích: Nền khối màu Hồng, Icon Ngôi sao/Trái tim màu trắng
            (holder.ivCover.parent as androidx.cardview.widget.CardView).setCardBackgroundColor(android.graphics.Color.parseColor("#FF2D55"))
            holder.ivCover.setImageResource(android.R.drawable.btn_star_big_on) // Dùng icon ngôi sao mặc định
            holder.ivCover.setColorFilter(android.graphics.Color.WHITE)

        } else {
            // 2. Giao diện Playlist bình thường: Nền xám nhạt, Tải ảnh bằng Glide
            (holder.ivCover.parent as androidx.cardview.widget.CardView).setCardBackgroundColor(android.graphics.Color.parseColor("#E5E5EA"))

            if (!playlist.cover.isNullOrEmpty()) {
                holder.ivCover.clearColorFilter()
                Glide.with(holder.itemView.context).load(playlist.cover).into(holder.ivCover)
            } else {
                holder.ivCover.setImageResource(android.R.drawable.ic_menu_gallery)
                holder.ivCover.setColorFilter(android.graphics.Color.parseColor("#8E8E93"))
            }
        }

        // Chạm nhẹ
        holder.itemView.setOnClickListener {
            onPlaylistClick(playlist)
        }

        // Bấm giữ
        holder.itemView.setOnLongClickListener {
            onPlaylistLongClick(playlist)
            true
        }
    }

    override fun getItemCount(): Int = playlistList.size
}