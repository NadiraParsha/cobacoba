import time
import threading
from .models import DeviceStatus

def check_device_offline():
    while True:
        time.sleep(5)
        try:
            status = DeviceStatus.objects.get(id=1)
            if time.time() - status.last_heartbeat > 10:
                status.is_online = False
                status.save()
        except:
            pass

t = threading.Thread(target=check_device_offline)
t.daemon = True
t.start()
