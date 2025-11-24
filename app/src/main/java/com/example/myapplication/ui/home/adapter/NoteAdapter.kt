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

class NoteAdapter(
    private val onLikeClick: (Note) -> Unit,
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

        init {
            setHasStableIds(true)
        }
        private val differ = AsyncListDiffer(this, NoteDiffCallback())

        fun submitList(list: List<Note>) {
            differ.submitList(list)
        }

        override fun getItemId(position: Int): Long {
            return differ.currentList[position].id.toLong()
        }

        class NoteViewHolder(private val binding: ItemNoteBinding) :
            RecyclerView.ViewHolder(binding.root)  {
            fun bind(
                note: Note,
                onLikeClick: (Note) -> Unit,
                onNoteClick: (Note) -> Unit) {
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
                        onLikeClick(note)
                    }

                    root.setOnClickListener {
                        onNoteClick(note)
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
            val note = differ.currentList[position]
            holder.bind(note, onLikeClick, onNoteClick)
        }

    override fun getItemCount(): Int = differ.currentList.size
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
                .dontAnimate()
        )
        .into(this)
}

fun ImageView.loadCircular(url: String) {
    Glide.with(context)
        .load(url)
        .circleCrop()
        .into(this)
}

