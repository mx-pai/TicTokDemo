package com.example.myapplication.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkUtils {
    private var retrofit: Retrofit? = null

    fun getRetrofitInstance(url: String): Retrofit? {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit
    }


}