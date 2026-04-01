package com.example.plant_care

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivityParametr : AppCompatActivity() {

    private lateinit var lightValueTv: TextView
    private lateinit var soilValueTv: TextView
    private lateinit var tempValueTv: TextView
    private lateinit var humidityValueTv: TextView
    private lateinit var pumpButton: Button
    private lateinit var timerText: TextView

    private val serverBaseUrl = "https://plant-care.up.railway.app"
    private val deviceId = "ESP32_PlantMonitor"

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val updateInterval = 5000L

    private var notificationRunnable: Runnable? = null
    private val notificationInterval = 60000L

    private var isRequestInProgress = false
    private var lastOffTime = 0L           // время последнего выключения (мс)
    private val cooldownSeconds = 10        // задержка после выключения 10 секунд
    private var countdownSeconds = 0
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_parametr)

        lightValueTv = findViewById(R.id.lightValue)
        soilValueTv = findViewById(R.id.soilValue)
        tempValueTv = findViewById(R.id.tempValue)
        humidityValueTv = findViewById(R.id.humidityValue)
        pumpButton = findViewById(R.id.button11)
        timerText = findViewById(R.id.timerText)

        pumpButton.text = "Загрузка..."
        pumpButton.setOnClickListener { sendTogglePumpRequest() }

        NotificationHelper.createNotificationChannel(this)
        startPolling()
        startNotificationPolling()
    }

    private fun startPolling() {
        updateRunnable = object : Runnable {
            override fun run() {
                requestSensorData()
                handler.postDelayed(this, updateInterval)
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun startNotificationPolling() {
        notificationRunnable = object : Runnable {
            override fun run() {
                checkNotifications()
                handler.postDelayed(this, notificationInterval)
            }
        }
        handler.post(notificationRunnable!!)
    }

    private fun stopPolling() {
        handler.removeCallbacksAndMessages(null)
        stopCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    private fun requestSensorData() {
        Thread {
            try {
                val url = URL("$serverBaseUrl/api/device/$deviceId/data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val pumpState = json.optBoolean("pump", false)
                    runOnUiThread {
                        updateSensorValues(
                            light = json.optInt("light", 0),
                            soil = json.optInt("soil", 0),
                            temp = json.optDouble("temp", Double.NaN),
                            humidity = json.optDouble("humidity", Double.NaN)
                        )
                        updatePumpUI(pumpState)
                    }
                } else {
                    Log.e("Polling", "HTTP error: ${conn.responseCode}")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Polling", "Error: ${e.message}")
            }
        }.start()
    }

    // Проверка: можно ли включать насос? (прошло ли 10 секунд с последнего выключения)
    private fun canTurnOn(): Boolean {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastOffTime) / 1000
        return lastOffTime == 0L || elapsedSeconds >= cooldownSeconds
    }

    private fun startCountdown() {
        stopCountdown()
        countdownSeconds = cooldownSeconds
        timerText.visibility = android.view.View.VISIBLE
        timerText.text = "Подождите ${countdownSeconds} сек..."

        countdownRunnable = object : Runnable {
            override fun run() {
                countdownSeconds--
                if (countdownSeconds > 0) {
                    timerText.text = "Подождите ${countdownSeconds} сек..."
                    handler.postDelayed(this, 1000)
                } else {
                    timerText.visibility = android.view.View.GONE
                    countdownRunnable = null
                }
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        timerText.visibility = android.view.View.GONE
    }

    private fun sendTogglePumpRequest() {
        if (isRequestInProgress) return

        // Определяем текущее состояние: насос включён, если текст кнопки "Выключить насос"
        val isPumpOn = pumpButton.text.toString() == "Выключить насос"

        // Если насос выключен (текст "Включить насос") – это действие "включить"
        if (!isPumpOn) {
            // Проверяем задержку после последнего выключения
            if (!canTurnOn()) {
                // Если задержка активна – показываем оставшееся время и выходим
                val remaining = cooldownSeconds - ((System.currentTimeMillis() - lastOffTime) / 1000).toInt()
                if (remaining > 0) {
                    timerText.visibility = android.view.View.VISIBLE
                    timerText.text = "Подождите $remaining сек..."
                }
                return
            }
        }

        isRequestInProgress = true
        pumpButton.isEnabled = false

        Thread {
            try {
                val url = URL("$serverBaseUrl/api/device/$deviceId/pump/toggle")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                conn.outputStream.use { os ->
                    os.write("{}".toByteArray())
                    os.flush()
                }

                val code = conn.responseCode
                if (code == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val newState = json.optBoolean("pump", false)

                    runOnUiThread {
                        updatePumpUI(newState)

                        // Если насос только что выключили – запоминаем время и запускаем таймер
                        if (!newState) {
                            lastOffTime = System.currentTimeMillis()
                            startCountdown()
                        } else {
                            // Если включили – ничего не делаем с таймером
                            // (можно сбросить, если нужно, но обычно после включения задержка не нужна)
                        }
                    }
                } else if (code == 429) {
                    // Сервер вернул "слишком много запросов" – показываем задержку
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val waitTime = json.optInt("cooldown_remaining", cooldownSeconds)
                    runOnUiThread {
                        timerText.visibility = android.view.View.VISIBLE
                        timerText.text = "Подождите $waitTime сек..."
                        startCountdown()
                    }
                } else {
                    Log.e("Pump", "Error code: $code")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Pump", "Error: ${e.message}")
            } finally {
                runOnUiThread {
                    isRequestInProgress = false
                    pumpButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun checkNotifications() {
        Thread {
            try {
                val url = URL("$serverBaseUrl/api/device/$deviceId/notifications")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val notifArray = json.getJSONArray("notifications")
                    for (i in 0 until notifArray.length()) {
                        val notif = notifArray.getJSONObject(i)
                        val title = notif.optString("title", "Уведомление")
                        val message = notif.getString("message")
                        val level = notif.optString("level", "info")
                        runOnUiThread {
                            if (level == "critical") {
                                NotificationHelper.sendNotification(this@MainActivityParametr, title, message)
                            }
                        }
                        Thread.sleep(1000)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Notifications", "Error: ${e.message}")
            }
        }.start()
    }

    private fun updateSensorValues(light: Int, soil: Int, temp: Double, humidity: Double) {
        lightValueTv.text = "$light лк"
        soilValueTv.text = "$soil%"
        tempValueTv.text = if (temp.isNaN()) "--" else "%.1f °C".format(temp)
        humidityValueTv.text = if (humidity.isNaN()) "--" else "%.1f%%".format(humidity)
    }

    private fun updatePumpUI(isOn: Boolean) {
        pumpButton.text = if (isOn) "Выключить насос" else "Включить насос"
    }
}