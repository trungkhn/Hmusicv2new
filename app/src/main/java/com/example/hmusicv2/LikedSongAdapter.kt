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
    private val onSongClick: (Song, Int) -> Unit,
    private val onUnlikeClick: (Song, Int) -> Unit,
    // 👉 ĐÂY LÀ TÍNH NĂNG BẤM GIỮ ĐỂ HIỆN MENU SỬA/XÓA CỦA BẠN:
    private val onSongLongClick: ((Song, Int) -> Unit)? = null
) : RecyclerView.Adapter<LikedSongAdapter.ViewHolder>() {

    // Đã thêm chữ "inner" để tránh lỗi ngớ ngẩn của Android Studio
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.ivLikedSongCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvLikedSongTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvLikedSongArtist)

        // ⚠️ Nếu dòng này bị đỏ, bạn nhớ mở file item_liked_song.xml
        // kiểm tra xem ID của nút trái tim đã đúng là "btnUnlike" chưa nhé!
        val btnUnlike: ImageView = itemView.findViewById(R.id.btnUnlike)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_liked_song, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]

        holder.tvTitle.text = song.title ?: "Unknown"
        holder.tvArtist.text = song.artist ?: "Unknown"

        Glide.with(holder.itemView.context)
            .load(song.cover)
            .into(holder.ivCover)

        // 1. Sự kiện bấm 1 lần để phát nhạc
        holder.itemView.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onSongClick(song, currentPos)
            }
        }

        // 2. Sự kiện bấm vào nút trái tim (Bỏ thích / Xóa)
        holder.btnUnlike.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onUnlikeClick(song, currentPos)
            }
        }

        // 3. Sự kiện BẤM GIỮ (Long Click) để mở Bottom Sheet Đổi tên/Xóa
        holder.itemView.setOnLongClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                onSongLongClick?.invoke(song, currentPos)
            }
            true // Trả về true để Android biết thao tác nhấn giữ đã xử lý xong
        }
    }
}