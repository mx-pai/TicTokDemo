package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Note
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.NetworkUtils.getRetrofitInstance

class FeedRepository {
    private val apiService = getRetrofitInstance()?.create(ApiService::class.java)

    suspend fun getFeed(page: Int = 1, pageSize: Int = 16): Result<List<Note>> {
        return try {
            val response = apiService?.getFeed(page, pageSize)
            if (response?.isSuccessful == true) {
                Result.success(response.body()?.data?.list ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch feed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}