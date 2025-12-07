package com.example.plant_care

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivitylogin2 : AppCompatActivity() {
    private lateinit var emailEditText2: EditText
    private lateinit var passEditText2: EditText
    private lateinit var submitButton: Button
    private lateinit var togglePasswordVisibilitySwitch2: Switch

    // Адрес вашего локального сервера (поставьте ваш IP)
    private val BASE_URL = "http://192.168.1.108:5000"

    private val client = OkHttpClient()
    private val sharedPreferences by lazy { getSharedPreferences("MyPrefs", MODE_PRIVATE) }
    private val TAG = "MainActivitylogin2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activitylogin2)

        emailEditText2 = findViewById(R.id.emailEditText2)
        passEditText2 = findViewById(R.id.passEditText2)
        submitButton = findViewById(R.id.loginbutton)
        togglePasswordVisibilitySwitch2 = findViewById(R.id.togglePasswordVisibilitySwitch2)

        passEditText2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        togglePasswordVisibilitySwitch2.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked)
        }

        submitButton.setOnClickListener {
            val email = emailEditText2.text.toString().trim()
            val pass = passEditText2.text.toString().trim()
            if (validateInput(email, pass)) {
                login(email, pass)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() || password.isBlank() -> {
                showToast("Пожалуйста, заполните все поля")
                false
            }
            else -> true
        }
    }

    private fun togglePasswordVisibility(isChecked: Boolean) {
        passEditText2.inputType = if (isChecked) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passEditText2.setSelection(passEditText2.text.length)
    }

    fun register(v: View) {
        startActivity(Intent(this, MainActivityLogin::class.java))
    }

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // отправляем username чтобы совпадало с сервером
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

                Log.d(TAG, "login() code=${response.code} body=$responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Фаллбек: если тело пустое, но код 200 — считаем вход успешным
                        if (responseBody.isNullOrEmpty()) {
                            saveLoginAndGoMain(email, null)
                          //  showToast("Вход выполнен (без тела ответа).")
                            return@withContext
                        }

                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.optBoolean("success", true)
                            val message = jsonResponse.optString("message", "")
                            val token = jsonResponse.optString("token", "")
                            val user = jsonResponse.optString("user", "")

                            if (success) {
                                // Сохраняем синхронно перед переходом
                                saveLoginAndGoMain(email, if (token.isNotEmpty()) token else null, user)
                                Log.d(TAG, "Login successful. token=${if (token.isNotEmpty()) "present" else "absent"}")
                                showToast(if (message.isNotEmpty()) message else "Вход выполнен успешно!")
                            } else {
                                showToast(if (message.isNotEmpty()) message else "Ошибка входа")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error", e)
                            // Если парсинг упал, но HTTP 200 — делаем fallback
                            saveLoginAndGoMain(email, null)
                           // showToast("Вход выполнен (не удалось распарсить ответ).")
                        }
                    } else {
                        // Ошибки HTTP
                        handleErrorResponse(response, responseBody)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                withContext(Dispatchers.Main) {
                    showToast("Ошибка сети. Проверьте подключение и адрес сервера")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                withContext(Dispatchers.Main) {
                    showToast("Ошибка: ${e.localizedMessage}")
                }
            }
        }
    }

    // Сохраняем логин синхронно и переходим в MainActivity (очищаем стек)
    private fun saveLoginAndGoMain(email: String, token: String?, username: String? = null) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        username?.let { editor.putString("username", it) }
        token?.let { editor.putString("jwt_token", it) }
        editor.putBoolean("isLoggedIn", true)
        editor.commit() // синхронная запись!

        val intent = Intent(this@MainActivitylogin2, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("skipLoginCheck", true)
        startActivity(intent)
        finish()
    }

    private fun handleErrorResponse(response: okhttp3.Response, responseBody: String?) {
        when (response.code) {
            400 -> {
                try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    val message = errorJson.optString("message", "Некорректный запрос")
                    showToast("Ошибка: $message")
                } catch (e: Exception) {
                    showToast("Ошибка 400: Некорректный запрос")
                }
            }
            401 -> {
                try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    val message = errorJson.optString("message", "Неверный email или пароль")
                    showToast(message)
                } catch (e: Exception) {
                    showToast("Неверный email или пароль")
                }
            }
            404 -> showToast("Пользователь не найден")
            500 -> showToast("Ошибка сервера. Попробуйте позже")
            else -> showToast("Ошибка сервера: ${response.code}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
