/* === REGISTER HANDLER === */
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("registerForm");
    if (form) form.addEventListener("submit", registerUser);
});

async function registerUser(event) {
    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();
    const password2 = document.getElementById("password2").value.trim();

    const alertBox = document.getElementById("alertBox");
    alertBox.classList.add("d-none");

    if (!username || !email || !password || !password2) {
        showAlert("Semua kolom wajib diisi!");
        return;
    }

    if (password !== password2) {
        showAlert("Konfirmasi password tidak cocok!");
        return;
    }

    try {
        const response = await fetch("/api/register/", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, email, password }),
        });

        if (!response.ok) {
            let msg = "Gagal mendaftar. Coba lagi.";
            try {
                const data = await response.json();
                msg = data.detail || JSON.stringify(data);
            } catch {}
            showAlert(msg);
            return;
        }

        // Jika berhasil register
        showAlert("Pendaftaran berhasil! Mengalihkan ke halaman login...", "success");

        setTimeout(() => {
            window.location.href = "/login/";
        }, 1500);

    } catch (error) {
        console.error("Network error:", error);
        showAlert("Terjadi kesalahan jaringan. Pastikan server aktif.");
    }
}

/* === ALERT HANDLER === */
function showAlert(message, type = "warning") {
    const alertBox = document.getElementById("alertBox");
    alertBox.textContent = message;
    alertBox.className = `alert alert-${type} text-center mt-2`;
    alertBox.classList.remove("d-none");
}
