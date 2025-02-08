package com.example.plant_care

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class MainActivitylogin2 : AppCompatActivity() {
    private lateinit var emailEditText2: EditText
    private lateinit var passEditText2:  EditText
    private lateinit var submitButton: Button
    private lateinit var togglePasswordVisibilitySwitch2: Switch

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
            val email = emailEditText2.text.toString()
            val pass = passEditText2.text.toString()
            login(email,pass)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun togglePasswordVisibility(isChecked: Boolean) {
        if (isChecked) {
            passEditText2.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            passEditText2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passEditText2.setSelection(passEditText2.text.length)
    }

    private fun login(email: String, password: String) {
        // Проверка на пустые строки
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return // Выход из функции, если поля пустые
        }
        if (password.length < 6){
            Toast.makeText(this, "Введите пароль от 6 символов", Toast.LENGTH_SHORT).show()
            return
        }
        if (!email.contains("@")) {
            Toast.makeText(this, "Введите корректный адрес почты", Toast.LENGTH_SHORT).show()
            return // Выход из функции, если в строке почты нет символа "@"
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Успешный вход, обновляем UI с информацией о пользователе
                    Log.d(TAG, "signInWithEmail:success")
                    val user = FirebaseAuth.getInstance().currentUser
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Если вход не удался, отображаем сообщение пользователю
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Не удалось войти в аккаунт", Toast.LENGTH_SHORT).show()
                }
            }
    }
}