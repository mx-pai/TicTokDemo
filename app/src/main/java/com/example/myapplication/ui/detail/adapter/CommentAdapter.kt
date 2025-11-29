package com.example.myapplication.ui.detail.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Comment
import com.example.myapplication.databinding.ItemCommentBinding
import com.example.myapplication.R
import com.example.myapplication.ui.home.adapter.loadCircular

class CommentAdapter(
    private val onLikeClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

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
        @SuppressLint("SetTextI18n")
        fun bind(comment: Comment) {
            binding.apply {
                tvUserName.text = comment.userName
                tvContent.text = if (comment.replyToUsername != null) {
                    "回复${comment.replyToUsername} ${comment.content}"
                } else {
                    comment.content
                }
                tvInfo.text = "${comment.timestamp} ${comment.location.take(2)}"
                tvLikeCount.text = comment.likes.toString()
                ivAvatar.loadCircular(comment.avatar)

                tvReply.setOnClickListener {
                    onReplyClick(comment)
                }

                ivCommentLike.setImageResource(
                    if (comment.isLiked) R.drawable.heart_filled else R.drawable.heart
                )
                ivCommentLike.setOnClickListener {
                    val updated = comment.copy(
                        isLiked = !comment.isLiked,
                        likes = if (comment.isLiked) comment.likes - 1 else comment.likes + 1
                    )
                    Log.d("CommentAdapter", "onLikeClick: $updated")
//                    val updatedList = currentList.toMutableList()
//                    val index = updatedList.indexOfFirst { it.id == updated.id }
//                    if (index != -1) {
//                        updatedList[index] = updated
//                        submitList(updatedList.toList())
//                    }
                    onLikeClick(updated)
                }
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