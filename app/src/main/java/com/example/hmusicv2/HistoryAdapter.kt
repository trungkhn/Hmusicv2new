package com.example.hmusicv2

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HistoryAdapter(private val songList: List<Song>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivHistoryCover)
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]
        holder.tvTitle.text = song.title

        Glide.with(holder.itemView.context)
            .load(song.cover)
            .into(holder.ivCover)

        // Bấm vào bài trong lịch sử -> Chuyển sang màn hình Đĩa than phát nhạc luôn
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("SONG_LIST", ArrayList(songList))
            intent.putExtra("SONG_POSITION", position)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = songList.size
}