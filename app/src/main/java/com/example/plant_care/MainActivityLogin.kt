package com.example.plant_care

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivityLogin : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var togglePasswordVisibilitySwitch: Switch

    private val BASE_URL = "https://plant-care.up.railway.app"
    private val client = OkHttpClient()
    private val sharedPreferences by lazy { getSharedPreferences("MyPrefs", MODE_PRIVATE) }
    private val TAG = "MainActivityLogin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_login)

        emailEditText = findViewById(R.id.emailEditText)
        passEditText = findViewById(R.id.passEditText)
        submitButton = findViewById(R.id.regbutton)
        togglePasswordVisibilitySwitch = findViewById(R.id.togglePasswordVisibilitySwitch)

        passEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        togglePasswordVisibilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked)
        }

        submitButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val pass = passEditText.text.toString().trim()
            if (validateInput(email, pass)) {
                register(email, pass)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                showToast("Введите email")
                false
            }
            password.isBlank() -> {
                showToast("Введите пароль")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Введите корректный email")
                false
            }
            password.length < 6 -> {
                showToast("Пароль должен содержать не менее 6 символов")
                false
            }
            else -> true
        }
    }

    private fun togglePasswordVisibility(isChecked: Boolean) {
        passEditText.inputType = if (isChecked) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passEditText.setSelection(passEditText.text.length)
    }

    fun login(v: View) {
        startActivity(Intent(this, MainActivitylogin2::class.java))
    }

    private fun register(email: String, password: String) {
        Log.d(TAG, "==================================================")
        Log.d(TAG, "РЕГИСТРАЦИЯ - Отправляем запрос...")

        lifecycleScope.launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", email)
                    put("password", password)
                }.toString()

                Log.d(TAG, "JSON тело: $jsonBody")

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/auth/register")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "URL: ${request.url}")

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = try {
                    response.body?.string()
                } catch (e: Exception) {
                    null
                }

                Log.d(TAG, "Код ответа: ${response.code}")
                Log.d(TAG, "Тело ответа: ${if (responseBody.isNullOrEmpty()) "ПУСТОЕ" else responseBody}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ УСПЕШНЫЙ ОТВЕТ код ${response.code}")

                        // Сохраняем данные пользователя
                        val editor = sharedPreferences.edit()
                        editor.putString("email", email)
                        editor.putBoolean("isLoggedIn", true)

                        // Пробуем распарсить JSON, если есть тело
                        if (!responseBody.isNullOrEmpty()) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val success = jsonResponse.optBoolean("success", false)
                                if (success) {
                                    val user = jsonResponse.optString("user", email)
                                    val userId = jsonResponse.optInt("user_id", -1)
                                    editor.putString("username", user)
                                    if (userId != -1) {
                                        editor.putInt("user_id", userId)
                                    }
                                }
                            } catch (e: JSONException) {
                                Log.e(TAG, "Ошибка парсинга JSON", e)
                            }
                        }

                        editor.apply()

                        // Переходим в MainActivity
                        val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putExtra("skipLoginCheck", true)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.d(TAG, "❌ НЕУСПЕШНЫЙ ОТВЕТ код ${response.code}")

                        // Обработка ошибок с показом сообщений пользователю
                        handleErrorResponse(response, responseBody)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                showToast("Ошибка сети. Проверьте подключение.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                showToast("Произошла непредвиденная ошибка.")
            }
        }
    }

    private fun handleErrorResponse(response: okhttp3.Response, responseBody: String?) {
        when (response.code) {
            409 -> { // Конфликт - пользователь уже существует
                showToast("Пользователь с таким email уже существует.")
            }
            400 -> { // Плохой запрос
                if (!responseBody.isNullOrEmpty()) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val message = jsonResponse.optString("message", "")
                        if (message.contains("уже существует", ignoreCase = true) ||
                            message.contains("already exists", ignoreCase = true)) {
                            showToast("Пользователь с таким email уже существует.")
                        } else {
                            showToast("Некорректные данные. Проверьте ввод.")
                        }
                    } catch (e: Exception) {
                        showToast("Некорректные данные. Проверьте ввод.")
                    }
                } else {
                    showToast("Некорректные данные. Проверьте ввод.")
                }
            }
            500 -> { // Ошибка сервера
                showToast("Ошибка сервера. Попробуйте позже.")
            }
            else -> { // Другие ошибки
                showToast("Ошибка регистрации. Код: ${response.code}")
            }
        }
    }

    // Вспомогательная функция для показа Toast
    private fun showToast(message: String) {
        Toast.makeText(this@MainActivityLogin, message, Toast.LENGTH_LONG).show()
    }

    private fun autoLoginAfterRegister(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", email)
                    put("password", password)
                }.toString()

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/auth/login")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                Log.d(TAG, "autoLogin() code=${response.code}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Сохраняем данные
                        val editor = sharedPreferences.edit()
                        editor.putString("email", email)
                        editor.putBoolean("isLoggedIn", true)

                        if (!responseBody.isNullOrEmpty()) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                val token = jsonResponse.optString("token", "")
                                val username = jsonResponse.optString("user", "")
                                if (token.isNotEmpty()) editor.putString("jwt_token", token)
                                if (username.isNotEmpty()) editor.putString("username", username)
                            } catch (e: Exception) {
                                Log.e(TAG, "Login JSON parse error", e)
                            }
                        }

                        editor.apply()

                        val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putExtra("skipLoginCheck", true)
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-login error", e)
            }
        }
    }
}