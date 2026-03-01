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
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
// Импортируем модели
import com.example.plant_care.UserScenario
import com.example.plant_care.SensorData

class SensorCheckService : IntentService("SensorCheckService") {

    private lateinit var sharedPref: SharedPreferences
    private val baseUrl = "http://192.168.1.107:5000" // тот же сервер

    override fun onCreate() {
        super.onCreate()
        sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d("SensorCheckService", "Запуск проверки...")

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

        // 2. Получаем последние данные с устройства (одно устройство)
        val deviceId = "ESP32_PlantMonitor" // из кода ESP32
        val sensorData = getLatestSensorData(deviceId)
        if (sensorData == null) {
            Log.e("SensorCheckService", "Не удалось получить данные датчика")
            return
        }

        // 3. Проверяем каждый сценарий
        var needPump = false
        for (scenario in scenarios) {
            checkScenario(scenario, sensorData)?.let { message ->
                // Отправляем уведомление
                val title = "Растение: ${scenario.name}"
                NotificationHelper.sendNotification(this, title, message)
            }
            // Определяем, нужен ли полив (хотя бы один сценарий требует)
            if (scenario.minSoil != 1000f && sensorData.soil < scenario.minSoil) {
                needPump = true
            }
            // Если какой-то сценарий требует выключения (выше max), но другой требует включения – приоритет включения
            if (scenario.maxSoil != 1000f && sensorData.soil > scenario.maxSoil) {
                // Если ни один не требует включения, то можно выключить, но мы учтём это позже
            }
        }

        // 4. Управление насосом
        managePump(deviceId, needPump)

        Log.d("SensorCheckService", "Проверка завершена")
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

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                // Парсим JSON
                val json = org.json.JSONObject(response.toString())
                if (json.getBoolean("success")) {
                    val arr = json.getJSONArray("scenarios_of_user")
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
                    }
                }
            } else {
                Log.e("SensorCheckService", "Ошибка получения сценариев: ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("SensorCheckService", "Исключение при получении сценариев: ${e.message}")
        }
        return result
    }

    // Получение последних данных датчика
    private fun getLatestSensorData(deviceId: String): SensorData? {
        return try {
            val url = URL("$baseUrl/api/device/latest?device_id=$deviceId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                connection.disconnect()

                // Парсим JSON
                val json = org.json.JSONObject(response.toString())

                // Исправлено: правильный парсинг timestamp
                SensorData(
                    light = json.getDouble("light").toFloat(),
                    soil = json.getDouble("soil").toFloat(),
                    temp = json.getDouble("temp").toFloat(),
                    humidity = json.getDouble("humidity").toFloat(),
                    pump = json.getBoolean("pump"),
                    timestamp = parseTimestamp(json.get("timestamp"))
                )
            } else {
                Log.e("SensorCheckService", "Ошибка получения данных: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("SensorCheckService", "Исключение при получении данных: ${e.message}")
            null
        }
    }

    // Добавьте эту функцию в класс SensorCheckService
    private fun parseTimestamp(timestampObj: Any): Long {
        return when (timestampObj) {
            is Long -> timestampObj
            is Int -> timestampObj.toLong()
            is String -> {
                try {
                    // Для Android 8+ (API 26)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
                        val dateTime = java.time.LocalDateTime.parse(timestampObj, formatter)
                        dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } else {
                        // Для старых версий Android
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
                        val date = format.parse(timestampObj)
                        date?.time ?: System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.e("SensorCheckService", "Ошибка парсинга даты: ${e.message}")
                    System.currentTimeMillis() // Возвращаем текущее время как запасной вариант
                }
            }
            else -> System.currentTimeMillis()
        }
    }


    // Проверка одного сценария: возвращает сообщение для уведомления или null
    private fun checkScenario(scenario: UserScenario, data: SensorData): String? {
        val problems = mutableListOf<String>()

        // Температура
        if (scenario.minTemp != 1000f && data.temp < scenario.minTemp) {
            problems.add("температура ниже нормы (${data.temp}°C < ${scenario.minTemp}°C)")
        }
        if (scenario.maxTemp != 1000f && data.temp > scenario.maxTemp) {
            problems.add("температура выше нормы (${data.temp}°C > ${scenario.maxTemp}°C)")
        }

        // Влажность воздуха
        if (scenario.minHum != 1000f && data.humidity < scenario.minHum) {
            problems.add("влажность воздуха ниже нормы (${data.humidity}% < ${scenario.minHum}%)")
        }
        if (scenario.maxHum != 1000f && data.humidity > scenario.maxHum) {
            problems.add("влажность воздуха выше нормы (${data.humidity}% > ${scenario.maxHum}%)")
        }

        // Влажность почвы (только уведомление, управление насосом отдельно)
        if (scenario.minSoil != 1000f && data.soil < scenario.minSoil) {
            problems.add("почва слишком сухая (${data.soil}% < ${scenario.minSoil}%)")
        }
        if (scenario.maxSoil != 1000f && data.soil > scenario.maxSoil) {
            problems.add("почва слишком влажная (${data.soil}% > ${scenario.maxSoil}%)")
        }

        // Освещённость
        if (scenario.minLight != 1000f && data.light < scenario.minLight) {
            problems.add("освещённость ниже нормы (${data.light} лк < ${scenario.minLight} лк)")
        }
        if (scenario.maxLight != 1000f && data.light > scenario.maxLight) {
            problems.add("освещённость выше нормы (${data.light} лк > ${scenario.maxLight} лк)")
        }

        return if (problems.isNotEmpty()) {
            problems.joinToString("\n")
        } else null
    }

    // Управление насосом через сервер
    private fun managePump(deviceId: String, needPump: Boolean) {
        try {
            val url = URL("$baseUrl/api/device/pump")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val json = """
                {
                    "device_id": "$deviceId",
                    "state": $needPump
                }
            """.trimIndent()

            val outputStream: OutputStream = connection.outputStream
            outputStream.write(json.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Log.d("SensorCheckService", "Команда насосу отправлена: $needPump")
            } else {
                Log.e("SensorCheckService", "Ошибка отправки команды насосу: $responseCode")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("SensorCheckService", "Исключение при управлении насосом: ${e.message}")
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

            // Интервал проверки – 15 минут (можно изменить)
            val interval = 60000L

            // Устанавливаем повторяющийся будильник с учётом Doze
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
            Log.d("SensorCheckService", "AlarmManager установлен с интервалом $interval мс")
        }
    }
}