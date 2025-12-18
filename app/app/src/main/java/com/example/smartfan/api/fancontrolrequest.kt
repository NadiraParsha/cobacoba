package com.example.smartfan.api

import com.google.gson.annotations.SerializedName

/**
 * Model data untuk mengirim perintah kontrol kipas ke server.
 * Contoh JSON: {"status": "on"}
 */
data class FanControlRequest(
    @SerializedName("status")
    val status: String // Nilainya akan "on" atau "off"
)
    