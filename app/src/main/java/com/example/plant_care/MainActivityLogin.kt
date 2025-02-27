package com.example.plant_care

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.content.Context
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.text.InputType
import android.util.Log
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivityLogin : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passEditText:  EditText
    private lateinit var submitButton: Button
    private lateinit var togglePasswordVisibilitySwitch: Switch

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val email = emailEditText.text.toString()
            val pass = passEditText.text.toString()
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("Password", pass) // Сохраняем пароль
            editor.apply()
            /*
            var intent = Intent(this, MainActivityreadyscript::class.java)
            intent.putExtra("Password", pass)

             */
            register(email,pass)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun togglePasswordVisibility(isChecked: Boolean) {
        if (isChecked) {
            passEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            passEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passEditText.setSelection(passEditText.text.length)
    }

    fun login(v: View) {
        val log = Intent(this, MainActivitylogin2::class.java)
        startActivity(log)
    }

    private fun register(email: String, password: String) {
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

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Успешная регистрация, обновляем UI с информацией о пользователе
                    Log.d(TAG, "createUserWithEmail:success")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Если регистрация не удалась, отображаем сообщение пользователю
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Такой аккаунт уже есть", Toast.LENGTH_SHORT).show()
                }
            }
    }
}