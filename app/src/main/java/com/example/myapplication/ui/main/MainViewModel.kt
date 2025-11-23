package com.example.myapplication.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Note
import com.example.myapplication.data.repository.FeedRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = FeedRepository()

    private var currentPage = 1
    private var pageSize = 16
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
    }

    fun loadFirstPage() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage = 1
                val result = repository.getFeed()
                if (result.isSuccess) {
                    _notes.value = result.getOrDefault(emptyList())
                    _hasMoreData.value = result.getOrDefault(emptyList()).size >= pageSize
                } else {
                    _notes.value = emptyList()
                    _hasMoreData.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            if (_hasMoreData.value == false || _isLoadingMore.value == true) {
                return@launch
            }
            _isLoading.value = true
            try {
                currentPage++
                val result = repository.getFeed(currentPage, pageSize)
                if (result.isSuccess) {
                    val newNotes = result.getOrDefault(emptyList())
                    val allNotes = _notes.value.orEmpty() + newNotes
                    _notes.value = allNotes
                    _hasMoreData.value = newNotes.size >= pageSize
                } else {
                    _hasMoreData.value = false
                }
            } catch (e: Exception) {
                currentPage--
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadFirstPage()
    }
}