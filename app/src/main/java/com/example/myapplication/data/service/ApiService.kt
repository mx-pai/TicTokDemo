package com.example.myapplication.data.service

import com.example.myapplication.data.model.FeedResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 16
    ): Response<FeedResponse>
}