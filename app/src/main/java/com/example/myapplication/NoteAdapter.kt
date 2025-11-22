package com.example.myapplication
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class NoteAdapter(private var noteList: List<Note>) :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
        class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivCover: ImageView = view.findViewById(R.id.ivCover)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            val tvUser: TextView = view.findViewById(R.id.tvUser)

            val ivLike: ImageView = view.findViewById(R.id.ivLike)
            var tvLikes: TextView = view.findViewById(R.id.tvLikes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return NoteViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = noteList[position]
            val ratio = "${note.coverWidth}:${note.coverHeight}"
            Log.d("COVER", "url = ${note.cover}")

            holder.tvTitle.text = note.title
            holder.tvUser.text = note.userName
            holder.tvLikes.text = note.likes.toString()

            if (note.isLiked) {
                holder.ivLike.setImageResource(R.drawable.heart_filled)
            } else {
                holder.ivLike.setImageResource(R.drawable.heart)
            }

            val params = holder.ivCover.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = ratio
            holder.ivCover.layoutParams = params


            Glide.with(holder.itemView.context)
                .load(note.cover)
                .apply(
                    RequestOptions()
                        .transform(RoundedCorners(20))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.cover_placeholder)
                )
                .into(holder.ivCover)

            Glide.with(holder.itemView.context)
                .load(note.avatar)
                .circleCrop()
                .into(holder.ivAvatar)

            holder.ivLike.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                val currentNote = noteList[pos]
                currentNote.isLiked = !currentNote.isLiked
                if (currentNote.isLiked) {
                    currentNote.likes += 1
                } else {
                    currentNote.likes -= 1
                }
                notifyItemChanged(pos, "like")
            }
        }

        override fun getItemCount(): Int = noteList.size

        fun updateData(newData: List<Note>) {
            noteList = newData
            notifyDataSetChanged()
        }
    }
