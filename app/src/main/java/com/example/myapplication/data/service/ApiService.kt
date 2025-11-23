package com.example.myapplication.data.service

import com.example.myapplication.data.model.FeedResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("feed")
    suspend fun getFeed(): Response<FeedResponse>
}