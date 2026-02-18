package com.example.plant_care

import android.content.Context
import android.content.Intent
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

    private val textAir = "    Условия для влажности воздуха:\n" +
            "   - Если уровень влажности воздуха < m1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень влажности воздуха > m2%.\n" +
            "   - Отправить уведомление.\n"

    private val textm1 = "введите\nm1:"
    private val textm2 = "введите\nm2:"

    private val textAirTemp = "    Условия для температуры воздуха:\n" +
            "   - Если уровень температуры воздуха < t1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень температуры воздуха > t2%.\n" +
            "   - Отправить уведомление.\n"

    private val textt1 = "введите\nt1:"
    private val textt2 = "введите\nt2:"

    // ИЗМЕНЕНО: Используем Float вместо Int
    private var currentM1: String = ""
    private var currentM2: String = ""
    private var currentT1: String = ""
    private var currentT2: String = ""
    private var currentM1Float: Float = -1f  // Float вместо Int
    private var currentM2Float: Float = -1f  // Float вместо Int
    private var currentT1Float: Float = -1f  // Float вместо Int
    private var currentT2Float: Float = -1f  // Float вместо Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript2)

        // Получаем данные из Intent
        currentM1 = intent.getStringExtra("min_humidity") ?: ""
        currentM2 = intent.getStringExtra("max_humidity") ?: ""
        var n1 = intent.getStringExtra("min_soil_moisture") ?: ""
        var n2 = intent.getStringExtra("max_soil_moisture") ?: ""
        var l1 = intent.getStringExtra("min_light_lux") ?: ""
        var l2 = intent.getStringExtra("max_light_lux") ?: ""
        currentT1 = intent.getStringExtra("min_temperature") ?: ""
        currentT2 = intent.getStringExtra("max_temperature") ?: ""

        // ИЗМЕНЕНО: Получаем Float значения (с поддержкой старых Int для обратной совместимости)
        currentT1Float = intent.getFloatExtra("t1Float", -1f)
        currentT2Float = intent.getFloatExtra("t2Float", -1f)
        currentM1Float = intent.getFloatExtra("m1Float", -1f)
        currentM2Float = intent.getFloatExtra("m2Float", -1f)

        // Для обратной совместимости: если Float не получены, пробуем получить Int и конвертировать
        if (currentT1Float == -1f) {
            val t1Int = intent.getIntExtra("t1Int", -1)
            if (t1Int != -1) currentT1Float = t1Int.toFloat()
        }
        if (currentT2Float == -1f) {
            val t2Int = intent.getIntExtra("t2Int", -1)
            if (t2Int != -1) currentT2Float = t2Int.toFloat()
        }
        if (currentM1Float == -1f) {
            val m1Int = intent.getIntExtra("m1Int", -1)
            if (m1Int != -1) currentM1Float = m1Int.toFloat()
        }
        if (currentM2Float == -1f) {
            val m2Int = intent.getIntExtra("m2Int", -1)
            if (m2Int != -1) currentM2Float = m2Int.toFloat()
        }

        // Преобразуем Float в String для отображения в EditText
        if (currentM1.isEmpty() && currentM1Float != -1f) {
            currentM1 = currentM1Float.toString()
        }
        if (currentM2.isEmpty() && currentM2Float != -1f) {
            currentM2 = currentM2Float.toString()
        }
        if (currentT1.isEmpty() && currentT1Float != -1f) {
            currentT1 = currentT1Float.toString()
        }
        if (currentT2.isEmpty() && currentT2Float != -1f) {
            currentT2 = currentT2Float.toString()
        }

        val plantName = intent.getStringExtra("PLANT_NAME") ?: "Неизвестное растение"
        val AirHumiditySwitchState = intent.getBooleanExtra("AirHumidityswitchState", false)
        val TempSwitchState = intent.getBooleanExtra("TempswitchState", false)

        // Инициализация UI элементов
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

        // Устанавливаем начальное состояние Switch
        AirHumiditySwitch.isChecked = AirHumiditySwitchState
        TempSwitch.isChecked = TempSwitchState

        // Заполняем EditText текущими значениями
        AirHumiditym1EditText.setText(currentM1)
        AirHumiditym2EditText.setText(currentM2)
        TempEditText1.setText(currentT1)
        TempEditText2.setText(currentT2)

        // Устанавливаем начальную видимость элементов
        updateAirHumidityVisibility(AirHumiditySwitch.isChecked)
        updateTempVisibility(TempSwitch.isChecked)

        // Обработчики для Switch
        AirHumiditySwitch.setOnCheckedChangeListener { _, isChecked ->
            updateAirHumidityVisibility(isChecked)
        }

        TempSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateTempVisibility(isChecked)
        }

        // Обработчики для кнопок сохранения
        AirHumidityButton.setOnClickListener {
            saveAirHumidityValues()
        }

        TempButton.setOnClickListener {
            saveTempValues()
        }

        // Обработчик кнопки "назад"
        vlevoButton.setOnClickListener {
            // Сохраняем текущие значения перед возвратом
            if (AirHumiditySwitch.isChecked) {
                saveAirHumidityValues()
            }
            if (TempSwitch.isChecked) {
                saveTempValues()
            }

            val intent = Intent()

            // Передаем данные обратно
            intent.putExtra("n1", n1)
            intent.putExtra("n2", n2)
           // intent.putExtra("wateringswitchState", wateringswitchState)
           // intent.putExtra("lightState", lightState)

            // Передаем строковые значения (для совместимости)
            intent.putExtra("m1", currentM1)
            intent.putExtra("m2", currentM2)
            intent.putExtra("t1", currentT1)
            intent.putExtra("t2", currentT2)

            // ИЗМЕНЕНО: Передаем Float значения
            intent.putExtra("t1Float", currentT1Float)
            intent.putExtra("t2Float", currentT2Float)
            intent.putExtra("m1Float", currentM1Float)
            intent.putExtra("m2Float", currentM2Float)

            // Также передаем для обратной совместимости Int значения
            intent.putExtra("t1Int", currentT1Float.toInt())
            intent.putExtra("t2Int", currentT2Float.toInt())
            intent.putExtra("m1Int", currentM1Float.toInt())
            intent.putExtra("m2Int", currentM2Float.toInt())

            // Передаем состояние Switch
            intent.putExtra("AirHumidityswitchState", AirHumiditySwitch.isChecked)
            intent.putExtra("TempswitchState", TempSwitch.isChecked)

            // Передаем название растения
            intent.putExtra("PLANT_NAME", plantName)

            // Передаем с новыми ключами (для MainActivityreadyscript)
            intent.putExtra("min_humidity", currentM1)
            intent.putExtra("max_humidity", currentM2)
            intent.putExtra("min_temperature", currentT1)
            intent.putExtra("max_temperature", currentT2)

            setResult(RESULT_OK, intent)
            finish() // Закрываем Activity
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Метод для обновления видимости элементов влажности
    private fun updateAirHumidityVisibility(isVisible: Boolean) {
        if (isVisible) {
            AirHumidityTextView.text = textAir
            AirHumiditym1TextView.text = textm1
            AirHumiditym2TextView.text = textm2
            AirHumiditym1TextView.visibility = View.VISIBLE
            AirHumiditym2TextView.visibility = View.VISIBLE
            AirHumidityButton.visibility = View.VISIBLE
            AirHumidityTextView.visibility = View.VISIBLE
            AirHumiditym1EditText.visibility = View.VISIBLE
            AirHumiditym2EditText.visibility = View.VISIBLE
        } else {
            AirHumidityTextView.visibility = View.GONE
            AirHumiditym1TextView.visibility = View.GONE
            AirHumiditym2TextView.visibility = View.GONE
            AirHumiditym1EditText.visibility = View.GONE
            AirHumiditym2EditText.visibility = View.GONE
            AirHumidityButton.visibility = View.GONE
        }
    }

    // Метод для обновления видимости элементов температуры
    private fun updateTempVisibility(isVisible: Boolean) {
        if (isVisible) {
            TempTextView.text = textAirTemp
            TempTextViewt1.text = textt1
            TempTextViewt2.text = textt2
            TempTextViewt1.visibility = View.VISIBLE
            TempTextViewt2.visibility = View.VISIBLE
            TempTextView.visibility = View.VISIBLE
            TempButton.visibility = View.VISIBLE
            TempEditText1.visibility = View.VISIBLE
            TempEditText2.visibility = View.VISIBLE
        } else {
            TempTextView.visibility = View.GONE
            TempTextViewt1.visibility = View.GONE
            TempTextViewt2.visibility = View.GONE
            TempEditText1.visibility = View.GONE
            TempEditText2.visibility = View.GONE
            TempButton.visibility = View.GONE
        }
    }

    // ИЗМЕНЕНО: Метод для сохранения значений влажности с Float
    private fun saveAirHumidityValues() {
        currentM1 = AirHumiditym1EditText.text.toString()
        currentM2 = AirHumiditym2EditText.text.toString()

        // ИЗМЕНЕНО: toFloatOrNull вместо toIntOrNull
        currentM1Float = currentM1.toFloatOrNull() ?: -1f
        currentM2Float = currentM2.toFloatOrNull() ?: -1f

        if (currentM1Float == -1f || currentM2Float == -1f) {
            Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Значения сохранены: m1=$currentM1Float, m2=$currentM2Float", Toast.LENGTH_SHORT).show()
        }
    }

    // ИЗМЕНЕНО: Метод для сохранения значений температуры с Float
    private fun saveTempValues() {
        currentT1 = TempEditText1.text.toString()
        currentT2 = TempEditText2.text.toString()

        // ИЗМЕНЕНО: toFloatOrNull вместо toIntOrNull
        currentT1Float = currentT1.toFloatOrNull() ?: -1f
        currentT2Float = currentT2.toFloatOrNull() ?: -1f

        if (currentT1Float == -1f || currentT2Float == -1f) {
            Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Значения сохранены: t1=$currentT1Float, t2=$currentT2Float", Toast.LENGTH_SHORT).show()
        }
    }
}