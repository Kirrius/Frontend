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
    private lateinit var TempSwitch: Switch
    private lateinit var TempTextView: TextView
    private lateinit var AirHumidityTextView: TextView
    private lateinit var AirHumiditym1TextView: TextView
    private lateinit var AirHumiditym2TextView: TextView
    private lateinit var TempTextViewt1: TextView
    private lateinit var TempTextViewt2: TextView
    private lateinit var TempEditText1: EditText
    private lateinit var TempEditText2: EditText
    private lateinit var AirHumiditym1EditText: EditText
    private lateinit var AirHumiditym2EditText: EditText
    private lateinit var AirHumidityButton: Button
    private lateinit var TempButton: Button
    private lateinit var vlevoButton: ImageButton

    val textAir = "    Условия для влажности воздуха:\n" +
            "   - Если уровень влажности воздуха < m1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень влажности воздуха > m2%.\n" +
            "   - Отправить уведомление.\n"

    val textm1 = "введите\nm1:"
    val textm2 = "введите\nm2:"

    val textAirTemp = "    Условия для температуры воздуха:\n" +
            "   - Если уровень температуры воздуха < t1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень температуры воздуха > t2%.\n" +
            "   - Отправить уведомление.\n"

    val textt1 = "введите\nt1:"
    val textt2 = "введите\nt2:"

    private val REQUEST_CODE = 1 // Код запроса для идентификации результата

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript2)

        var m1 = intent.getStringExtra("m1") ?: ""
        var m2 = intent.getStringExtra("m2") ?: ""
        var n1 = intent.getStringExtra("n1") ?: ""
        var n2 = intent.getStringExtra("n2") ?: ""
        var n1v = intent.getStringExtra("n1v") ?: ""
        var n2v = intent.getStringExtra("n2v") ?: ""
        var t1 = intent.getStringExtra("t1") ?: ""
        var t2 = intent.getStringExtra("t2") ?: ""
        var t1Int = intent.getIntExtra("t1Int", -1)
        var t2Int = intent.getIntExtra("t2Int", -1)
        var m1Int = intent.getIntExtra("m1Int", -1)
        var m2Int = intent.getIntExtra("m2Int", -1)
        var wateringswitchState = intent.getBooleanExtra("wateringswitchState", false)
        var wateringswitchuvState = intent.getBooleanExtra("wateringswitchuvState", false)
        var AirHumiditySwitchState = intent.getBooleanExtra("AirHumidityswitchState", false)
        var TempSwitchState = intent.getBooleanExtra("TempswitchState", false)

        AirHumiditySwitch = findViewById(R.id.switch1)
        TempSwitch = findViewById(R.id.switch4)
        AirHumidityTextView = findViewById(R.id.textView12)
        TempTextView = findViewById(R.id.textView17)
        AirHumiditym1TextView = findViewById(R.id.textView15)
        AirHumiditym2TextView = findViewById(R.id.textView16)
        TempTextViewt1 = findViewById(R.id.textView18)
        TempTextViewt2 = findViewById(R.id.textView19)
        TempEditText1 = findViewById(R.id.editTextText8)
        TempEditText2 = findViewById(R.id.editTextText9)
        AirHumiditym1EditText = findViewById(R.id.editTextText7)
        AirHumiditym2EditText = findViewById(R.id.editTextText5)
        AirHumidityButton = findViewById(R.id.button8)
        TempButton = findViewById(R.id.button12)
        vlevoButton = findViewById(R.id.imageButton2)

        AirHumiditySwitch.isChecked = AirHumiditySwitchState
        TempSwitch.isChecked = TempSwitchState

        if (AirHumiditySwitch.isChecked){
            AirHumidityTextView.text = textAir // Устанавливаем текст при включении
            AirHumiditym1TextView.text = textm1
            AirHumiditym2TextView.text = textm2
            AirHumiditym1TextView.visibility = View.VISIBLE
            AirHumiditym2TextView.visibility = View.VISIBLE
            AirHumidityButton.visibility = View.VISIBLE // Показываем кнопку
            AirHumidityTextView.visibility = View.VISIBLE // Показываем текст
            AirHumiditym1EditText.visibility = View.VISIBLE // Показываем EditText
            AirHumiditym2EditText.visibility = View.VISIBLE // Показываем EditText
            AirHumiditym1EditText.setText(m1)
            AirHumiditym2EditText.setText(m2)
            AirHumidityButton.setOnClickListener {
                m1 = AirHumiditym1EditText.text.toString()
                m2 = AirHumiditym2EditText.text.toString()

                m1Int = m1.toIntOrNull() ?: -1
                m2Int = m2.toIntOrNull() ?: - 1

                if(m1Int == -1 || m2Int == -1) {
                    Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                }
            }

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
                    AirHumiditym1EditText.setText(m1)
                    AirHumiditym2EditText.setText(m2)
                    AirHumidityButton.setOnClickListener {
                        m1 = AirHumiditym1EditText.text.toString()
                        m2 = AirHumiditym2EditText.text.toString()

                        m1Int = m1.toIntOrNull() ?: -1
                        m2Int = m2.toIntOrNull() ?: - 1

                        if(m1Int == -1 || m2Int == -1) {
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
        }

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
                AirHumiditym1EditText.setText(m1)
                AirHumiditym2EditText.setText(m2)
                AirHumidityButton.setOnClickListener {
                    m1 = AirHumiditym1EditText.text.toString()
                    m2 = AirHumiditym2EditText.text.toString()

                    m1Int = m1.toIntOrNull() ?: -1
                    m2Int = m2.toIntOrNull() ?: - 1

                    if(m1Int == -1 || m2Int == -1) {
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

        if (TempSwitch.isChecked) {
            TempTextView.text = textAirTemp // Устанавливаем текст при включении
            TempTextViewt1.text = textt1
            TempTextViewt2.text = textt2
            TempTextViewt1.visibility = View.VISIBLE
            TempTextViewt2.visibility = View.VISIBLE
            TempTextView.visibility = View.VISIBLE
            TempButton.visibility = View.VISIBLE // Показываем кнопку
            TempEditText1.visibility = View.VISIBLE // Показываем EditText
            TempEditText2.visibility = View.VISIBLE // Показываем EditText
            TempEditText1.setText(t1)
            TempEditText2.setText(t2)
            TempButton.setOnClickListener {
                t1 = TempEditText1.text.toString()
                t2 = TempEditText2.text.toString()

                t1Int = t1.toIntOrNull() ?: -1
                t2Int = t2.toIntOrNull() ?: -1

                if (t1Int == -1 || t2Int == -1){
                    Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                }
            }

            TempSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    TempTextView.text = textAirTemp // Устанавливаем текст при включении
                    TempTextViewt1.text = textt1
                    TempTextViewt2.text = textt2
                    TempTextViewt1.visibility = View.VISIBLE
                    TempTextViewt2.visibility = View.VISIBLE
                    TempTextView.visibility = View.VISIBLE
                    TempButton.visibility = View.VISIBLE // Показываем кнопку
                    TempEditText1.visibility = View.VISIBLE // Показываем EditText
                    TempEditText2.visibility = View.VISIBLE // Показываем EditText
                    TempEditText1.setText(t1)
                    TempEditText2.setText(t2)
                    TempButton.setOnClickListener {
                        t1 = TempEditText1.text.toString()
                        t2 = TempEditText2.text.toString()

                        t1Int = t1.toIntOrNull() ?: -1
                        t2Int = t2.toIntOrNull() ?: -1

                        if (t1Int == -1 || t2Int == -1){
                            Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    TempTextView.visibility = View.GONE // Скрываем текст
                    TempTextViewt1.visibility = View.GONE // Скрываем текст
                    TempTextViewt2.visibility = View.GONE // Скрываем текст
                    TempEditText1.visibility = View.GONE // Скрываем EditText
                    TempEditText2.visibility = View.GONE // Скрываем EditText
                    TempButton.visibility = View.GONE // Скрываем кнопку
                }
            }
        }

        TempSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                TempTextView.text = textAirTemp // Устанавливаем текст при включении
                TempTextViewt1.text = textt1
                TempTextViewt2.text = textt2
                TempTextViewt1.visibility = View.VISIBLE
                TempTextViewt2.visibility = View.VISIBLE
                TempTextView.visibility = View.VISIBLE
                TempButton.visibility = View.VISIBLE // Показываем кнопку
                TempEditText1.visibility = View.VISIBLE // Показываем EditText
                TempEditText2.visibility = View.VISIBLE // Показываем EditText
                TempEditText1.setText(t1)
                TempEditText2.setText(t2)
                TempButton.setOnClickListener {
                    t1 = TempEditText1.text.toString()
                    t2 = TempEditText2.text.toString()

                    t1Int = t1.toIntOrNull() ?: -1
                    t2Int = t2.toIntOrNull() ?: -1

                    if (t1Int == -1 || t2Int == -1){
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                TempTextView.visibility = View.GONE // Скрываем текст
                TempTextViewt1.visibility = View.GONE // Скрываем текст
                TempTextViewt2.visibility = View.GONE // Скрываем текст
                TempEditText1.visibility = View.GONE // Скрываем EditText
                TempEditText2.visibility = View.GONE // Скрываем EditText
                TempButton.visibility = View.GONE // Скрываем кнопку
            }
        }


        vlevoButton.setOnClickListener{
            var intent = Intent(this, MainActivityreadyscript::class.java)
            intent.putExtra("n1", n1)
            intent.putExtra("n2", n2)
            intent.putExtra("n1v", n1v)
            intent.putExtra("n2v", n2v)
            intent.putExtra("wateringswitchState", wateringswitchState)
            intent.putExtra("wateringswitchuvState", wateringswitchuvState)
            intent.putExtra("m1", m1)
            intent.putExtra("m2", m2)
            intent.putExtra("t1", t1)
            intent.putExtra("t2", t2)
            intent.putExtra("t1Int", t1Int)
            intent.putExtra("t2Int", t2Int)
            intent.putExtra("m1Int", m1Int)
            intent.putExtra("m2Int", m2Int)
            intent.putExtra("AirHumidityswitchState", AirHumiditySwitch.isChecked) // Передаем состояние свитча
            intent.putExtra("TempswitchState", TempSwitch.isChecked)
            // Toast.makeText(this, "$m1", Toast.LENGTH_SHORT).show()
           // Toast.makeText(this, "$AirHumiditySwitchState", Toast.LENGTH_SHORT).show()
           // Toast.makeText(this, "$m2", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, intent) // Устанавливаем результат
            onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}