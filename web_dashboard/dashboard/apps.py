from django.apps import AppConfig
import joblib
from pathlib import Path
import os

class DashboardConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'dashboard'
    model_obj = None

    def ready(self):
        # Load ML model once on app startup
        BASE_DIR = Path(__file__).resolve().parent.parent
        model_path = BASE_DIR / "smartfan_dt_model.pkl"
        if model_path.exists():
            try:
                self.model_obj = joblib.load(model_path)
                print(f"Loaded model from {model_path}")
            except Exception as e:
                print("Error loading model:", e)
        else:
            print("Model file not found at", model_path)

