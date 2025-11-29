package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.CommentData
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.Constants.BASE_NOTE_COMMENT_URL
import com.example.myapplication.utils.NetworkUtils

object CommentRepository {
    private val apiService = NetworkUtils.getRetrofitInstance(BASE_NOTE_COMMENT_URL).create(ApiService::class.java)
    private val commentCache = mutableMapOf<Int, CommentData>()

    suspend fun getComments(noteId: Int, cursor: String? = null): Result<CommentData> {
        commentCache[noteId]?.let { cached ->
            return Result.success(cached)
        }
        return try {
            val response = apiService.getComments(noteId, cursor)
            if (response.isSuccessful) {
                val commentData = response.body()?.data ?: CommentData(0, null, emptyList())
                commentCache[noteId] = commentData
                Result.success(commentData)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            commentCache[noteId]?.let { cached ->
                Result.success(cached)
            } ?: Result.failure(e)
        }
    }

    fun addCommentToCache(noteId: Int, comment: Comment) {
        val current = commentCache[noteId]?.list?.toMutableList() ?: mutableListOf()
        current.add(comment)
        commentCache[noteId] = CommentData(0, null, current)
    }

    fun addLocalCommentToCache(noteId: Int, localComment: List<Comment>) {
        val currentData = commentCache[noteId]
        val serverList = currentData?.list ?: emptyList()
        val merged = (localComment + serverList).distinctBy { it.id }
        commentCache[noteId] = CommentData(merged.size, null, merged)
    }
}