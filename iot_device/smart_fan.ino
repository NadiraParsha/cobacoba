#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "DHT.h"
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ======= Konfigurasi Sensor & LCD =======
#define DHTPIN 4
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);

LiquidCrystal_I2C lcd(0x27, 16, 2);

// ======= Konfigurasi MOSFET & Suhu =======
#define MOSFET_PIN 5
#define TEMP_ON 25.0     // suhu mulai nyala kipas
#define TEMP_OFF 24.0    // suhu di bawah ini kipas mati

// ======= Konfigurasi WiFi =======
const char* ssid = "tarisa";
const char* password = "12345678";

// ======= Konfigurasi MQTT =======
const char* mqtt_server = "broker.hivemq.com";
const char* mqtt_pub_topic  = "smartfan/sensor";     // ESP32 → Django
const char* mqtt_sub_topic  = "smartfan/control";  // Django → ESP32

WiFiClient espClient;
PubSubClient client(espClient);

// ======= Variabel Global =======
bool fanState = false;          // status kipas sekarang
bool manualOverride = false;    // apakah mode manual aktif
bool manualFanState = false;    // ON/OFF dari Django

// -------------------- SETUP WIFI --------------------
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Menghubungkan ke WiFi: ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);
  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < 30) {
    delay(500);
    Serial.print(".");
    retry++;
  }

  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("✅ WiFi terhubung!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("❌ Gagal koneksi WiFi!");
  }
}

// -------------------- CALLBACK MQTT --------------------
void callback(char* topic, byte* message, unsigned int length) {
  Serial.print("MQTT pesan dari topic: ");
  Serial.println(topic);

  String msg = "";
  for (int i = 0; i < length; i++) {
    msg += (char)message[i];
  }

  Serial.print("Payload: ");
  Serial.println(msg);

  // Parse JSON
  DynamicJsonDocument doc(200);
  deserializeJson(doc, msg);

  String fanCmd = doc["fan"];        // "ON" / "OFF"
  String source = doc["source"];     // "manual" / "auto"

  // ====== MODE MANUAL ======
  if (source == "manual") {
    manualOverride = true;
    manualFanState = (fanCmd == "ON");

    digitalWrite(MOSFET_PIN, manualFanState ? HIGH : LOW);
    fanState = manualFanState;

    Serial.println("📌 MODE MANUAL dari Django");
    return;
  }

  // ====== MODE AUTO ======
  if (source == "auto") {
    manualOverride = false;
    Serial.println("🔄 Override dimatikan → kembali AUTO");
  }
}

// -------------------- RECONNECT MQTT --------------------
void reconnect() {
  while (!client.connected()) {
    Serial.print("🔄 Menghubungkan ke MQTT...");
    if (client.connect("ESP32SmartFanClient")) {
      Serial.println("✅ Terhubung ke broker!");

      client.subscribe(mqtt_sub_topic);
      Serial.print("Subscribe ke: ");
      Serial.println(mqtt_sub_topic);

    } else {
      Serial.print("❌ Gagal, rc=");
      Serial.print(client.state());
      Serial.println(" | Coba lagi dalam 5 detik");
      delay(5000);
    }
  }
}

// -------------------- SETUP --------------------
void setup() {
  Serial.begin(115200);
  dht.begin();
  lcd.init();
  lcd.backlight();

  pinMode(MOSFET_PIN, OUTPUT);
  digitalWrite(MOSFET_PIN, LOW);

  lcd.setCursor(0, 0);
  lcd.print("Smart Fan ESP32");
  lcd.setCursor(0, 1);
  lcd.print("Connecting WiFi");

  setup_wifi();

  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("WiFi Connected");
  delay(1500);
  lcd.clear();
}

// -------------------- LOOP --------------------
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("⚠️ Gagal membaca sensor!");
    lcd.setCursor(0, 0);
    lcd.print("Sensor Error     ");
    delay(1000);
    return;
  }

  // ================= MODE MANUAL =================
  if (manualOverride) {
    digitalWrite(MOSFET_PIN, manualFanState ? HIGH : LOW);
    fanState = manualFanState;
  }

  // ================= MODE AUTO =================
  else {
    if (temperature >= TEMP_ON && !fanState) {
      digitalWrite(MOSFET_PIN, HIGH);
      fanState = true;
      Serial.println("🌀 AUTO: Kipas ON");
    }
    else if (temperature <= TEMP_OFF && fanState) {
      digitalWrite(MOSFET_PIN, LOW);
      fanState = false;
      Serial.println("🛑 AUTO: Kipas OFF");
    }
  }

  // ===== LCD =====
  lcd.setCursor(0, 0);
  lcd.print("Temp:");
  lcd.print(temperature, 1);
  lcd.print((char)223);
  lcd.print("C   ");

  lcd.setCursor(0, 1);
  lcd.print("Hum:");
  lcd.print(humidity, 1);
  lcd.print("% ");
  lcd.print(fanState ? "ON " : "OFF");

  // ===== Publish Data Ke MQTT =====
  DynamicJsonDocument doc(200);
  doc["suhu"] = temperature;
  doc["kelembapan"] = humidity;

  String jsonString;
  serializeJson(doc, jsonString);

  client.publish(mqtt_pub_topic, jsonString.c_str());
  Serial.print("📡 Publish: ");
  Serial.println(jsonString);

  delay(5000);
}
