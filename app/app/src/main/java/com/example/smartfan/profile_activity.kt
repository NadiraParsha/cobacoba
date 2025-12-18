package com.example.smartfan

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    // Daftar ID drawable untuk avatar yang bisa dipilih
    private val avatarOptions = listOf(
        R.drawable.baseline_person_24,
        R.drawable.outline_face_6_24,
        R.drawable.baseline_face_3_24,
        R.drawable.baseline_pets_24
    )

    private lateinit var ivProfileAvatar: ImageView
    private lateinit var etProfileName: EditText

    // Variabel untuk menyimpan pilihan avatar saat ini
    private var selectedAvatarId: Int = R.drawable.baseline_person_24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Hubungkan komponen dari layout
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        etProfileName = findViewById(R.id.etProfileName)
        val btnChangeAvatar: Button = findViewById(R.id.btnChangeAvatar)
        val btnSaveProfile: Button = findViewById(R.id.btnSaveProfile)
        val btnBack: ImageButton = findViewById(R.id.btnBack)

        // Muat data yang tersimpan dari SharedPreferences
        loadProfileData()

        // Listener untuk tombol kembali (panah di kiri atas)
        btnBack.setOnClickListener {
            finish() // Menutup activity saat ini dan kembali ke sebelumnya (Dashboard)
        }

        // Listener untuk tombol ganti avatar
        btnChangeAvatar.setOnClickListener {
            showAvatarSelectionDialog()
        }

        // Listener untuk tombol simpan
        btnSaveProfile.setOnClickListener {
            saveProfileData()
        }
    }

    /**
     * Menampilkan dialog untuk memilih avatar.
     */
    private fun showAvatarSelectionDialog() {
        // Nama yang akan tampil di dialog, sesuai urutan di avatarOptions
        val avatarNames = arrayOf("Orang", "Wajah", "Mood", "Peliharaan")

        AlertDialog.Builder(this)
            .setTitle("Pilih Avatar Baru")
            .setItems(avatarNames) { dialog, which ->
                // 'which' adalah indeks dari item yang diklik
                selectedAvatarId = avatarOptions[which]
                ivProfileAvatar.setImageResource(selectedAvatarId)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Menyimpan nama dan ID avatar ke SharedPreferences.
     */
    private fun saveProfileData() {
        val newName = etProfileName.text.toString()

        if (newName.isBlank()) {
            Toast.makeText(this, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        // SharedPreferences adalah cara mudah menyimpan data simpel (seperti setting)
        // "ProfilePrefs" adalah nama file penyimpanan
        val sharedPrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        editor.putString("USER_NAME", newName)
        editor.putInt("AVATAR_ID", selectedAvatarId)
        editor.apply() // .apply() menyimpan secara asinkron (lebih efisien)

        Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
        finish() // Kembali ke Dashboard setelah menyimpan
    }

    /**
     * Memuat nama dan ID avatar dari SharedPreferences.
     */
    private fun loadProfileData() {
        val sharedPrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)

        // Muat nama, jika tidak ada, gunakan "Pengguna Baru" sebagai default
        val savedName = sharedPrefs.getString("USER_NAME", "Pengguna Baru")
        etProfileName.setText(savedName)

        // Muat ID avatar, jika tidak ada, gunakan avatar pertama sebagai default
        selectedAvatarId = sharedPrefs.getInt("AVATAR_ID", avatarOptions.first())
        ivProfileAvatar.setImageResource(selectedAvatarId)
    }
}
