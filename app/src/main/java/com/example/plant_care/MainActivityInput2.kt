package com.example.plant_care

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivityInput2 : AppCompatActivity() {

    private lateinit var ipEditText: EditText
    private lateinit var submitButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_input2)

        // Инициализация элементов UI
        ipEditText = findViewById(R.id.ipEditText)
        submitButton = findViewById(R.id.submitButton)
        val instructionsText = findViewById<TextView>(R.id.instructionsText)

        // Установка инструкции
        instructionsText.text = """
            Инструкция:
            1. Убедитесь, что телефон и ESP32 подключены к одной сети Wi-Fi
            2. Введите адрес ESP32 в поле выше
            3. Нажмите "Отправить на ESP32"
        """.trimIndent()

        // Обработка нажатия кнопки
        submitButton.setOnClickListener {
            val ipAddress = ipEditText.text.toString().trim()
            // Полностью убрана проверка IP
            sendIpToEsp(ipAddress)
        }
    }

    private fun sendIpToEsp(phoneIp: String) {
        Thread {
            try {
                val url = "http://$phoneIp/updatePhoneIP" // Используем введенный адрес как есть

                val json = JSONObject().apply {
                    put("phoneIP", phoneIp)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "Данные отправлены на $phoneIp", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Ошибка отправки: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка соединения: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}