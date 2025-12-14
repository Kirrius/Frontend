package com.example.plant_care

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivityParametr : AppCompatActivity() {
    private var sensorDataServer: SensorDataServer? = null

    // UI элементы (соответствуют вашему XML)
    private lateinit var lightValueTv: TextView
    private lateinit var soilValueTv: TextView
    private lateinit var tempValueTv: TextView
    private lateinit var humidityValueTv: TextView
    private lateinit var pumpButton: Button

    // Хост ESP по умолчанию (используйте IP если mDNS не доступен)
    private var espHost = "http://esp32.local" // замените на "http://192.168.x.y" при необходимости

    // Текущее известное состояние насоса (null = неизвестно)
    private var pumpStateKnown: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_parametr)

        // Находим UI по id из вашего XML
        lightValueTv = findViewById(R.id.lightValue)
        soilValueTv = findViewById(R.id.soilValue)
        tempValueTv = findViewById(R.id.tempValue)
        humidityValueTv = findViewById(R.id.humidityValue)
        pumpButton = findViewById(R.id.button11)

        // Инициализация текста кнопки
        pumpButton.text = "Загрузка..."

        // Клик по кнопке: переключаем насос
        pumpButton.setOnClickListener {
            // отправляем команду toggle на ESP
            sendTogglePumpRequest(espHost)
        }

        // Запускаем сервер для приёма данных от ESP
        startSensorServer()

        // Попытка получить текущее состояние насоса при старте
        requestPumpStatus(espHost)
    }

    private fun startSensorServer() {
        sensorDataServer = SensorDataServer(8080)
        try {
            sensorDataServer?.start()
            Log.d("SensorServer", "Сервер запущен")
        } catch (e: IOException) {
            Log.e("SensorServer", "Ошибка запуска", e)
            showToast("Ошибка запуска локального сервера")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorDataServer?.stop()
        Log.d("SensorServer", "Сервер остановлен")
    }

    inner class SensorDataServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            return when (session.method) {
                Method.POST -> handlePostRequest(session)
                else -> newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    "text/plain",
                    "Only POST method is allowed"
                )
            }
        }

        private fun handlePostRequest(session: IHTTPSession): Response {
            return try {
                // Чтение данных запроса
                val files = mutableMapOf<String, String>()
                session.parseBody(files)
                val postData = files["postData"] ?: return errorResponse("No data received")

                Log.d("SensorData", "Received: $postData")

                // Парсинг JSON
                val json = JSONObject(postData)
                val deviceId = json.optString("device", "unknown")
                val sensors = json.optJSONObject("sensors") ?: JSONObject()

                // Извлечение значений (без падения, если чего-то нет)
                val light = sensors.optInt("light", 0)
                val soil = sensors.optInt("soil", 0)
                val temp = sensors.optDouble("temp", Double.NaN)
                val humidity = sensors.optDouble("humidity", Double.NaN)
                val pump = sensors.optBoolean("pump", false) // <- если ESP присылает

                Log.i("SENSOR_VALUES", "===================================")
                Log.i("SENSOR_VALUES", "Device: $deviceId")
                Log.i("SENSOR_VALUES", "Освещенность: $light лк")
                Log.i("SENSOR_VALUES", "Влажность почвы: $soil%")
                Log.i("SENSOR_VALUES", "Температура: $temp °C")
                Log.i("SENSOR_VALUES", "Влажность воздуха: $humidity%")
                Log.i("SENSOR_VALUES", "Насос (from ESP): ${if (pump) "ON" else "OFF"}")
                Log.i("SENSOR_VALUES", "===================================")

                // Обновление UI
                runOnUiThread {
                    updateSensorValues(light, soil, temp, humidity)
                    // если в приходящем JSON есть поле pump — обновляем состояние кнопки
                    updatePumpUI(pump)
                }

                // Опционально: зафиксировать IP ESP как последний отправитель (если понадобится)
                // Здесь мы не извлекаем IP из session напрямую, чтобы не зависеть от версии NanoHTTPD.

                // Успешный ответ
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"status": "success", "device": "$deviceId"}"""
                )
            } catch (e: Exception) {
                Log.e("SensorData", "Error processing request", e)
                errorResponse(e.message ?: "Unknown error")
            }
        }

        private fun errorResponse(message: String): Response {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                """{"error": "$message"}"""
            )
        }
    }

    private fun updateSensorValues(light: Int, soil: Int, temp: Double, humidity: Double) {
        lightValueTv.text = "$light лк"
        soilValueTv.text = "$soil%"
        tempValueTv.text = if (temp.isNaN()) "--" else "%.1f °C".format(temp)
        humidityValueTv.text = if (humidity.isNaN()) "--" else "%.1f%%".format(humidity)

        Log.d("UI", "Updated values: light=$light, soil=$soil, temp=$temp, humidity=$humidity")
    }

    // Обновление UI кнопки по состоянию насоса
    private fun updatePumpUI(isOn: Boolean) {
        pumpStateKnown = isOn
        pumpButton.text = if (isOn) "Выключить насос" else "Включить насос"
    }

    // Отправка POST запроса на ESP /togglePump
    private fun sendTogglePumpRequest(host: String) {
        Thread {
            try {
                val url = URL("$host/togglePump")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Content-Type", "application/json")
                }

                // отправим пустой JSON — ESP не требует тела
                conn.outputStream.use { os ->
                    os.write("{}".toByteArray())
                    os.flush()
                }

                val code = conn.responseCode
                val resp = try {
                    InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream).use { it.readText() }
                } catch (e: Exception) {
                    ""
                }

                Log.d("PumpToggle", "HTTP $code, resp: $resp")

                // Попытка распарсить ответ JSON {"pump":true/false}
                var newState: Boolean? = null
                try {
                    if (resp.isNotBlank()) {
                        val j = JSONObject(resp)
                        if (j.has("pump")) newState = j.getBoolean("pump")
                    }
                } catch (e: Exception) {
                    Log.w("PumpToggle", "Не удалось распарсить ответ в JSON", e)
                }

                runOnUiThread {
                    if (code in 200..299) {
                        if (newState != null) updatePumpUI(newState)
                        else {
                            // если состояние не вернули — инвертируем локально (предположение)
                            pumpStateKnown = pumpStateKnown?.not() ?: null
                            pumpStateKnown?.let { updatePumpUI(it) }
                        }
                    } else {
                        showToast("Ошибка управления насосом (код $code)")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("PumpToggle", "Error toggling pump", e)
                runOnUiThread {
                    showToast("Ошибка отправки команды: ${e.message}")
                }
            }
        }.start()
    }

    // Получить статус насоса с ESP (/pumpStatus)
    private fun requestPumpStatus(host: String) {
        Thread {
            try {
                val url = URL("$host/pumpStatus")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }

                val code = conn.responseCode
                val resp = try {
                    InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream).use { it.readText() }
                } catch (e: Exception) {
                    ""
                }

                Log.d("PumpStatus", "HTTP $code, resp: $resp")

                if (code in 200..299 && resp.isNotBlank()) {
                    try {
                        val j = JSONObject(resp)
                        val pump = j.optBoolean("pump", false)
                        runOnUiThread { updatePumpUI(pump) }
                    } catch (e: Exception) {
                        Log.w("PumpStatus", "Не удалось распарсить JSON", e)
                    }
                } else {
                    Log.w("PumpStatus", "Non-OK response $code")
                }

                conn.disconnect()
            } catch (e: Exception) {
                Log.e("PumpStatus", "Error requesting pump status", e)
            }
        }.start()
    }
}
