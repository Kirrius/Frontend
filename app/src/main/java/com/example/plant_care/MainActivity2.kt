package com.example.plant_care

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.play.integrity.internal.y

class MainActivity2 : AppCompatActivity() {
    private lateinit var nameRost: EditText
    private lateinit var add_button: Button
    private lateinit var pop: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        nameRost = findViewById(R.id.editTextText)
        add_button = findViewById(R.id.button2)

        add_button.setOnClickListener {
            val name = nameRost.getText().toString()

            if (name.isBlank()) {
                Toast.makeText(this, "Пожалуйста, введите растение", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, MainActivityScript::class.java)
                startActivity(intent)
            }
        }
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}