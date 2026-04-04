package com.example.plant_care

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivityParametr : AppCompatActivity() {

    private lateinit var sensorsRecyclerView: RecyclerView
    private lateinit var adapter: SensorsAdapter
    private val sensorsMap = mutableMapOf<String, Any>()
    private lateinit var pumpButton: Button
    private lateinit var timerText: TextView
    private lateinit var progressBar: ProgressBar

    private val serverBaseUrl = "https://plant-care.up.railway.app"
    private val deviceId = "ESP32_PlantMonitor"

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val updateInterval = 5000L

    private var notificationRunnable: Runnable? = null
    private val notificationInterval = 60000L

    private var isRequestInProgress = false
    private var lastOffTime = 0L
    private val cooldownSeconds = 10
    private var countdownSeconds = 0
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_parametr)

        sensorsRecyclerView = findViewById(R.id.sensorsRecyclerView)
        pumpButton = findViewById(R.id.button11)
        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)

        sensorsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SensorsAdapter(sensorsMap)
        sensorsRecyclerView.adapter = adapter

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

                val code = conn.responseCode
                if (code == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    // Получаем все датчики из поля sensors
                    val sensorsJson = json.optJSONObject("sensors")
                    val pumpState = json.optBoolean("pump", false)

                    runOnUiThread {
                        try {
                            sensorsMap.clear()
                            if (sensorsJson != null) {
                                val keys = sensorsJson.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val value = sensorsJson.get(key)
                                    if (key != "pump") {
                                        sensorsMap[key] = value
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged()
                            updatePumpUI(pumpState)
                            progressBar.visibility = View.GONE
                        } catch (e: Exception) {
                            Log.e("UI", "Update error: ${e.message}", e)
                            Toast.makeText(this@MainActivityParametr, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("Polling", "HTTP error: $code")
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivityParametr, "Ошибка HTTP: $code", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Polling", "Error: ${e.message}", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivityParametr, "Ошибка соединения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun canTurnOn(): Boolean {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastOffTime) / 1000
        return lastOffTime == 0L || elapsedSeconds >= cooldownSeconds
    }

    private fun startCountdown() {
        stopCountdown()
        countdownSeconds = cooldownSeconds
        timerText.visibility = View.VISIBLE
        timerText.text = "Подождите ${countdownSeconds} сек..."

        countdownRunnable = object : Runnable {
            override fun run() {
                countdownSeconds--
                if (countdownSeconds > 0) {
                    timerText.text = "Подождите ${countdownSeconds} сек..."
                    handler.postDelayed(this, 1000)
                } else {
                    timerText.visibility = View.GONE
                    countdownRunnable = null
                }
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        timerText.visibility = View.GONE
    }

    private fun sendTogglePumpRequest() {
        if (isRequestInProgress) {
            Toast.makeText(this, "Подождите, команда выполняется...", Toast.LENGTH_SHORT).show()
            return
        }

        val isPumpOn = pumpButton.text.toString() == "Выключить насос"

        // Если насос выключен - проверяем задержку
        if (!isPumpOn && !canTurnOn()) {
            val remaining = cooldownSeconds - ((System.currentTimeMillis() - lastOffTime) / 1000).toInt()
            if (remaining > 0) {
                timerText.visibility = View.VISIBLE
                timerText.text = "Подождите $remaining сек..."
            }
            return
        }

        isRequestInProgress = true
        pumpButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

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
                        if (!newState) {
                            lastOffTime = System.currentTimeMillis()
                            startCountdown()
                        }
                        Toast.makeText(
                            this@MainActivityParametr,
                            if (newState) "Насос включен" else "Насос выключен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (code == 429) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val waitTime = json.optInt("cooldown_remaining", cooldownSeconds)
                    runOnUiThread {
                        timerText.visibility = View.VISIBLE
                        timerText.text = "Подождите $waitTime сек..."
                        startCountdown()
                    }
                } else {
                    Log.e("Pump", "Error code: $code")
                    runOnUiThread {
                        Toast.makeText(this@MainActivityParametr, "Ошибка: код $code", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Pump", "Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivityParametr, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                runOnUiThread {
                    isRequestInProgress = false
                    pumpButton.isEnabled = true
                    progressBar.visibility = View.GONE
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
                            } else {
                                Toast.makeText(this@MainActivityParametr, message, Toast.LENGTH_LONG).show()
                            }
                        }
                        Thread.sleep(1000)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("Notifications", "Error: ${e.message}", e)
            }
        }.start()
    }

    private fun updatePumpUI(isOn: Boolean) {
        pumpButton.text = if (isOn) "Выключить насос" else "Включить насос"
    }
}