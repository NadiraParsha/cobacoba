package com.example.smartfan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menghubungkan file Kotlin ini dengan file XML-nya
        setContentView(R.layout.activity_register)

        // Menemukan semua komponen dari layout XML
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginHere = findViewById<TextView>(R.id.tvLoginHere)

        // Memberi aksi klik pada tombol "DAFTAR"
        btnRegister.setOnClickListener {
            // Ambil data dari semua kolom input
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // TODO: Tambahkan validasi di sini (misal: cek kolom kosong, password sama, dll)
            // Untuk saat ini, kita hanya tampilkan pesan
            Toast.makeText(this, "Tombol Daftar diklik!", Toast.LENGTH_SHORT).show()

            // TODO: Tambahkan logika untuk menyimpan data ke Firebase
        }

        // Memberi aksi klik pada teks "Login disini"
        tvLoginHere.setOnClickListener {
            // Pindah kembali ke halaman MainActivity (Login)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup halaman register
        }
    }
}
