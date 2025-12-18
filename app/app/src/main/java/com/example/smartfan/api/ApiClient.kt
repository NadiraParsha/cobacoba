package com.example.smartfan.api

import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://103.151.63.68:8057/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // ▼▼▼ INTERCEPTOR MODIFIKASI: CACHE BUSTER & PENYAMARAN ▼▼▼
        .addInterceptor { chain ->
            val original = chain.request()

            // 1. TRIK URL UNIK (CACHE BUSTER)
            val originalHttpUrl: HttpUrl = original.url
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("t", System.currentTimeMillis().toString())
                .build()

            // 2. TRIK MENYAMAR JADI BROWSER (USER-AGENT SPOOFING)
            val requestBuilder = original.newBuilder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .cacheControl(CacheControl.FORCE_NETWORK)

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        // ▲▲▲ BATAS MODIFIKASI ▲▲▲

        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
