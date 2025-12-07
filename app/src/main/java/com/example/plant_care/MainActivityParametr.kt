package com.example.plant_care

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.IOException

class MainActivityParametr : AppCompatActivity() {
    private var sensorDataServer: SensorDataServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_parametr)

        // Запускаем сервер для приема данных
        startSensorServer()
    }

    private fun startSensorServer() {
        sensorDataServer = SensorDataServer(8080)
        try {
            sensorDataServer?.start()
            //showToast("Сервер запущен на порту 8080")
            Log.d("SensorServer", "Сервер запущен")
        } catch (e: IOException) {
           // showToast("Ошибка запуска сервера")
            Log.e("SensorServer", "Ошибка запуска", e)
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
                val sensors = json.getJSONObject("sensors")

                // Извлечение значений
                val light = sensors.getInt("light")
                val soil = sensors.getInt("soil")
                val temp = sensors.getDouble("temp")
                val humidity = sensors.getDouble("humidity")

                // Вывод в консоль (Logcat)
                Log.i("SENSOR_VALUES", "===================================")
                Log.i("SENSOR_VALUES", "Освещенность: $light лк")
                Log.i("SENSOR_VALUES", "Влажность почвы: $soil%")
                Log.i("SENSOR_VALUES", "Температура: $temp °C")
                Log.i("SENSOR_VALUES", "Влажность воздуха: $humidity%")
                Log.i("SENSOR_VALUES", "===================================")

                // Обновление UI
                runOnUiThread {
                    updateSensorValues(light, soil, temp, humidity)
                }

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
        findViewById<TextView>(R.id.lightValue).text = "$light лк"
        findViewById<TextView>(R.id.soilValue).text = "$soil%"
        findViewById<TextView>(R.id.tempValue).text = "%.1f °C".format(temp)
        findViewById<TextView>(R.id.humidityValue).text = "%.1f%%".format(humidity)

        Log.d("UI", "Updated values: light=$light, soil=$soil, temp=$temp, humidity=$humidity")
    }
}