package com.example.hmusicv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Cấu trúc dữ liệu cho Playlist hiển thị trong Thư Viện
data class LibraryPlaylist(
    val name: String,
    val songCount: Int,
    val coverUrl: String?
)

class LibraryPlaylistAdapter(
    private val playlists: List<LibraryPlaylist>,
    private val onMoreClick: (LibraryPlaylist) -> Unit // Xử lý khi bấm nút 3 chấm
) : RecyclerView.Adapter<LibraryPlaylistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivLibPlaylistCover)
        val tvName: TextView = view.findViewById(R.id.tvLibPlaylistName)
        val tvCount: TextView = view.findViewById(R.id.tvLibSongCount)
        val btnMore: ImageView = view.findViewById(R.id.btnLibMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_library_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.tvName.text = playlist.name
        holder.tvCount.text = "${playlist.songCount} bài hát"

        // Nạp ảnh bìa (Ảnh của bài hát đầu tiên trong Playlist)
        if (playlist.coverUrl != null) {
            Glide.with(holder.itemView.context).load(playlist.coverUrl).into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(android.R.drawable.ic_menu_gallery) // Ảnh mặc định nếu chưa có bài nào
        }

        // Bắt sự kiện bấm nút 3 chấm
        holder.btnMore.setOnClickListener {
            onMoreClick(playlist)
        }
    }

    override fun getItemCount() = playlists.size
}