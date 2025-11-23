package com.example.myapplication.ui.home.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.graphics.shapes.RoundedPolygon
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.R
import com.example.myapplication.data.model.Note
import com.example.myapplication.databinding.ItemNoteBinding
import kotlin.text.get

class NoteAdapter : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
        private val differ = AsyncListDiffer(this, NoteDiffCallback())
        var notes: List<Note>
            get() = differ.currentList
            set(value) = differ.submitList(value)

        class NoteViewHolder(
            private val binding: ItemNoteBinding
        ) : RecyclerView.ViewHolder(binding.root)  {
            fun bind(note: Note, onLikeClick: (Int) -> Unit) {
                binding.apply {
                    tvTitle.text = note.title
                    tvUser.text = note.userName
                    tvLikes.text = note.likes.toString()
                    ivLike.setImageResource(
                        if (note.isLiked) R.drawable.heart_filled else R.drawable.heart
                    )
                    ivCover.loadWithRatio(note.cover, note.coverWidth, note.coverHeight)
                    ivAvatar.loadCircular(note.avatar)

                    ivLike.setOnClickListener {
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            note.isLiked = !note.isLiked
                            if (note.isLiked) {
                                note.likes += 1
                            } else {
                                note.likes -= 1
                            }
                            binding.tvLikes.text = note.likes.toString()
                            ivLike.setImageResource(
                                if (note.isLiked) R.drawable.heart_filled else R.drawable.heart
                            )
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return NoteViewHolder(binding)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            holder.bind(notes[position]) { pos ->
                if (pos != RecyclerView.NO_POSITION) {
                    val currentNote = notes[pos]
                    currentNote.isLiked = !currentNote.isLiked
                    if (currentNote.isLiked) {
                        currentNote.likes += 1
                    } else {
                        currentNote.likes -= 1
                    }
                }
            }
        }
        override fun getItemCount(): Int = notes.size
    }
class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}

fun ImageView.loadWithRatio(url: String, width: Int, height: Int) {
    val ratio = "$width:$height"
    val params = layoutParams as ConstraintLayout.LayoutParams
    params.dimensionRatio = ratio
    layoutParams = params

    Glide.with(context)
        .load(url)
        .apply(
            RequestOptions()
                .transform(RoundedCorners(16))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.cover_placeholder)
        )
        .into(this)
}

fun ImageView.loadCircular(url: String) {
    Glide.with(context)
        .load(url)
        .circleCrop()
        .into(this)
}
