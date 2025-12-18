package com.example.smartfan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // <-- [1] Import TextView ditambahkan
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartfan.api.ApiClient
import com.example.smartfan.api.LoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // [2] Deklarasikan semua komponen UI di sini agar mudah diakses
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var tvGoToRegister: TextView // Deklarasi untuk tombol navigasi register

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Panggil fungsi untuk inisialisasi dan setup
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        // Menghubungkan komponen dari layout XML ke variabel
        usernameEditText = findViewById(R.id.etUsername)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister) // Inisialisasi TextView
    }

    private fun setupListeners() {
        // Menambahkan aksi ketika tombol login di-klik
        loginButton.setOnClickListener {
            // Panggil fungsi untuk melakukan login
            performLogin()
        }

        // [3] Menambahkan aksi ketika teks "Daftar di sini" di-klik
        tvGoToRegister.setOnClickListener {
            // Membuat "Intent" untuk pindah ke halaman RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Fungsi untuk menangani proses login melalui API.
     */
    private fun performLogin() {
        // Mengambil teks yang diinput oleh pengguna
        val inputUsername = usernameEditText.text.toString().trim()
        val inputPassword = passwordEditText.text.toString().trim()

        // Validasi dasar: pastikan input tidak kosong
        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(this, "Username dan Password harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan pesan loading
        Toast.makeText(this, "Mencoba login...", Toast.LENGTH_SHORT).show()

        // Buat objek request untuk dikirim ke API
        val loginRequest = LoginRequest(username = inputUsername, password = inputPassword)

        // [4] Panggil API menggunakan Coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Menggunakan endpoint "api/token/" dari Postman
                val response = ApiClient.instance.loginUser(loginRequest)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Jika login berhasil
                        Toast.makeText(this@MainActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                        // Pindah ke halaman Dashboard
                        val intent = Intent(this@MainActivity, Dashboard::class.java)
                        startActivity(intent)
                        finish() // Tutup halaman login

                    } else {
                        // Jika login gagal (misal: username/password salah)
                        val errorBody = response.errorBody()?.string()
                        Log.e("LoginError", "Response Code: ${response.code()}, Body: $errorBody")
                        Toast.makeText(this@MainActivity, "Login Gagal: Username atau Password salah", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // Jika terjadi error koneksi
                withContext(Dispatchers.Main) {
                    Log.e("LoginError", "Exception: ${e.message}")
                    Toast.makeText(this@MainActivity, "Login Gagal: Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
