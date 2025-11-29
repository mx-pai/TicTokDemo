package com.example.myapplication.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkUtils {
    private var retrofitCache: MutableMap<String, Retrofit> = mutableMapOf()

    fun getRetrofitInstance(url: String): Retrofit {
        return retrofitCache.getOrPut(url) {
            Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}