package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

data class FeedResponse(
    val code: Int,
    val msg: String,
    val data: FeedData
)

data class FeedData(
    @SerializedName("list")
    val list: List<Note>
)

data class CommentResponse(
    val code: Int,
    val msg: String,
    val data: CommentData
)

data class CommentData(
    val total: Int,
    val nextCursor: String?,
    @SerializedName("list")
    val list: List<Comment>
)

@Parcelize
data class Note(
    val id: Int,
    val title: String,
    @SerializedName("userName")
    val userName: String,
    val avatar: String,
    val cover: String,
    val coverWidth: Int,
    val coverHeight: Int,
    //之后可以添加视频等
    var likes: Int,
    val isVideo: Boolean,
    var isLiked: Boolean = false,
    val images: List<String>? = null
) : Parcelable


data class Comment(
    val id: Int,
    val userName: String,
    val avatar: String,
    val content: String,
    val timestamp: String,
    val location: String,
    val likes: Int,
    val isLiked: Boolean
)
