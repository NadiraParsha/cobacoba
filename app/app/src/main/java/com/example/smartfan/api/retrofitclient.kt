package com.example.smartfan.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // PENTING: Ganti dengan alamat IP perangkat IoT Anda
    private const val BASE_URL = "http://192.168.1.10/" // Sesuaikan IP ini!

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
