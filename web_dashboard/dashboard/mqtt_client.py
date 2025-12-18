import json
import time
import threading
import paho.mqtt.client as mqtt
from .models import DeviceStatus

BROKER = "103.151.63.68"
TOPIC_STATUS = "smartfan/status"
TOPIC_HEARTBEAT = "smartfan/heartbeat"


def on_message(client, userdata, msg):
    payload = json.loads(msg.payload.decode())

    # Listen to STATUS from ESP32
    if msg.topic == TOPIC_STATUS:
        DeviceStatus.objects.update_or_create(
            id=1,
            defaults={
                "fan_status": payload.get("fan"),
                "last_update": time.time(),
                "is_online": True,
            }
        )

    # Listen to HEARTBEAT from ESP32
    if msg.topic == TOPIC_HEARTBEAT:
        DeviceStatus.objects.update_or_create(
            id=1,
            defaults={
                "is_online": True,
                "last_heartbeat": time.time(),
            }
        )


def mqtt_loop():
    client = mqtt.Client()
    client.on_message = on_message
    client.connect(BROKER, 1883, 60)
    client.subscribe("smartfan/#")
    client.loop_forever()


# Start thread
t = threading.Thread(target=mqtt_loop)
t.daemon = True
t.start()
