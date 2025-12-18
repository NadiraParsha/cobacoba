package com.example.smartfan.api

import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object ApiClient {

    private const val BASE_URL = "http://103.151.63.68:8057/"
    private const val TIMEOUT_DURATION = 30L

    // User-Agent Chrome untuk memastikan server merespons selayaknya browser
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Interceptor Khusus: Menambahkan Cache Buster & Header Penyamaran
        .addInterceptor { chain ->
            val originalRequest = chain.request()

            // 1. Tambahkan parameter waktu unik agar server tidak mengirim data cache lama
            val urlWithTimestamp = originalRequest.url.newBuilder()
                .addQueryParameter("t", System.currentTimeMillis().toString())
                .build()

            // 2. Samarkan request agar terlihat seperti dari browser Chrome & paksa ambil dari jaringan
            val newRequest = originalRequest.newBuilder()
                .url(urlWithTimestamp)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

            chain.proceed(newRequest)
        }
        .connectTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_DURATION, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // --- FUNGSI HELPER (DIPINDAHKAN KE DALAM OBJECT) ---

    /**
     * Mengecek apakah device online berdasarkan timestamp terakhir.
     * Mengembalikan TRUE jika data terakhir kurang dari 1 menit yang lalu.
     * Menerima input Long (timestamp) sesuai data dari server.
     */
    fun isDeviceOnline(timestamp: Long?): Boolean {
        if (timestamp == null || timestamp == 0L) return false

        val currentTime = System.currentTimeMillis()
        // Hitung selisih waktu (absolute value untuk menghindari error jika jam HP user lebih lambat)
        val diff = abs(currentTime - timestamp)

        // Batas toleransi 1 menit (60.000 milidetik)
        return diff < 60000
    }

    /**
     * Format tanggal dari timestamp (Long) ke format Indonesia yang mudah dibaca.
     * Contoh output: "12 Juli 2025 07:34"
     */
    fun formatDateTime(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            outputFormat.format(date)
        } catch (e: Exception) {
            "-"
        }
    }

    /**
     * Format tanggal dari String ISO (Jika diperlukan untuk tipe data lain)
     */
    fun formatDateTimeIso(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "-"
        return try {
            val cleanIso = if (isoString.length > 19) isoString.substring(0, 19) else isoString
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))

            val date = inputFormat.parse(cleanIso)
            if (date != null) outputFormat.format(date) else isoString
        } catch (e: Exception) {
            isoString ?: "-"
        }
    }
}
