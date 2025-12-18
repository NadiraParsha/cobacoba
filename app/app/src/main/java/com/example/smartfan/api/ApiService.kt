package com.example.smartfan.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /**
     * 1. Mengambil data sensor TERBARU.
     * Metode: GET
     * Alamat: BASE_URL + "api/sensor/latest"
     */
    @GET("api/sensor/latest")
    suspend fun getFanStatus(): FanData

    /**
     * 2. Mengambil RIWAYAT data sensor.
     * Metode: GET
     * Alamat: BASE_URL + "api/sensor/history"
     * PENTING: Server akan mengembalikan sebuah DAFTAR/LIST dari FanData.
     */
    @GET("api/sensor/history")
    suspend fun getSensorHistory(): List<FanData> // Mengembalikan List<FanData>, bukan hanya FanData

    /**
     * 3. Mengirim perintah untuk MENGONTROL kipas (menyalakan/mematikan).
     * Metode: POST
     * Alamat: BASE_URL + "api/fan/control"
     */
    @POST("api/fan/control")
    suspend fun controlFan(
        @Body request: FanControlRequest
    ): Response<Unit>

    /**
     * 4. Membuat atau mengirim data sensor baru ke server.
     * Metode: POST
     * Alamat: BASE_URL + "api/sensor/create"
     * Catatan: Endpoint ini mungkin lebih relevan untuk perangkat IoT, bukan untuk aplikasi mobile.
     */
    @POST("api/sensor/create")
    suspend fun createSensorData(
        @Body sensorData: FanData // Mengirim data sensor sebagai body permintaan
    ): Response<Unit> // Biasanya responsnya kosong atau berisi data yang baru dibuat

    /**
     * 5. Mengirim data login untuk mendapatkan token otentikasi.
     * Metode: POST
     * Alamat: BASE_URL + "api/token/"
     */
    @POST("api/token/")
    suspend fun loginUser(@Body request: LoginRequest): Response<Unit>

    /**
     * 6. Mengirim data registrasi untuk membuat akun pengguna baru.
     * Metode: POST
     * Alamat: BASE_URL + "api/register/"
     */
    @POST("api/register/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<Unit>


    // ▼▼▼ PERUBAHAN: FUNGSI BARU UNTUK MODE AUTO DITAMBAHKAN DI SINI ▼▼▼

    /**
     * 7. Mengirim status mode auto ke server.
     * Metode: POST
     * Alamat: BASE_URL + "api/fan/set-auto/"
     */
    @POST("api/fan/set-auto/")
    suspend fun setAutoMode(@Body request: SetAutoModeRequest): Response<Unit>
    // ▲▲▲ AKHIR DARI PERUBAHAN ▲▲▲
}
