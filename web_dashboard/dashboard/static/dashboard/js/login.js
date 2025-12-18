/* ============================================================
   LOGIN HANDLER
   ============================================================ */
async function loginUser(event) {
    event.preventDefault(); // cegah form submit default

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();

    // Hapus alert lama jika ada
    const oldAlert = document.querySelector(".alert");
    if (oldAlert) oldAlert.remove();

    // Validasi input
    if (!username || !password) {
        showError("Username dan password wajib diisi.");
        return;
    }

    try {
        const response = await fetch("/api/token/", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        });

        if (!response.ok) {
            let errMsg = "Login gagal, periksa username atau password Anda.";
            try {
                const errData = await response.json();
                if (errData.detail) errMsg = errData.detail;
            } catch {
                // Abaikan jika tidak bisa parsing JSON error
            }
            showError(errMsg);
            return;
        }

        const data = await response.json();

        // Simpan token ke sessionStorage agar digunakan di halaman dashboard
        sessionStorage.setItem("access_token", data.access);
        sessionStorage.setItem("refresh_token", data.refresh);
        sessionStorage.setItem("username", username);

        // Redirect ke dashboard utama
        window.location.href = "/";
    } catch (error) {
        console.error("Login error:", error);
        showError("Terjadi kesalahan koneksi ke server. Silakan coba lagi.");
    }
}

/* ============================================================
   TAMPILKAN PESAN ERROR
   ============================================================ */
function showError(message) {
    const alertBox = document.createElement("div");
    alertBox.className = "alert alert-warning text-center py-2";
    alertBox.textContent = message;

    const loginBox = document.querySelector(".login-box");
    if (loginBox) {
        loginBox.prepend(alertBox);
    } else {
        console.warn("Elemen .login-box tidak ditemukan, pesan error tidak dapat ditampilkan.");
        alert(message); // fallback
    }
}

/* ============================================================
   EVENT LISTENER
   ============================================================ */
document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    if (form) {
        form.addEventListener("submit", loginUser);
    } else {
        console.error("Form login tidak ditemukan di halaman.");
    }
});
