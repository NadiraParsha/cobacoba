package com.example.smartfan.api

import android.util.Log
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object MockApiManager {

    private val server = MockWebServer()
    private var fanStatus = "OFF"
    private var fanSpeed = 0

    // Respons JSON default saat kipas MATI
    private fun getOffResponse(): String {
        return """
        {
            "temperature": 27.5,
            "humidity": 65,
            "fan_status": "OFF",
            "fan_speed": 0
        }
        """.trimIndent()
    }

    // Respons JSON default saat kipas NYALA
    private fun getOnResponse(): String {
        return """
        {
            "temperature": 29.0,
            "humidity": 62,
            "fan_status": "ON",
            "fan_speed": 3
        }
        """.trimIndent()
    }

    fun startServer() {
        // Dispatcher berfungsi seperti "router" yang menentukan respons
        // berdasarkan request yang masuk.
        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                // Jika request adalah GET ke /status
                if (request.method == "GET" && request.path == "/status") {
                    return if (fanStatus == "ON") {
                        MockResponse().setResponseCode(200).setBody(getOnResponse())
                    } else {
                        MockResponse().setResponseCode(200).setBody(getOffResponse())
                    }
                }
                // Jika request adalah POST ke /fan/on
                else if (request.method == "POST" && request.path == "/fan/on") {
                    fanStatus = "ON"
                    fanSpeed = 3
                    Log.d("MockApiServer", "Status diubah ke ON")
                    // Setelah menyalakan, kembalikan status terbaru
                    return MockResponse().setResponseCode(200).setBody(getOnResponse())
                }
                // Jika request adalah POST ke /fan/off
                else if (request.method == "POST" && request.path == "/fan/off") {
                    fanStatus = "OFF"
                    fanSpeed = 0
                    Log.d("MockApiServer", "Status diubah ke OFF")
                    // Setelah mematikan, kembalikan status terbaru
                    return MockResponse().setResponseCode(200).setBody(getOffResponse())
                }
                // Jika endpoint tidak ditemukan
                return MockResponse().setResponseCode(404)
            }
        }
        server.dispatcher = dispatcher
        server.start(8080) // Jalankan server di port 8080
        Log.d("MockApiServer", "Server tiruan berjalan di: ${server.url("/")}")
    }

    fun getBaseUrl(): String {
        // Mengembalikan URL server yang sedang berjalan
        return server.url("/").toString()
    }

    fun stopServer() {
        server.shutdown()
        Log.d("MockApiServer", "Server tiruan dihentikan.")
    }
}
