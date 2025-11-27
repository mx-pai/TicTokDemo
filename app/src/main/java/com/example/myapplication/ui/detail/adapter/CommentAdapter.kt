package com.example.myapplication.ui.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Comment
import com.example.myapplication.databinding.ItemCommentBinding
import com.example.myapplication.R
import com.example.myapplication.ui.home.adapter.loadCircular

class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CommentViewHolder(binding)
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            binding.apply {
                tvUserName.text = comment.userName
                tvContent.text = comment.content
                tvInfo.text = "${comment.timestamp} ${comment.location}"
                tvLikeCount.text = comment.likes.toString()
                ivAvatar.loadCircular(comment.avatar)

                ivCommentLike.setImageResource(
                    if (comment.isLiked) R.drawable.heart_filled else R.drawable.heart
                )

            }
        }
    }

    override fun onBindViewHolder(holder: CommentAdapter.CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<Comment>(){
    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem.id == newItem.id
    }

}