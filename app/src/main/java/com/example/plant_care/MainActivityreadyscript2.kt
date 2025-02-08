package com.example.plant_care

import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivityreadyscript2 : AppCompatActivity() {

    private lateinit var AirHumiditySwitch: Switch
    private lateinit var AirHumidityTextView: TextView
    private lateinit var AirHumiditym1TextView: TextView
    private lateinit var AirHumiditym2TextView: TextView
    private lateinit var AirHumiditym1EditText: EditText
    private lateinit var AirHumiditym2EditText: EditText
    private lateinit var AirHumidityButton: Button
    private lateinit var vlevoButton: ImageButton

    val textAir = "    Условия для влажности воздуха:\n" +
            "   - Если уровень влажности воздуха < m1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень влажности воздуха > m2%.\n" +
            "   - Отправить уведомление.\n"

    val textm1 = "введите m1:"
    val textm2 = "введите m2:"
    private val REQUEST_CODE = 1 // Код запроса для идентификации результата

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript2)

        var m1 = ""
        var m2 = ""
        var n1 = intent.getStringExtra("n1") ?: ""
        var n2 = intent.getStringExtra("n2") ?: ""
        var n1v = intent.getStringExtra("n1v") ?: ""
        var n2v = intent.getStringExtra("n2v") ?: ""
        var wateringswitchState = intent.getBooleanExtra("wateringswitchState", false)
        var wateringswitchuvState = intent.getBooleanExtra("wateringswitchuvState", false)

        AirHumiditySwitch = findViewById(R.id.switch1)
        AirHumidityTextView = findViewById(R.id.textView12)
        AirHumiditym1TextView = findViewById(R.id.textView15)
        AirHumiditym2TextView = findViewById(R.id.textView16)
        AirHumiditym1EditText = findViewById(R.id.editTextText7)
        AirHumiditym2EditText = findViewById(R.id.editTextText5)
        AirHumidityButton = findViewById(R.id.button8)
        vlevoButton = findViewById(R.id.imageButton2)

        var intent = Intent(this, MainActivityreadyscript::class.java)
        intent.putExtra("switchState", AirHumiditySwitch.isChecked) // Передаем состояние свитча


        AirHumiditySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AirHumidityTextView.text = textAir // Устанавливаем текст при включении
                AirHumiditym1TextView.text = textm1
                AirHumiditym2TextView.text = textm2
                AirHumiditym1TextView.visibility = View.VISIBLE
                AirHumiditym2TextView.visibility = View.VISIBLE
                AirHumidityButton.visibility = View.VISIBLE // Показываем кнопку
                AirHumidityTextView.visibility = View.VISIBLE // Показываем текст
                AirHumiditym1EditText.visibility = View.VISIBLE // Показываем EditText
                AirHumiditym2EditText.visibility = View.VISIBLE // Показываем EditText
                AirHumidityButton.setOnClickListener {
                    m1 = AirHumiditym1EditText.text.toString()
                    m2 = AirHumiditym2EditText.text.toString()

                    var m1Int = m1.toIntOrNull()
                    var m2Int = m2.toIntOrNull()

                    if(m1Int != null && m2Int != null){
                        intent.putExtra("m1Int", m1Int) // передача данных влажности воздуха
                        intent.putExtra("m2Int", m2Int)
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                AirHumidityTextView.visibility = View.GONE // Скрываем текст
                AirHumiditym1TextView.visibility = View.GONE // Скрываем текст
                AirHumiditym2TextView.visibility = View.GONE // Скрываем текст
                AirHumiditym1EditText.visibility = View.GONE // Скрываем EditText
                AirHumiditym2EditText.visibility = View.GONE // Скрываем EditText
                AirHumidityButton.visibility = View.GONE // Скрываем кнопку
            }
        }

        vlevoButton.setOnClickListener{
            var intent = Intent(this, MainActivityreadyscript::class.java)
            intent.putExtra("switchState", AirHumiditySwitch.isChecked) // Передаем состояние свитча
            intent.putExtra("n1", n1)
            intent.putExtra("n2", n2)
            intent.putExtra("n1v", n1v)
            intent.putExtra("n2v", n2v)
            intent.putExtra("wateringswitchState", wateringswitchState)
            intent.putExtra("wateringswitchuvState", wateringswitchuvState)
            setResult(RESULT_OK, intent) // Устанавливаем результат
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}