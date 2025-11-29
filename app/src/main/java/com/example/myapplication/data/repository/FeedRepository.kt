package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Note
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.Constants.BASE_NOTE_URL
import com.example.myapplication.utils.NetworkUtils.getRetrofitInstance

class FeedRepository {
    private val apiService = getRetrofitInstance(BASE_NOTE_URL).create(ApiService::class.java)

    suspend fun getFeed(): Result<List<Note>> {
        return try {
            val response = apiService?.getFeed()
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