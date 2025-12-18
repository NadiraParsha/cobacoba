from rest_framework import serializers
from .models import SensorData, PredictionLog
from django.contrib.auth.models import User

class SensorDataSerializer(serializers.ModelSerializer):
    class Meta:
        model = SensorData
        fields = "__all__"

class PredictionLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = PredictionLog
        fields = "__all__"


class RegisterSerializer(serializers.Serializer):
    username = serializers.CharField(max_length=150)
    password = serializers.CharField(write_only=True)
    email = serializers.EmailField(required=False, allow_blank=True)

    def validate_username(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError("Username sudah digunakan.")
        return value

    def create(self, validated_data):
        user = User(
            username=validated_data["username"],
            email=validated_data.get("email", "")
        )
        user.set_password(validated_data["password"])  # penting: hash password!
        user.save()
        return user
