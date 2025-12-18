package com.example.smartfan.api

import com.google.gson.annotations.SerializedName

/**
 * Model data yang mewakili satu pembacaan sensor dari server.
 */
data class FanData(
    // Properti ini datang dari JSON server
    @SerializedName("id")
    val id: Int,

    @SerializedName("suhu")
    val suhu: Double,

    @SerializedName("kelembapan")
    val kelembapan: Double,

    @SerializedName("gas_status")
    val gas_status: String,

    // Properti ini TIDAK datang dari JSON, kita isi secara manual di aplikasi.
    // Gunakan 'var' agar nilainya bisa diubah.
    var timestamp: Long = 0L
)
