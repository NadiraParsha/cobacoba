/* === VARIABEL GLOBAL === */
let chart;
let lastUpdateTime;

/* === CEK LOGIN STATUS === */
const accessToken = sessionStorage.getItem("access_token");
const username = sessionStorage.getItem("username");

if (!accessToken) {
    window.location.href = "/login/";
}

/* === UTILITAS FETCH API DENGAN JWT === */
async function apiFetch(url, options = {}) {
    const token = sessionStorage.getItem("access_token");
    const fetchOptions = {
        ...options,
        headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
            ...options.headers,
        },
    };

    try {
        const res = await fetch(url, fetchOptions);
        if (res.status === 401 || res.status === 403) {
            console.warn("Token invalid / expired. Redirect ke login...");
            sessionStorage.clear();
            window.location.href = "/login/";
            return null;
        }
        return res;
    } catch (err) {
        console.error("API fetch error:", err);
        updateConnectionStatus(false);
        updateDeviceStatus(false);
        return null;
    }
}

/* === STATUS KONEKSI DAN DEVICE === */
function updateConnectionStatus(isConnected) {
    const header = document.getElementById("connectionStatus");
    const footer = document.getElementById("footerConnectionStatus");

    const connectedHTML = '<i class="fas fa-circle text-success me-1"></i> Terhubung';
    const disconnectedHTML = '<i class="fas fa-circle text-danger me-1"></i> Terputus';

    if (header) header.innerHTML = isConnected ? connectedHTML : disconnectedHTML;
    if (footer) footer.innerHTML = isConnected
        ? '<i class="fas fa-circle text-success me-1"></i> Sistem Aktif'
        : '<i class="fas fa-circle text-danger me-1"></i> Sistem Offline';
}

function updateDeviceStatus(isOnline) {
    const badge = document.getElementById("deviceStatusBadge");

    if (!badge) return;

    if (isOnline) {
        badge.innerHTML = '<i class="fas fa-circle text-success me-1"></i> <span>ONLINE</span>';
    } else {
        badge.innerHTML = '<i class="fas fa-circle text-danger me-1"></i> <span>OFFLINE</span>';
    }
}

/* === LOGOUT HANDLER === */
async function logout() {
    try {
        const refresh_token = sessionStorage.getItem("refresh_token");
        await apiFetch("/api/logout/", {
            method: "POST",
            body: JSON.stringify({ refresh: refresh_token }),
        });
    } catch (err) {
        console.warn("Logout error:", err);
    } finally {
        sessionStorage.clear();
        window.location.href = "/login/";
    }
}

/* === UTILITAS SUHU & STATUS === */
function getTemperatureStatus(suhu) {
    if (suhu === null || suhu === undefined) return "UNKNOWN";
    const temp = parseFloat(suhu);
    if (isNaN(temp)) return "UNKNOWN";
    if (temp > 32) return "HIGH";
    if (temp >= 28 && temp <= 32) return "MEDIUM";
    if (temp >= 25 && temp < 28) return "LOW";
    return "OFF";
}

function getTemperatureStatusText(suhu) {
    if (suhu === null || suhu === undefined) return "TIDAK ADA DATA";
    const temp = parseFloat(suhu);
    if (isNaN(temp)) return "DATA INVALID";
    if (temp > 32) return "PANAS";
    if (temp >= 28 && temp <= 32) return "HANGAT";
    if (temp >= 25 && temp < 28) return "SEJUK";
    return "DINGIN";
}

function convertApiStatusToStandard(apiStatus) {
    if (!apiStatus) return "UNKNOWN";
    const status = apiStatus.toString().trim().toUpperCase();

    if (status === "ON") return "LOW";
    if (["HIGH", "MEDIUM", "LOW", "OFF"].includes(status)) return status;

    return "UNKNOWN";
}

function convertStatusToDisplay(status) {
    if (["HIGH", "MEDIUM", "LOW"].includes(status)) return "ON";
    if (status === "OFF") return "OFF";
    return "-";
}

function getFanBadgeText(status) {
    switch (status) {
        case "HIGH": return "HIGH";
        case "MEDIUM": return "MEDIUM";
        case "LOW": return "LOW";
        case "OFF": return "OFF";
        default: return "UNKNOWN";
    }
}

/* === BACA FLAG AI DARI TEMPLATE === */
function readAiFlagFromDOM() {
    const el = document.getElementById("aiFlag");
    if (!el) return null;
    const txt = el.textContent.trim().toUpperCase();
    return (txt === "AKTIF" || txt === "NONAKTIF") ? txt : null;
}

/* === AMBIL DEVICE STATUS (real-time dari MQTT / backend) === */
async function loadDeviceStatus() {
    try {
        const res = await apiFetch("/api/device/status/");
        if (!res || !res.ok) {
            console.warn("Endpoint device/status belum tersedia, fallback ke sensor data");
            const sensorData = await loadLatestSensor();
            
            const simulatedStatus = {
                device_online: true,
                fan_status: sensorData?.status_kipas || "OFF",
                manual_override: false,
                last_sensor: sensorData
            };
            
            updateDeviceStatusUI(simulatedStatus);
            updateConnectionStatus(true);
            updateDeviceStatus(true); // TAMBAHKAN INI
            return simulatedStatus;
        }
        
        const data = await res.json();
        console.log("=== DEVICE STATUS ===", data);
        updateDeviceStatusUI(data);
        updateConnectionStatus(data.device_online);
        updateDeviceStatus(data.device_online); // TAMBAHKAN INI
        return data;
    } catch (err) {
        console.error("loadDeviceStatus error:", err);
        
        const fallbackStatus = {
            device_online: false,
            fan_status: "OFF",
            manual_override: false,
            last_sensor: null
        };
        
        updateDeviceStatusUI(fallbackStatus);
        updateConnectionStatus(false);
        updateDeviceStatus(false); // TAMBAHKAN INI
        return null;
    }
}

/* === UPDATE UI UNTUK DEVICE STATUS === */
function updateDeviceStatusUI(data) {
    if (!data) return;

    const online = data.device_online;
    const fanStatus = data.fan_status;
    const manual = data.manual_override;

    // indikator online/offline - DIPINDAHKAN KE loadDeviceStatus
    // updateConnectionStatus(online);

    // tampilkan mode manual/otomatis di status panel
    const modeEl = document.getElementById("modeStatus");
    if (modeEl) {
        modeEl.className = "status-badge";
        if (manual) {
            modeEl.classList.add("status-off");
            modeEl.textContent = "MANUAL";
        } else {
            modeEl.classList.add("status-on");
            modeEl.textContent = "AUTO";
        }
    }

    // update mode di stat card
    const modeValEl = document.getElementById("modeVal");
    if (modeValEl) {
        modeValEl.innerText = manual ? "MANUAL" : "AUTO";
    }

    // status kipas (HIGH/MEDIUM/LOW/OFF)
    const fanBadge = document.getElementById("fanStatusBadge");
    if (fanBadge) {
        fanBadge.className = "status-badge";

        const s = fanStatus.toString().toUpperCase();

        const classMap = {
            HIGH: "status-on",
            MEDIUM: "status-medium",
            LOW: "status-low",
            OFF: "status-off"
        };

        fanBadge.classList.add(classMap[s] || "status-off");
        fanBadge.textContent = s;
    }

    // update nilai kipas (ON/OFF)
    const fanValEl = document.getElementById("fanVal");
    if (fanValEl) {
        const s = fanStatus.toString().toUpperCase();
        fanValEl.innerText = ["HIGH", "MEDIUM", "LOW"].includes(s) ? "ON" : "OFF";
    }

    // update sensor terakhir jika backend mengirim
    if (data.last_sensor) {
        updateSensorUI(data.last_sensor);
    }
}

/* === AMBIL DATA SENSOR TERBARU === */
async function loadLatestSensor() {
    try {
        const res = await apiFetch("/api/sensor/latest/");
        if (!res || !res.ok) throw new Error("Gagal fetch sensor terbaru");
        const data = await res.json();

        console.log("=== DATA SENSOR TERBARU ===", data);
        updateSensorUI(data);
        return data;
    } catch (err) {
        console.error("loadLatestSensor error:", err);
        return null;
    }
}

/* === UPDATE UI DASHBOARD === */
function updateSensorUI(data) {
    if (!data) return console.error("Data sensor kosong!");

    const suhu = data.suhu;
    const kelembapan = data.kelembapan;
    const statusKipasApi = data.status_kipas;
    const waktu = data.waktu;

    const statusKipasStandard = convertApiStatusToStandard(statusKipasApi);
    const calculatedStatus = getTemperatureStatus(suhu);

    // Debug log
    console.log("Suhu:", suhu, "| Kelembapan:", kelembapan);
    console.log("API:", statusKipasApi, "→", statusKipasStandard);
    console.log("AI Prediksi (level):", calculatedStatus);

    /* === NILAI SENSOR === */
    const suhuEl = document.getElementById("suhuVal");
    const humEl = document.getElementById("humVal");
    const aiValEl = document.getElementById("aiVal");

    if (suhuEl) suhuEl.innerText = suhu !== null && suhu !== undefined ? suhu + " °C" : "-";
    if (humEl) humEl.innerText = kelembapan !== null && kelembapan !== undefined ? kelembapan + " %" : "-";
    if (aiValEl) aiValEl.innerText = (calculatedStatus && calculatedStatus !== "UNKNOWN") ? calculatedStatus : "-";

    /* === STATUS AI (AKTIF / NONAKTIF saja) === */
    const aiBadge = document.getElementById("aiStatusBadge");
    const aiFlag = readAiFlagFromDOM();

    if (aiBadge) {
        aiBadge.className = "status-badge";

        if (aiFlag === "AKTIF") {
            aiBadge.classList.add("status-on");
            aiBadge.textContent = "AKTIF";
        } else if (aiFlag === "NONAKTIF") {
            aiBadge.classList.add("status-off");
            aiBadge.textContent = "NONAKTIF";
        } else {
            if (calculatedStatus !== "UNKNOWN") {
                if (["HIGH", "MEDIUM", "LOW"].includes(calculatedStatus) &&
                    calculatedStatus === statusKipasStandard) {
                    aiBadge.classList.add("status-on");
                    aiBadge.textContent = "AKTIF";
                } else {
                    aiBadge.classList.add("status-off");
                    aiBadge.textContent = "NONAKTIF";
                }
            } else {
                aiBadge.classList.add("status-off");
                aiBadge.textContent = "NONAKTIF";
            }
        }
    }

    /* === KONDISI RUANGAN === */
    const tempStatus = getTemperatureStatus(suhu);
    const tempText = getTemperatureStatusText(suhu);
    const roomCondition = document.getElementById("roomCondition");

    if (roomCondition) {
        roomCondition.className = "status-badge";
        roomCondition.textContent = tempText;

        const classMap = {
            HIGH: "status-off",
            MEDIUM: "status-medium",
            LOW: "status-low",
            OFF: "status-on",
        };
        roomCondition.classList.add(classMap[tempStatus] || "status-off");
    }

    /* === REKOMENDASI === */
    const recommendation = document.getElementById("recommendation");
    if (recommendation) {
        let message = "";
        switch (calculatedStatus) {
            case "HIGH": message = "Suhu tinggi — kipas seharusnya HIGH"; break;
            case "MEDIUM": message = "Suhu hangat — kipas seharusnya MEDIUM"; break;
            case "LOW": message = "Suhu sejuk — kipas seharusnya LOW"; break;
            case "OFF": message = "Suhu dingin — kipas seharusnya OFF"; break;
            default: message = "Data suhu tidak valid";
        }
        recommendation.innerText = message;
    }

    const updateTimeElement = document.getElementById("updateTime");
    if (updateTimeElement) {
        updateTimeElement.innerText = waktu
            ? new Date(waktu).toLocaleTimeString("id-ID", { hour12: false })
            : "-";
    }
    
    lastUpdateTime = waktu ? new Date(waktu) : null;
}

/* === AMBIL HISTORY === */
async function loadHistory() {
    try {
        const res = await apiFetch("/api/sensor/history/?limit=100");
        if (!res || !res.ok) throw new Error("Gagal ambil history");
        const data = await res.json();

        updateChart(data);
        updateHistoryTable(data);
        updateConnectionStatus(true);
        updateDeviceStatus(true);
    } catch (err) {
        console.error("loadHistory error:", err);
        updateConnectionStatus(false);
        updateDeviceStatus(false);
    }
}

/* === CHART MONITORING === */
function updateChart(data) {
    const ctx = document.getElementById("chartTemp");
    if (!ctx) return;

    const labels = data.map(d => new Date(d.waktu).toLocaleTimeString("id-ID", { 
        hour12: false,
        timeZone: "Asia/Jakarta"
    }));
    
    const temps = data.map(d => d.suhu);
    const hums = data.map(d => d.kelembapan);

    if (chart) chart.destroy();

    chart = new Chart(ctx.getContext("2d"), {
        type: "line",
        data: {
            labels,
            datasets: [
                {
                    label: "Suhu (°C)",
                    data: temps,
                    borderColor: "rgba(255,99,132,1)",
                    backgroundColor: "rgba(255,99,132,0.1)",
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                },
                {
                    label: "Kelembapan (%)",
                    data: hums,
                    borderColor: "rgba(54,162,235,1)",
                    backgroundColor: "rgba(54,162,235,0.1)",
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { labels: { color: "#fff", font: { size: 12 } } },
                tooltip: {
                    backgroundColor: "rgba(0,0,0,0.7)",
                    titleColor: "#fff",
                    bodyColor: "#fff",
                },
            },
            scales: {
                x: { ticks: { color: "#fff" }, grid: { color: "rgba(255,255,255,0.1)" } },
                y: { ticks: { color: "#fff" }, grid: { color: "rgba(255,255,255,0.1)" } },
            },
        },
    });
}

/* === TABEL HISTORY === */
function updateHistoryTable(data) {
    const tbody = document.querySelector("#logTable");
    if (!tbody) return;

    tbody.innerHTML = "";
    data.slice().reverse().forEach(row => {
        const statusStandard = convertApiStatusToStandard(row.status_kipas);
        const statusText = getFanBadgeText(statusStandard);
        const classMap = {
            HIGH: "status-on",
            MEDIUM: "status-medium",
            LOW: "status-low",
            OFF: "status-off",
        };

        // Ambil mode dari data_tambahan jika ada, atau default AUTO
        let mode = "AUTO";
        try {
            if (row.data_tambahan) {
                const additionalData = JSON.parse(row.data_tambahan);
                mode = additionalData.manual_override ? "MANUAL" : "AUTO";
            }
        } catch (e) {
            console.warn("Gagal parse data_tambahan:", e);
            mode = "AUTO";
        }

        // Tentukan class badge untuk mode
        const modeClass = mode === "MANUAL" ? "badge bg-warning" : "badge bg-info";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${new Date(row.waktu).toLocaleString("id-ID", { timeZone: "Asia/Jakarta" })}</td>
            <td>${row.suhu ?? "-"}</td>
            <td>${row.kelembapan ?? "-"}</td>
            <td><span class="status-badge ${classMap[statusStandard] || "status-off"}">${statusText}</span></td>
            <td><span class="${modeClass}">${mode}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

/* === REFRESH & UPDATE === */
function refresh() {
    fetchLatest();
    loadHistory();
}

async function fetchLatest() {
    try {
        await loadDeviceStatus();   // AMBIL REAL-TIME STATUS
    } catch (err) {
        console.error("fetchLatest error:", err);
        updateConnectionStatus(false);
        updateDeviceStatus(false);
    }
}

/* === DETEKSI KONEKSI === */
setInterval(() => {
    if (lastUpdateTime && new Date() - lastUpdateTime > 15000) {
        const el = document.getElementById("connectionStatus");
        if (el) el.innerHTML = '<i class="fas fa-circle text-warning me-1"></i> Tidak Ada Update';
    }
}, 5000);

/* === AUTO REFRESH TIAP 10 DETIK === */
setInterval(fetchLatest, 10000);

/* === INISIALISASI === */
document.addEventListener("DOMContentLoaded", () => {
    loadDeviceStatus();  // Ambil status device real-time
    loadHistory();       // Ambil data history

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) logoutBtn.addEventListener("click", e => {
        e.preventDefault();
        logout();
    });

    const refreshBtn = document.querySelector(".refresh-btn");
    if (refreshBtn) refreshBtn.addEventListener("click", refresh);
});