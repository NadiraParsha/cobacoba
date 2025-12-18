from django.urls import path
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from . import views
from .views import (
    control_fan,
    sensor_data_create,
    sensor_latest,
    sensor_history,
    RegisterAPIView,
    LogoutView,
    device_status,
    ai_control,
    fan_set_auto
)

urlpatterns = [
    # ====== WEB PAGES ======
    path("", views.dashboard_page, name="dashboard_page"),
    path("login/", views.login_page, name="login_page"),
    path("register/", views.register_page, name="register_page"),

    # ====== AUTH API (JSON) ======
    path("api/register/", RegisterAPIView.as_view(), name="register_api"),
    path("api/logout/", LogoutView.as_view(), name="logout_api"),
    path("api/token/", TokenObtainPairView.as_view(), name="token_obtain_pair"),
    path("api/token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),

    # ====== SENSOR API ======
    path("api/sensor/create/", sensor_data_create, name="sensor_create"),
    path("api/sensor/latest/", sensor_latest, name="sensor_latest"),
    path("api/sensor/history/", sensor_history, name="sensor_history"),

    # ====== FAN CONTROL ======
    path("api/fan/control/", control_fan, name="control_fan"),      # manual ON/OFF
    path("api/fan/ai/", ai_control, name="ai_fan_control"),         # AI override
    path("api/fan/set-auto/", fan_set_auto, name="fan_set_auto"),   # disable manual override

    # ====== DEVICE STATUS ======
    path("api/device/status/", device_status, name="device_status"),

    # ====== SESSION CHECK ======
    path("api/auth/check/", views.check_session, name="check_session"),
]
