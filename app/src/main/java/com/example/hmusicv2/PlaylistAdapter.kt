package com.example.hmusicv2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Cái khuôn nhỏ chứa Tên Playlist và trạng thái (Đã thêm bài này chưa?)
data class PlaylistState(val name: String, var isAdded: Boolean)

class PlaylistAdapter(
    private val playlistList: List<PlaylistState>,
    private val onPlaylistClick: (PlaylistState) -> Unit // Hành động khi bấm vào playlist
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlaylistName: TextView = itemView.findViewById(R.id.tvPlaylistName)
        val btnToggleAdd: ImageView = itemView.findViewById(R.id.btnToggleAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_bottom_sheet, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlistList[position]
        holder.tvPlaylistName.text = playlist.name


        // PHÉP THUẬT ĐỔI MÀU & ICON Ở ĐÂY 👇
        if (playlist.isAdded) {
            // Nếu đã có bài -> Hiện dấu Tích (✓) mặc định của Android, TẮT ColorFilter đi cho nó đẹp
            holder.btnToggleAdd.setImageResource(android.R.drawable.checkbox_on_background)
            holder.btnToggleAdd.colorFilter = null
        } else {
            // Nếu chưa có -> Hiện dấu Cộng (+) màu Trắng
            holder.btnToggleAdd.setImageResource(android.R.drawable.ic_input_add)
            holder.btnToggleAdd.setColorFilter(android.graphics.Color.WHITE)
        }

        // Bắt sự kiện khi dũng sĩ bấm vào dòng Playlist này
        holder.itemView.setOnClickListener {
            onPlaylistClick(playlist)
        }
    }

    override fun getItemCount(): Int = playlistList.size
}