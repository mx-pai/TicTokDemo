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
    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // 这里调用 Repository 里的 suspend 函数
                val data = repository.getFeedByCategory("")
                _notes.value = data.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}