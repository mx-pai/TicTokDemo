package com.example.myapplication.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.Note
import com.example.myapplication.data.repository.FeedRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = FeedRepository()

    private var currentPage = 1
    private var pageSize = 16

    private val localNotes = mutableListOf<Note>()

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _hasMoreData = MutableLiveData<Boolean>()
    val hasMoreData: LiveData<Boolean> = _hasMoreData


    init {
        _hasMoreData.value = true
        _isLoadingMore.value = false
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage = 1
                val result = repository.getFeed()
                if (result.isSuccess) {
                    val apiNotes = result.getOrDefault(emptyList())
                    localNotes.clear()
                    localNotes.addAll(apiNotes)

                    _notes.value = localNotes.toList()
                    _hasMoreData.value = apiNotes.size >= pageSize
                }
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            if (_hasMoreData.value == false || _isLoadingMore.value == true) {
                return@launch
            }
            _isLoadingMore.value = true
            kotlinx.coroutines.delay(1000)
            try {
                currentPage++
                Log.d("MainViewModel", "loadMore: currentPage=$currentPage")
                val result = repository.getFeed()
                if (result.isSuccess) {
                    val newApiNotes = result.getOrDefault(emptyList())
                    val mergeNotes = newApiNotes.map { newNote ->
                        val existingNote = localNotes.find {it.id == newNote.id}
                        if (existingNote != null) {
                            newNote.copy(
                                isLiked = existingNote.isLiked,
                                likes = existingNote.likes
                            )
                        } else {
                            newNote
                        }
                    }

                    //addAll会出现刷新问题
                    val distinctNotes = mergeNotes.filter { newNote ->
                        localNotes.none { it.id == newNote.id }
                    }

                    if (distinctNotes.isNotEmpty()) {
                        localNotes.addAll(distinctNotes)
                        _notes.value = localNotes.toList()
                    }
                    Log.d("MainViewModel", "后端返回条数: ${newApiNotes.size}")
                    Log.d("MainViewModel", "去重后剩余条数: ${distinctNotes.size}") // 如果这里是 0，说明就是这个问题

                    _hasMoreData.value = newApiNotes.size >= pageSize
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleLike(targetNote: Note) {
        val currentList = _notes.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == targetNote.id }
        if (index != -1) {
            val oldNote = currentList[index]
            val newNote = oldNote.copy(
                isLiked = !oldNote.isLiked,
                likes = if (oldNote.isLiked) oldNote.likes - 1 else oldNote.likes + 1
            )
            currentList[index] = newNote

            val localIndex = localNotes.indexOfFirst { it.id == targetNote.id }
            if (localIndex != -1) {
                localNotes[localIndex] = newNote
            }
            _notes.value = currentList

        }
    }

    fun updateNote(updatedNote: Note) {
        _notes.value = _notes.value?.map { note ->
            if (note.id == updatedNote.id) updatedNote else note
        }
    }
}