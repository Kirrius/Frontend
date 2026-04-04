package com.example.plant_care

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.example.plant_care.UserScenario
import com.example.plant_care.SensorData

class SensorCheckService : IntentService("SensorCheckService") {

    private lateinit var sharedPref: SharedPreferences
    private val baseUrl = "https://plant-care.up.railway.app"
    private val notificationCooldown = 60000L // 1 минута между уведомлениями

    override fun onCreate() {
        super.onCreate()
        sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d("SensorCheckService", "========== ЗАПУСК ПРОВЕРКИ ==========")

        val email = sharedPref.getString("email", null)
        if (email.isNullOrEmpty()) {
            Log.e("SensorCheckService", "Email не найден")
            return
        }

        // 1. Получаем список сценариев пользователя
        val scenarios = getUserScenarios(email)
        if (scenarios.isEmpty()) {
            Log.d("SensorCheckService", "Нет сценариев для проверки")
            return
        }

        // 2. Получаем последние данные с устройства
        val deviceId = "ESP32_PlantMonitor"
        val sensorData = getLatestSensorData(deviceId)
        if (sensorData == null) {
            Log.e("SensorCheckService", "Не удалось получить данные датчика")
            return
        }

        Log.d("SensorCheckService", "📊 Данные: soil=${sensorData.soil}%, temp=${sensorData.temp}°C, light=${sensorData.light} лк, humidity=${sensorData.humidity}%")

        // 3. Собираем все нарушения
        val violations = mutableListOf<String>()

        for (scenario in scenarios) {
            val scenarioName = scenario.name

            // Проверка температуры (ниже нормы)
            if (scenario.minTemp != 1000f && sensorData.temp < scenario.minTemp) {
                violations.add("🌡️ $scenarioName: Низкая температура (${sensorData.temp}°C < ${scenario.minTemp}°C)")
            }
            // Проверка температуры (выше нормы)
            if (scenario.maxTemp != 1000f && sensorData.temp > scenario.maxTemp) {
                violations.add("🌡️ $scenarioName: Высокая температура (${sensorData.temp}°C > ${scenario.maxTemp}°C)")
            }

            // Проверка влажности почвы (ниже нормы - сухо)
            if (scenario.minSoil != 1000f && sensorData.soil < scenario.minSoil) {
                violations.add("💧 $scenarioName: Низкая влажность почвы (${sensorData.soil}% < ${scenario.minSoil}%)")
            }
            // Проверка влажности почвы (выше нормы - переувлажнение)
            if (scenario.maxSoil != 1000f && sensorData.soil > scenario.maxSoil) {
                violations.add("💧 $scenarioName: Высокая влажность почвы (${sensorData.soil}% > ${scenario.maxSoil}%)")
            }

            // Проверка влажности воздуха (ниже нормы)
            if (scenario.minHum != 1000f && sensorData.humidity < scenario.minHum) {
                violations.add("💨 $scenarioName: Низкая влажность воздуха (${sensorData.humidity}% < ${scenario.minHum}%)")
            }
            // Проверка влажности воздуха (выше нормы)
            if (scenario.maxHum != 1000f && sensorData.humidity > scenario.maxHum) {
                violations.add("💧 $scenarioName: Высокая влажность воздуха (${sensorData.humidity}% > ${scenario.maxHum}%)")
            }

            // Проверка освещенности (ниже нормы)
            if (scenario.minLight != 1000f && sensorData.light < scenario.minLight) {
                violations.add("💡 $scenarioName: Недостаточно света (${sensorData.light} лк < ${scenario.minLight} лк)")
            }
            // Проверка освещенности (выше нормы)
            if (scenario.maxLight != 1000f && sensorData.light > scenario.maxLight) {
                violations.add("☀️ $scenarioName: Слишком ярко (${sensorData.light} лк > ${scenario.maxLight} лк)")
            }
        }

        // 4. Отправляем одно уведомление, если есть нарушения
        if (violations.isNotEmpty()) {
            sendCombinedNotification(violations)
        } else {
            Log.d("SensorCheckService", "✅ Нарушений не найдено")
        }

        Log.d("SensorCheckService", "✅ Проверка завершена, найдено нарушений: ${violations.size}")
    }

    /**
     * Отправляет одно уведомление со списком всех нарушений (не чаще раза в минуту)
     */
    private fun sendCombinedNotification(violations: List<String>) {
        val prefs = getSharedPreferences("NotificationCooldown", MODE_PRIVATE)
        val lastSent = prefs.getLong("last_combined_notification", 0)
        val now = System.currentTimeMillis()

        if (now - lastSent < notificationCooldown) {
            Log.d("SensorCheckService", "⏳ Уведомление было отправлено недавно, пропускаем")
            return
        }

        // Формируем сообщение
        val title = "Обратите внимания!"
        val message = violations.joinToString("\n")

        // Отправляем уведомление
        NotificationHelper.sendNotification(this, title, message)
        prefs.edit().putLong("last_combined_notification", now).apply()
        Log.d("SensorCheckService", "🔔 Отправлено общее уведомление с ${violations.size} проблемами")
    }

    // Получение списка сценариев с сервера
    private fun getUserScenarios(email: String): List<UserScenario> {
        val result = mutableListOf<UserScenario>()
        try {
            val url = URL("$baseUrl/api/user/scenarios?username=$email")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            Log.d("SensorCheckService", "📡 Запрос сценариев, код: ${connection.responseCode}")

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val response = reader.readText()
                reader.close()

                Log.d("SensorCheckService", "📄 Ответ: $response")

                val json = org.json.JSONObject(response)
                if (json.getBoolean("success")) {
                    val arr = json.getJSONArray("scenarios_of_user")
                    Log.d("SensorCheckService", "📋 Найдено сценариев: ${arr.length()}")

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val displayName = if (obj.has("display_name") && !obj.isNull("display_name")) {
                            obj.getString("display_name")
                        } else {
                            obj.getString("scenario_name")
                        }
                        val scenario = UserScenario(
                            name = displayName,
                            minTemp = obj.optDouble("min_temperature", 1000.0).toFloat(),
                            maxTemp = obj.optDouble("max_temperature", 1000.0).toFloat(),
                            minSoil = obj.optDouble("min_soil_moisture", 1000.0).toFloat(),
                            maxSoil = obj.optDouble("max_soil_moisture", 1000.0).toFloat(),
                            minHum = obj.optDouble("min_humidity", 1000.0).toFloat(),
                            maxHum = obj.optDouble("max_humidity", 1000.0).toFloat(),
                            minLight = obj.optDouble("min_light_lux", 1000.0).toFloat(),
                            maxLight = obj.optDouble("max_light_lux", 1000.0).toFloat()
                        )
                        result.add(scenario)
                        Log.d("SensorCheckService", "  📌 Сценарий: ${scenario.name}")
                        Log.d("SensorCheckService", "     Почва: ${scenario.minSoil} - ${scenario.maxSoil}")
                        Log.d("SensorCheckService", "     Темп: ${scenario.minTemp} - ${scenario.maxTemp}")
                        Log.d("SensorCheckService", "     Свет: ${scenario.minLight} - ${scenario.maxLight}")
                    }
                }
            } else if (connection.responseCode == 401) {
                Log.e("SensorCheckService", "❌ Ошибка авторизации! Нужно перелогиниться")
            } else {
                Log.e("SensorCheckService", "❌ Ошибка получения сценариев: ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("SensorCheckService", "❌ Исключение при получении сценариев: ${e.message}")
        }
        return result
    }

    // Получение последних данных датчика
    private fun getLatestSensorData(deviceId: String): SensorData? {
        return try {
            val url = URL("$baseUrl/api/device/$deviceId/data")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                val json = org.json.JSONObject(response)
                SensorData(
                    light = json.getDouble("light").toFloat(),
                    soil = json.getDouble("soil").toFloat(),
                    temp = json.getDouble("temp").toFloat(),
                    humidity = json.getDouble("humidity").toFloat(),
                    pump = json.getBoolean("pump"),
                    timestamp = parseTimestamp(json.get("timestamp"))
                )
            } else {
                Log.e("SensorCheckService", "❌ Ошибка получения данных: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("SensorCheckService", "❌ Исключение при получении данных: ${e.message}")
            null
        }
    }

    private fun parseTimestamp(timestampObj: Any): Long {
        return when (timestampObj) {
            is Long -> timestampObj
            is Int -> timestampObj.toLong()
            is String -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
                        val dateTime = java.time.LocalDateTime.parse(timestampObj, formatter)
                        dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } else {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
                        val date = format.parse(timestampObj)
                        date?.time ?: System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.e("SensorCheckService", "Ошибка парсинга даты: ${e.message}")
                    System.currentTimeMillis()
                }
            }
            else -> System.currentTimeMillis()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SensorCheckService::class.java)
            context.startService(intent)
        }

        fun setupAlarmManager(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SensorCheckService::class.java)
            val pendingIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val interval = 60000L // 1 минута

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    interval,
                    pendingIntent
                )
            } else {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    interval,
                    pendingIntent
                )
            }
            Log.d("SensorCheckService", "⏰ AlarmManager установлен с интервалом $interval мс")
        }
    }
}