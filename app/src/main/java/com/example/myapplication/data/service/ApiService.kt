package com.example.myapplication.data.service

import android.database.AbstractCursor
import com.example.myapplication.data.model.CommentResponse
import com.example.myapplication.data.model.FeedResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 16
    ): Response<FeedResponse>

    @GET("comments/{noteId}")
    suspend fun getComments(
        @Path("noteId") noteId: Int,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<CommentResponse>

}
