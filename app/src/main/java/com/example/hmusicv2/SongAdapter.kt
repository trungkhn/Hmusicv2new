package com.example.hmusicv2 // Nhớ giữ nguyên dòng chữ này theo máy em nha!

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(private val songList: List<Song>) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    // 1. Tìm các "biển tên" (ID) trên giao diện
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
    }

    // 2. Lấy cái khuôn giao diện ra để chuẩn bị đúc
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_daily, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val currentSong = songList[position]

        holder.tvTitle.text = currentSong.title
        holder.tvArtist.text = currentSong.artist

        // Nhờ "phù thủy" Glide tải ảnh từ đường link trên mạng và ốp vào ImageView
        Glide.with(holder.itemView.context)
            .load(currentSong.cover)
            .into(holder.ivCover)

        // 👇 ĐOẠN MỚI THÊM: Bấm vào bài nào, mang CẢ DANH SÁCH qua bài đó 👇
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, PlayerActivity::class.java)

            // 1. Đóng gói nguyên cái danh sách nhạc ném vào phong bì (ép kiểu sang ArrayList)
            intent.putExtra("SONG_LIST", ArrayList(songList))

            // 2. Báo cho PlayerActivity biết mình đang bấm vào bài số mấy
            intent.putExtra("SONG_POSITION", position)

            // 3. Phóng qua màn hình mới!
            context.startActivity(intent)
        }
    }

    // 4. Báo cho cỗ máy biết có tổng cộng bao nhiêu bài hát
    override fun getItemCount(): Int {
        return songList.size
    }
}