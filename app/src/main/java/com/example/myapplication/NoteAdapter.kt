package com.example.myapplication
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class NoteAdapter(private val noteList: List<Note>) :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
        class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivCover: ImageView = view.findViewById(R.id.ivCover)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            val tvUser: TextView = view.findViewById(R.id.tvUser)
            val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return NoteViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = noteList[position]
            holder.tvTitle.text = note.title
            holder.tvUser.text = note.userName
            holder.tvLikes.text = note.likes

            Glide.with(holder.itemView.context)
                .load(note.cover)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(holder.ivCover)

            Glide.with(holder.itemView.context)
                .load(note.avatar)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(holder.ivAvatar)
        }
        override fun getItemCount(): Int {
            return noteList.size
        }
    }
