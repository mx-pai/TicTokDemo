package com.example.myapplication.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Note
import com.example.myapplication.data.service.ApiService
import com.example.myapplication.utils.BASE_URL
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val response = retrofit.create(ApiService::class.java).getFeed()
                if (response.isSuccessful) {
                    _notes.value = response.body()?.data?.list ?: emptyList()
                }
            } catch (e: Exception) {
                // 错误处理
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}