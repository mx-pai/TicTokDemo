package com.example.myapplication

import com.google.gson.annotations.SerializedName


data class FeedResponse(
    val code: Int,
    val msg: String,
    val data: FeedData
)

data class FeedData(
    @SerializedName("list")
    val list: List<Note>
)

data class Note(
    val id: Int,
    val title: String,
    @SerializedName("userName")
    val userName: String,
    val avatar: String,
    val cover: String,
    val likes: String,
    val isVideo: Boolean
)
