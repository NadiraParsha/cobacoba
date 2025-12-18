from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from django.contrib.auth.models import User
from django.contrib import messages
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator
from django.apps import apps

from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework import status, serializers
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework import generics

import numpy as np
import json
import time

from .models import SensorData, PredictionLog, DeviceStatus, FanControl
from .serializers import SensorDataSerializer, PredictionLogSerializer, RegisterSerializer

import paho.mqtt.publish as publish

from django.utils import timezone
from datetime import timedelta


# ============================================================
# ================= REGISTER SERIALIZER ======================
# ============================================================
class RegisterSerializer(serializers.Serializer):
    username = serializers.CharField()
    password = serializers.CharField(write_only=True)
    email = serializers.EmailField(required=False)

    def validate_username(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError("Username sudah digunakan.")
        return value

    def validate_password(self, value):
        if len(value) < 6:
            raise serializers.ValidationError("Password minimal 6 karakter.")
        return value


# ============================================================
# ================= REGISTER API VIEW ========================
# ============================================================
class RegisterAPIView(APIView):
    permission_classes = [AllowAny]

    def post(self, request, *args, **kwargs):
        username = request.data.get("username")
        email = request.data.get("email")
        password = request.data.get("password")

        if not username or not password:
            return Response(
                {"detail": "Username dan password wajib diisi"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if User.objects.filter(username=username).exists():
            return Response(
                {"detail": "Username sudah digunakan"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            user = User.objects.create_user(
                username=username,
                email=email,
                password=password
            )
            user.save()

        except Exception as e:
            return Response(
                {"detail": f"Gagal membuat user: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        return Response(
            {"detail": f"Registrasi berhasil untuk user '{username}'"},
            status=status.HTTP_201_CREATED,
        )


# ============================================================
# ================= LOGOUT API ===============================
# ============================================================
@method_decorator(csrf_exempt, name='dispatch')
class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data.get("refresh")
            if not refresh_token:
                return Response({"detail": "Refresh token wajib dikirim."},
                                status=status.HTTP_400_BAD_REQUEST)

            token = RefreshToken(refresh_token)
            token.blacklist()
            return Response({"detail": "Logout berhasil."},
                            status=status.HTTP_205_RESET_CONTENT)
        except Exception:
            return Response({"detail": "Token tidak valid atau sudah logout."},
                            status=status.HTTP_400_BAD_REQUEST)


# ============================================================
# ========= LOGIN PAGE & DASHBOARD PAGE ======================
# ============================================================
def login_page(request):
    return render(request, "dashboard/login.html")


def dashboard_page(request):
    appcfg = apps.get_app_config('dashboard')
    model = getattr(appcfg, "model_obj", None)
    status_ai = "AKTIF" if model else "NONAKTIF"
    return render(request, "dashboard/dashboard.html", {"status_ai": status_ai})


# ============================================================
# ================= SENSOR API (AI CONTROL) ==================
# ============================================================
@api_view(['POST'])
@permission_classes([AllowAny])
def sensor_data_create(request):
    """
    AI hanya boleh mengontrol jika manual_override = False.
    Kalau manual override aktif, AI diblok total.
    """

    try:
        suhu = request.data.get("suhu")
        kelembapan = request.data.get("kelembapan")

        if suhu is None or kelembapan is None:
            return Response({"error": "suhu dan kelembapan wajib diisi."},
                            status=status.HTTP_400_BAD_REQUEST)

        suhu = float(suhu)
        kelembapan = float(kelembapan)

        # Ambil pengaturan kipas
        control = FanControl.objects.first()
        if not control:
            control = FanControl.objects.create(fan_status="OFF", manual_override=False)

        # ======================================================
        # 💥 BLOK AI JIKA SEDANG MANUAL OVERRIDE
        # ======================================================
        if control.manual_override:
            sd = SensorData.objects.create(
                suhu=suhu,
                kelembapan=kelembapan,
                status_kipas=control.fan_status
            )

            dev = DeviceStatus.objects.first()
            if dev:
                dev.is_online = True
                dev.last_update = time.time()
                dev.last_heartbeat = time.time()
                dev.save()

            return Response({
                "sensor": SensorDataSerializer(sd).data,
                "prediction": {
                    "hasil": "MANUAL_OVERRIDE",
                    "rekomendasi": "Mode manual aktif — AI tidak mengontrol",
                    "level": control.fan_status
                }
            }, status=status.HTTP_201_CREATED)

        # ======================================================
        # ==================== AUTO MODE =======================
        # ======================================================
        sd = SensorData.objects.create(
            suhu=suhu,
            kelembapan=kelembapan,
            status_kipas="UNKNOWN"
        )

        appcfg = apps.get_app_config('dashboard')
        model = getattr(appcfg, "model_obj", None)

        hasil = ""
        rekom = ""
        level = "OFF"

        # ==== AI PREDICT ====
        if model:
            try:
                pred = model.predict(np.array([[suhu, kelembapan]]))[0]
                hasil = str(pred).lower()

                if hasil in ("high", "panas", "bahaya"):
                    level = "HIGH"
                    rekom = "Suhu tinggi — kipas HIGH"
                elif hasil in ("medium", "sedang", "hangat"):
                    level = "MEDIUM"
                    rekom = "Suhu hangat — kipas MEDIUM"
                elif hasil in ("low", "sejuk"):
                    level = "LOW"
                    rekom = "Suhu sejuk — kipas LOW"
                else:
                    level = "OFF"
                    rekom = "Suhu dingin — kipas OFF"

            except:
                hasil = "ai_error"
                level = "OFF"
                rekom = "AI error — fallback"

        else:
            # Fallback rule
            if suhu > 32:
                level = "HIGH"; hasil = "rule_high"; rekom = "Suhu panas — HIGH"
            elif suhu >= 28:
                level = "MEDIUM"; hasil = "rule_medium"; rekom = "Suhu hangat — MEDIUM"
            elif suhu >= 25:
                level = "LOW"; hasil = "rule_low"; rekom = "Suhu sejuk — LOW"
            else:
                level = "OFF"; hasil = "rule_off"; rekom = "Suhu dingin — OFF"

        # Simpan status AI
        sd.status_kipas = level
        sd.save()

        # MQTT kirim
        publish.single(
            "smartfan/control",
            json.dumps({"fan": level, "source": "ai"}),
            hostname="broker.hivemq.com"
        )

        # Update device status
        dev = DeviceStatus.objects.first()
        if not dev:
            dev = DeviceStatus.objects.create()
        dev.is_online = True
        dev.last_update = time.time()
        dev.last_heartbeat = time.time()
        dev.fan_status = level
        dev.save()

        # Update FanControl
        control.fan_status = level
        control.save()

        # Log
        PredictionLog.objects.create(
            suhu=suhu,
            kelembapan=kelembapan,
            hasil_ai=hasil.upper(),
            rekomendasi=rekom
        )

        return Response({
            "sensor": SensorDataSerializer(sd).data,
            "prediction": {
                "hasil": hasil.upper(),
                "rekomendasi": rekom,
                "level": level
            }
        }, status=status.HTTP_201_CREATED)

    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


# ============================================================
# ======================= SENSOR GET =========================
# ============================================================
@api_view(['GET'])
@permission_classes([AllowAny])
def sensor_latest(request):
    latest = SensorData.objects.last()
    if not latest:
        return Response({})
    return Response(SensorDataSerializer(latest).data)


@api_view(['GET'])
@permission_classes([AllowAny])
def sensor_history(request):
    limit = int(request.GET.get('limit', 200))
    qs = SensorData.objects.order_by('-waktu')[:limit]
    return Response(list(reversed(SensorDataSerializer(qs, many=True).data)))


# ============================================================
# =================== MANUAL CONTROL =========================
# ============================================================
@api_view(['POST'])
@permission_classes([AllowAny])
def control_fan(request):
    status_kipas = request.data.get("status")  # "ON" / "OFF"

    if status_kipas not in ["ON", "OFF"]:
        return Response({"error": "status harus ON / OFF"}, status=400)

    # MQTT
    publish.single(
        "smartfan/control",
        json.dumps({"fan": status_kipas, "source": "manual"}),
        hostname="broker.hivemq.com"
    )

    # Update FanControl
    control = FanControl.objects.first()
    if not control:
        control = FanControl.objects.create()

    control.fan_status = status_kipas
    control.manual_override = True
    control.save()

    # Update device
    dev = DeviceStatus.objects.first()
    if not dev:
        dev = DeviceStatus.objects.create(
            fan_status=status_kipas,
            is_online=True
        )
    else:
        dev.fan_status = status_kipas
        dev.is_online = True
        dev.save()

    # Buat history supaya dashboard tampil
    SensorData.objects.create(
        suhu=0.0,
        kelembapan=0.0,
        status_kipas=status_kipas
    )

    return Response({
        "message": "Perintah manual dikirim",
        "status": status_kipas
    })


# ============================================================
# ======================== AI CONTROL =========================
# ============================================================
@api_view(['POST'])
@permission_classes([AllowAny])
def ai_control(request):
    fan_cmd = request.data.get("fan")  # "ON" / "OFF"
    dev = DeviceStatus.objects.first()
    control = FanControl.objects.first()

    if not control:
        control = FanControl.objects.create(fan_status="OFF", manual_override=False)

    # Blok AI jika manual override
    if control.manual_override:
        return Response({
            "message": "AI blocked — manual override aktif.",
            "fan_status": control.fan_status
        }, status=403)

    publish.single(
        "smartfan/control",
        json.dumps({"fan": fan_cmd, "source": "ai"}),
        hostname="broker.hivemq.com"
    )

    control.fan_status = fan_cmd
    control.save()

    # Update device
    if dev:
        dev.fan_status = fan_cmd
        dev.save()

    return Response({
        "message": "AI updated fan status",
        "fan_status": fan_cmd
    }, status=200)


# ============================================================
# ================== DEVICE STATUS FOR DASHBOARD =============
# ============================================================
@csrf_exempt
@api_view(['GET'])
@permission_classes([AllowAny])
def device_status(request):
    dev = DeviceStatus.objects.first()
    last_sensor = SensorData.objects.last()
    control = FanControl.objects.first()

    is_online = False
    if last_sensor and (timezone.now() - last_sensor.waktu < timedelta(seconds=30)):
        is_online = True

    return Response({
        "device_online": is_online,
        "fan_status": control.fan_status if control else "unknown",
        "manual_override": control.manual_override if control else False,
        "last_sensor": {
            "suhu": last_sensor.suhu,
            "kelembapan": last_sensor.kelembapan,
            "status_kipas": last_sensor.status_kipas,
            "waktu": last_sensor.waktu
        } if last_sensor else None
    })


# ============================================================
# ==================== CHECK SESSION =========================
# ============================================================
@login_required
def check_session(request):
    return JsonResponse({"status": "authenticated"})


# ============================================================
# ================= REGISTER PAGE (HTML) =====================
# ============================================================
@csrf_exempt
def register_page(request):
    if request.method == "POST":
        username = request.POST.get("username")
        password = request.POST.get("password")
        confirm_password = request.POST.get("confirm_password")

        if not username or not password:
            messages.error(request, "Username dan password wajib diisi.")
            return render(request, "dashboard/register.html")

        if password != confirm_password:
            messages.error(request, "Password tidak cocok.")
            return render(request, "dashboard/register.html")

        if User.objects.filter(username=username).exists():
            messages.error(request, "Username sudah digunakan.")
            return render(request, "dashboard/register.html")

        User.objects.create_user(username=username, password=password, is_active=True)
        messages.success(request, "Registrasi berhasil! Silakan login.")
        return redirect("login_page")

    return render(request, "dashboard/register.html")


# ============================================================
# ================ SET KE MODE AUTO (AI ON) ==================
# ============================================================
@api_view(['POST'])
@permission_classes([AllowAny])
def fan_set_auto(request):
    control = FanControl.objects.first()
    if not control:
        control = FanControl.objects.create(
            fan_status="OFF",
            manual_override=False
        )

    control.manual_override = False
    control.save()

    dev = DeviceStatus.objects.first()
    if not dev:
        dev = DeviceStatus.objects.create(
            fan_status="OFF",
            is_online=True
        )
    else:
        dev.fan_status = control.fan_status
        dev.is_online = True
        dev.save()

    return Response({
        "message": "Mode otomatis aktif. AI mengambil alih kembali.",
        "manual_override": False,
        "current_fan_status": control.fan_status
    })
