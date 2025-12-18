from django.db import models
from django.utils import timezone
import time


# ===========================================================
# ===================== SENSOR DATA =========================
# ===========================================================
class SensorData(models.Model):
    suhu = models.FloatField()
    kelembapan = models.FloatField()
    status_kipas = models.CharField(max_length=20, blank=True)
    waktu = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.waktu} - {self.suhu}°C / {self.kelembapan}%"


# ===========================================================
# ===================== PREDICTION LOG =======================
# ===========================================================
class PredictionLog(models.Model):
    suhu = models.FloatField()
    kelembapan = models.FloatField()
    hasil_ai = models.CharField(max_length=50)
    rekomendasi = models.CharField(max_length=100, blank=True)
    waktu = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.waktu} - {self.hasil_ai}"


# ===========================================================
# =============== FAN CONTROL (MANUAL OVERRIDE) ==============
# ===========================================================
class FanControl(models.Model):
    fan_status = models.CharField(max_length=10, default="OFF")   # OFF / ON
    last_update = models.DateTimeField(auto_now=True)

    # TRUE = user override AI → AI tidak boleh kontrol
    manual_override = models.BooleanField(default=False)

    def __str__(self):
        return f"Fan={self.fan_status}, Override={self.manual_override}"


# ===========================================================
# ===================== DEVICE STATUS ========================
# ===========================================================
class DeviceStatus(models.Model):
    # Apakah alat sedang online (heartbeat < 30s)
    is_online = models.BooleanField(default=False)

    # Waktu terakhir alat mengirim sensor data (timezone.now)
    last_seen = models.DateTimeField(auto_now=True)

    # Simpan status terakhir perangkat (HIGH / MEDIUM / LOW / OFF)
    fan_status = models.CharField(max_length=10, default="OFF")

    def __str__(self):
        return f"Online={self.is_online}, Fan={self.fan_status}, LastSeen={self.last_seen}"
