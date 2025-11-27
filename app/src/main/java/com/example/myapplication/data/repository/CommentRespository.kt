package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.CommentData
import com.example.myapplication.data.model.CommentResponse
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.NetworkUtils

class CommentRepository {
    private val apiService = NetworkUtils.getRetrofitInstance()?.create(ApiService::class.java)

    suspend fun getComments(noteId: Int, cursor: String? = null): Result<CommentData> {
        return try {
            val response = apiService?.getComments(noteId, cursor)
            if (response?.isSuccessful == true) {
                Result.success(response.body()?.data ?: CommentData(0, 0, emptyList()))
            } else {
                Result.failure(Exception(response?.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}