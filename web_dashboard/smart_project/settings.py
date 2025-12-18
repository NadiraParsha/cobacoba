from pathlib import Path
from datetime import timedelta
import os

# ============================================================
# ======== PATH & BASIC CONFIG ===============================
# ============================================================

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = 'django-insecure-%!h-!yu7!ovugwpkj*shd@yi-lt0us%vb5drr3=l-)vugp4*7%'
DEBUG = True

ALLOWED_HOSTS = ['127.0.0.1', 'localhost']

# ============================================================
# ======== APPS ==============================================
# ============================================================

INSTALLED_APPS = [
    # Django Core
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    

    # 3rd Party
    'rest_framework',
    'rest_framework_simplejwt.token_blacklist',
    'corsheaders',

    # Local Apps
    'dashboard',
]

# ============================================================
# ======== REST FRAMEWORK & JWT CONFIG =======================
# ============================================================

REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': [
        'rest_framework.renderers.JSONRenderer',
        'rest_framework.renderers.BrowsableAPIRenderer',
    ],
    'DEFAULT_PARSER_CLASSES': [
        'rest_framework.parsers.JSONParser',
    ],
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework_simplejwt.authentication.JWTAuthentication',
        'rest_framework.authentication.SessionAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
}

SIMPLE_JWT = {
    'ACCESS_TOKEN_LIFETIME': timedelta(hours=2),
    'REFRESH_TOKEN_LIFETIME': timedelta(days=7),
    'ROTATE_REFRESH_TOKENS': True,
    'BLACKLIST_AFTER_ROTATION': True,
    'AUTH_HEADER_TYPES': ('Bearer',),
}

# ============================================================
# ======== MIDDLEWARE ========================================
# ============================================================

MIDDLEWARE = [
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]

# ============================================================
# ======== URLS & WSGI =======================================
# ============================================================

ROOT_URLCONF = 'smart_project.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],  # bisa isi path custom template jika perlu
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'smart_project.wsgi.application'

# ============================================================
# ======== DATABASE ==========================================
# ============================================================

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.getenv("POSTGRES_DB", "smartfan_db"),
        "USER": os.getenv("POSTGRES_USER", "postgres"),
        "PASSWORD": os.getenv("POSTGRES_PASSWORD", "LIIAA"),
        "HOST": os.getenv("POSTGRES_HOST", "localhost"),
        "PORT": os.getenv("POSTGRES_PORT", "5432"),
    }
}

# ============================================================
# ======== PASSWORD VALIDATION ===============================
# ============================================================

AUTH_PASSWORD_VALIDATORS = [
    {'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'},
    {'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'},
    {'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'},
    {'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'},
]

# ============================================================
# ======== INTERNATIONALIZATION ==============================
# ============================================================

LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'Asia/Jakarta'
USE_I18N = True
USE_TZ = True

# ============================================================
# ======== STATIC FILES ======================================
# ============================================================

STATIC_URL = '/static/'
STATICFILES_DIRS = [
    BASE_DIR / "dashboard" / "static",
]

# ============================================================
# ======== LOGIN/LOGOUT REDIRECT =============================
# ============================================================

LOGIN_URL = '/login/'
LOGIN_REDIRECT_URL = '/'
LOGOUT_REDIRECT_URL = '/login/'

# ============================================================
# ======== CORS & CSRF CONFIG ================================
# ============================================================

CORS_ALLOW_ALL_ORIGINS = True
CORS_ALLOW_CREDENTIALS = True

CSRF_TRUSTED_ORIGINS = [
    "http://127.0.0.1:8000",
    "http://localhost:8000",
]

# Cookie behavior
SESSION_COOKIE_SAMESITE = 'Lax'
SESSION_COOKIE_SECURE = False
CSRF_COOKIE_SAMESITE = 'Lax'
CSRF_COOKIE_SECURE = False

# ============================================================
# ======== DEFAULT AUTO FIELD ================================
# ============================================================

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'
