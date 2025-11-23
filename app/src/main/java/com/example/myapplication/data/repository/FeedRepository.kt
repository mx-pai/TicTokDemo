package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Note
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.BASE_URL
import com.example.myapplication.utils.NetworkUtils.getRetrofitInstance
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedRepository {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService = getRetrofitInstance()?.create(ApiService::class.java)
    suspend fun getFeed(): Result<List<Note>> {
        return try {
            val response = apiService?.getFeed()
            if (response?.isSuccessful ?: false) {
                val notes =  response.body()?.data?.list ?: emptyList()
                Result.success(notes)
            } else {
                Result.failure(Exception("Failed to fetch feed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeedByCategory(category: String): Result<List<Note>> {
        return getFeed()
    }
}