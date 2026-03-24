package com.example.hmusicv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(private val songList: List<Song>) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_daily, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val currentSong = songList[position]

        holder.tvTitle.text = currentSong.title
        holder.tvArtist.text = currentSong.artist

        Glide.with(holder.itemView.context)
            .load(currentSong.cover)
            .into(holder.ivCover)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, PlayerActivity::class.java)
            intent.putExtra("SONG_LIST", ArrayList(songList))
            intent.putExtra("SONG_POSITION", position)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = songList.size
}