package com.example.plant_care

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

    // ------------------- НОВОЕ: адрес сервера -------------------
    private val serverBaseUrl = "http://172.20.10.4:5000" // или "http://192.168.1.100:5000"
    private val deviceId = "ESP32_PlantMonitor"                    // должно совпадать с device_id в ESP

    // Для периодического обновления данных (polling)
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val updateInterval = 5000L  // 5 секунд
    // ------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_parametr)

        lightValueTv = findViewById(R.id.lightValue)
        soilValueTv = findViewById(R.id.soilValue)
        tempValueTv = findViewById(R.id.tempValue)
        humidityValueTv = findViewById(R.id.humidityValue)
        pumpButton = findViewById(R.id.button11)

        pumpButton.text = "Загрузка..."
        pumpButton.setOnClickListener { sendTogglePumpRequest() }

        // Запускаем периодическое обновление данных
        startPolling()
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

    private fun stopPolling() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    // ------------------- ЗАПРОС ДАННЫХ С СЕРВЕРА -------------------
    private fun requestSensorData() {
        Thread {
            try {
                val url = URL("$serverBaseUrl/api/device/$deviceId/data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val code = conn.responseCode
                if (code == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    runOnUiThread {
                        updateSensorValues(
                            light = json.optInt("light", 0),
                            soil = json.optInt("soil", 0),
                            temp = json.optDouble("temp", Double.NaN),
                            humidity = json.optDouble("humidity", Double.NaN)
                        )
                        val pumpState = json.optBoolean("pump", false)
                        updatePumpUI(pumpState)
                    }
                } else {
                    Log.e("Polling", "HTTP error: $code")
                    runOnUiThread { showToast("Ошибка получения данных (код $code)") }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Polling", "Error: ${e.message}")
                runOnUiThread { showToast("Ошибка соединения с сервером") }
            }
        }.start()
    }
    // ---------------------------------------------------------------

    // ------------------- ПЕРЕКЛЮЧЕНИЕ НАСОСА ЧЕРЕЗ СЕРВЕР ---------
    private fun sendTogglePumpRequest() {
        Thread {
            try {
                val url = URL("$serverBaseUrl/api/device/$deviceId/pump/toggle")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                // Отправляем пустой JSON (можно и без тела, но для единообразия)
                conn.outputStream.use { os ->
                    os.write("{}".toByteArray())
                    os.flush()
                }

                val code = conn.responseCode
                if (code == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val newState = json.optBoolean("pump", false)
                    runOnUiThread { updatePumpUI(newState) }
                } else {
                    runOnUiThread { showToast("Ошибка переключения насоса (код $code)") }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread { showToast("Ошибка: ${e.message}") }
            }
        }.start()
    }
    // ---------------------------------------------------------------

    private fun updateSensorValues(light: Int, soil: Int, temp: Double, humidity: Double) {
        lightValueTv.text = "$light лк"
        soilValueTv.text = "$soil%"
        tempValueTv.text = if (temp.isNaN()) "--" else "%.1f °C".format(temp)
        humidityValueTv.text = if (humidity.isNaN()) "--" else "%.1f%%".format(humidity)
    }

    private fun updatePumpUI(isOn: Boolean) {
        pumpButton.text = if (isOn) "Выключить насос" else "Включить насос"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                        val message = notif.getString("message")
                        runOnUiThread {
                            Toast.makeText(this@MainActivityParametr, message, Toast.LENGTH_LONG).show()
                        }
                        // Небольшая задержка между тостами, чтобы они не накладывались
                        Thread.sleep(1000)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Notifications", "Error: ${e.message}")
            }
        }.start()
    }
}