package com.example.hmusicv2

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class SearchFragment : Fragment() {

    private lateinit var adapter: SongAdapter
    private var fullSongList = mutableListOf<Song>()
    private var filteredList = mutableListOf<Song>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvSearchResults = view.findViewById<RecyclerView>(R.id.rvSearchResults)

        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        adapter = SongAdapter(filteredList)
        rvSearchResults.adapter = adapter

        val database = FirebaseDatabase.getInstance("https://hmusicv2-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        database.child("Songs").get().addOnSuccessListener { snapshot ->
            fullSongList.clear()
            for (songSnapshot in snapshot.children) {
                val song = songSnapshot.getValue(Song::class.java)
                if (song != null) {
                    fullSongList.add(song)
                }
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterSongs(query)
            }
        })

        return view
    }

    private fun filterSongs(query: String) {
        filteredList.clear()
        if (query.isNotEmpty()) {
            for (song in fullSongList) {
                if (song.title?.lowercase()?.contains(query.lowercase()) == true) {
                    filteredList.add(song)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}