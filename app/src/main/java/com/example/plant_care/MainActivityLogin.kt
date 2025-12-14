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
import org.json.JSONObject
import java.io.IOException

class MainActivityLogin : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var togglePasswordVisibilitySwitch: Switch

    // Поменяйте на IP вашего сервера в локальной сети (например "http://192.168.1.108:5000")
    // Для эмулятора: "http://10.0.2.2:5000"
    private val BASE_URL = "http://192.168.1.108:5000"

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
            email.isBlank() || password.isBlank() -> {
                showToast("Пожалуйста, заполните все поля")
                false
            }
            password.length < 6 -> {
                showToast("Введите пароль от 6 символов")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Введите корректный адрес почты")
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
        lifecycleScope.launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", email)
                    put("password", password)
                }.toString()

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL/auth/register")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val responseBody = try { response.body?.string() } catch (e: Exception) { null }

                Log.d(TAG, "register() code=${response.code} body=$responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Сервер может возвращать пустое тело — всё равно считаем регистрацию успешной.
                        // Сохраняем синхронно, чтобы MainActivity увидел значение сразу.
                        val editor = sharedPreferences.edit()
                        editor.putString("email", email)
                        editor.putBoolean("isLoggedIn", true)
                        editor.commit()

                       // showToast("Регистрация успешна")
                            // Переходим в MainActivity; ставим флаг skipLoginCheck чтобы главный экран не редиректил обратно.
                            // Также очищаем стек, чтобы не вернуться на экран логина.
                            val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.putExtra("skipLoginCheck", true)
                            startActivity(intent)
                            finish()
                    } else {
                        // Явная обработка 409 — пользователь с таким email уже существует
                        if (response.code == 409) {
                            showToast("Пользователь с таким email уже существует")
                        } else {
                            // Остальные ошибки: разбираем тело или показываем код
                            handleErrorResponse(response, responseBody)
                        }
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

    /**
     * Авто-вход: делаем POST /auth/login. Если тело пустое, но HTTP 200 — считаем вход успешным.
     * Сохраняем токен (если пришёл) и переходим в MainActivity.
     */
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

                Log.d(TAG, "autoLogin() code=${response.code} body=$responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Если тело пустое — считаем, что сервер подтвердил вход (fallback).
                        if (responseBody.isNullOrEmpty()) {
                            val editor = sharedPreferences.edit()
                            editor.putString("email", email)
                            editor.putBoolean("isLoggedIn", true)
                            editor.commit()

                            val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.putExtra("skipLoginCheck", true)
                            startActivity(intent)
                            finish()
                            return@withContext
                        }

                        // Если тело есть — парсим JSON и сохраняем токен, если он есть
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val success = jsonResponse.optBoolean("success", true)
                            val token = jsonResponse.optString("token", "")
                            val username = jsonResponse.optString("user", "")

                            if (success) {
                                val editor = sharedPreferences.edit()
                                editor.putString("email", email)
                                if (token.isNotEmpty()) editor.putString("jwt_token", token)
                                if (username.isNotEmpty()) editor.putString("username", username)
                                editor.putBoolean("isLoggedIn", true)
                                editor.commit()

                                showToast("Вход выполнен")
                                val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("skipLoginCheck", true)
                                startActivity(intent)
                                finish()
                            } else {
                                val message = jsonResponse.optString("message", "Вход не удался")
                                showToast(message)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Login JSON parse error", e)
                            // Если парсинг упал — делаем fallback: считаем вход выполненным
                            val editor = sharedPreferences.edit()
                            editor.putString("email", email)
                            editor.putBoolean("isLoggedIn", true)
                            editor.commit()

                            showToast("Вход выполнен (не удалось распарсить ответ).")
                            val intent = Intent(this@MainActivityLogin, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.putExtra("skipLoginCheck", true)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Вход неуспешен — обрабатываем ошибки
                        // Если сервер может вернуть 401/400/409 и т.д. — handleErrorResponse покроет их
                        handleErrorResponse(response, responseBody)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-login error", e)
                withContext(Dispatchers.Main) {
                    showToast("Ошибка автоматического входа")
                }
            }
        }
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
            409 -> showToast("Пользователь с таким email уже существует")
            500 -> showToast("Ошибка сервера. Попробуйте позже")
            else -> showToast("Ошибка сервера: ${response.code}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

