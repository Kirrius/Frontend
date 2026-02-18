package com.example.plant_care

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class MainActivityreadyscript : AppCompatActivity() {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var wateringSwitch: Switch
    private lateinit var lightSwitch: Switch
    private lateinit var TempSwitch: Switch
    private lateinit var wateringTextView: TextView
    private lateinit var lightTextView: TextView
    private lateinit var TempTextView: TextView
    private lateinit var wateringn1TextView: TextView
    private lateinit var wateringn2TextView: TextView
    private lateinit var light1TextView: TextView
    private lateinit var light2TextView: TextView
    private lateinit var TempTextViewt1: TextView
    private lateinit var TempTextViewt2: TextView
    private lateinit var wateringEditText1: EditText
    private lateinit var wateringEditText2: EditText
    private lateinit var lightEditText1: EditText
    private lateinit var lightEditText2: EditText
    private lateinit var wateringnButton: Button
    private lateinit var lightButton: Button
    private lateinit var saveButton: Button
    private lateinit var vpravoButton: ImageButton
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private val textwatering = "    Условия для полива:\n" +
            "   - Если уровень влажности почвы < n1%:\n" +
            "   - Включить насос.\n" +
            "   - Поливать до достижения уровня влажности почвы > n2%.\n" +
            "   - Выключить насос.\n"

    private val textlight = "    Условия для освещения:\n" +
            "   - Если уровень освещенности < l1 люкс:\n" +
            "   - Включить фитолампу.\n" +
            "   - Досвечивать до достижения уровня > l2 люкс.\n" +
            "   - Выключить фитолампу.\n"

    private val textn1 = "введите\nn1:"
    private val textn2 = "введите\nn2:"

    private val textl1 = "введите\nl1:"
    private val textl2 = "введите\nl2:"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript)

        // Получаем название растения из предыдущего экрана
        val plantName = intent.getStringExtra("PLANT_NAME") ?: "Неизвестное растение"

        // Получаем данные из Intent
        var n1 = intent.getStringExtra("n1") ?: "" // влажность почвы min
        var n2 = intent.getStringExtra("n2") ?: "" // влажность почвы max
        var m1 = intent.getStringExtra("m1") ?: "" // влажность воздуха min
        var m2 = intent.getStringExtra("m2") ?: "" // влажность воздуха max
        var t1 = intent.getStringExtra("t1") ?: "" // температура воздуха min
        var t2 = intent.getStringExtra("t2") ?: "" // температура воздуха max
        var l1 = intent.getStringExtra("l1") ?: "" // освещённость min
        var l2 = intent.getStringExtra("l2") ?: "" // освещённость max
        var AirHumiditySwitchState = intent.getBooleanExtra("AirHumidityswitchState", false)
        var TempSwitchState = intent.getBooleanExtra("TempswitchState", false)

        // ИЗМЕНЕНО: Используем Float вместо Int
        var minimumAirHumidity = intent.getFloatExtra("m1Float", -1f)
        var maximumAirHumidity = intent.getFloatExtra("m2Float", -1f)
        var minimumTemp = intent.getFloatExtra("t1Float", -1f)
        var maximumTemp = intent.getFloatExtra("t2Float", -1f)

        // Инициализация UI элементов
        wateringSwitch = findViewById(R.id.switch2)
        lightSwitch = findViewById(R.id.switch3)
        wateringTextView = findViewById(R.id.textView13)
        lightTextView = findViewById(R.id.textView10)
        wateringn1TextView = findViewById(R.id.textView6)
        wateringn2TextView = findViewById(R.id.textView9)
        light1TextView = findViewById(R.id.textView11)
        light2TextView = findViewById(R.id.textView14)
        wateringEditText1 = findViewById(R.id.editTextText2)
        wateringEditText2 = findViewById(R.id.editTextText3)
        lightEditText1 = findViewById(R.id.editTextText4)
        lightEditText2 = findViewById(R.id.editTextText6)
        wateringnButton = findViewById(R.id.button5)
        lightButton = findViewById(R.id.button7)
        saveButton = findViewById(R.id.button6)
        vpravoButton = findViewById(R.id.imageButton9)

        // Заполняем поля значениями из Intent (если они есть)
        if (n1.isNotEmpty()) wateringEditText1.setText(n1)
        if (n2.isNotEmpty()) wateringEditText2.setText(n2)
        if (l1.isNotEmpty()) lightEditText1.setText(l1) // НОВЫЙ
        if (l2.isNotEmpty()) lightEditText2.setText(l2) // НОВЫЙ

        // Настройка переключателя полива
        wateringSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wateringTextView.text = textwatering
                wateringn1TextView.text = textn1
                wateringn2TextView.text = textn2
                wateringn1TextView.visibility = View.VISIBLE
                wateringn2TextView.visibility = View.VISIBLE
                wateringnButton.visibility = View.VISIBLE
                wateringTextView.visibility = View.VISIBLE
                wateringEditText1.visibility = View.VISIBLE
                wateringEditText2.visibility = View.VISIBLE

                wateringnButton.setOnClickListener {
                    n1 = wateringEditText1.text.toString()
                    n2 = wateringEditText2.text.toString()
                    // ИЗМЕНЕНО: toFloatOrNull вместо toIntOrNull
                    val n1Float = n1.toFloatOrNull()
                    val n2Float = n2.toFloatOrNull()

                    if (n1Float != null && n2Float != null) {
                        Toast.makeText(this, "Значения сохранены: n1=$n1Float, n2=$n2Float", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                wateringTextView.visibility = View.GONE
                wateringn1TextView.visibility = View.GONE
                wateringn2TextView.visibility = View.GONE
                wateringEditText1.visibility = View.GONE
                wateringEditText2.visibility = View.GONE
                wateringnButton.visibility = View.GONE
            }
        }



        // Настройка переключателя освещённости
        lightSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                lightTextView.text = textlight
                light1TextView.text = textl1
                light2TextView.text = textl2
                light1TextView.visibility = View.VISIBLE
                light2TextView.visibility = View.VISIBLE
                lightButton.visibility = View.VISIBLE
                lightTextView.visibility = View.VISIBLE
                lightEditText1.visibility = View.VISIBLE
                lightEditText2.visibility = View.VISIBLE

                lightButton.setOnClickListener {
                    l1 = lightEditText1.text.toString()
                    l2 = lightEditText2.text.toString()
                    // ИЗМЕНЕНО: toFloatOrNull вместо toIntOrNull
                    val l1Float = l1.toFloatOrNull()
                    val l2Float = l2.toFloatOrNull()

                    if (l1Float != null && l2Float != null) {
                        Toast.makeText(this, "Значения сохранены: l1=$l1Float, l2=$l2Float", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                lightTextView.visibility = View.GONE
                light1TextView.visibility = View.GONE
                light2TextView.visibility = View.GONE
                lightEditText1.visibility = View.GONE
                lightEditText2.visibility = View.GONE
                lightButton.visibility = View.GONE
            }
        }



        // Настройка ActivityResultLauncher для получения данных из следующего Activity
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                AirHumiditySwitchState = data?.getBooleanExtra("AirHumidityswitchState", false) ?: false
                TempSwitchState = data?.getBooleanExtra("TempswitchState", false) ?: false
                // ИЗМЕНЕНО: getFloatExtra вместо getIntExtra
                minimumAirHumidity = data?.getFloatExtra("m1Float", -1f) ?: -1f
                maximumAirHumidity = data?.getFloatExtra("m2Float", -1f) ?: -1f
                minimumTemp = data?.getFloatExtra("t1Float", -1f) ?: -1f
                maximumTemp = data?.getFloatExtra("t2Float", -1f) ?: -1f
                n1 = data?.getStringExtra("n1") ?: ""
                n2 = data?.getStringExtra("n2") ?: ""
                m1 = data?.getStringExtra("m1") ?: ""
                m2 = data?.getStringExtra("m2") ?: ""
                t1 = data?.getStringExtra("t1") ?: ""
                t2 = data?.getStringExtra("t2") ?: ""
                l1 = data?.getStringExtra("l1") ?: ""
                l2 = data?.getStringExtra("l2") ?: ""

                // Обновляем поля, если значения пришли
                if (n1.isNotEmpty()) wateringEditText1.setText(n1)
                if (n2.isNotEmpty()) wateringEditText2.setText(n2)
                if (l1.isNotEmpty()) lightEditText1.setText(l1)
                if (l2.isNotEmpty()) lightEditText2.setText(l2)
            }
        }

        // Обработка кнопки сохранения
        saveButton.setOnClickListener {
            // Создаем Map с данными для отправки на сервер
            val serverData = mutableMapOf<String, Any?>()

            // Добавляем название растения
            serverData["nam"] = plantName

            // Обработка полива (влажность почвы)
            if (wateringSwitch.isChecked) {
                n1 = wateringEditText1.text.toString()
                n2 = wateringEditText2.text.toString()

                // ИЗМЕНЕНО: toFloatOrNull вместо toIntOrNull
                val n1Float = n1.toFloatOrNull()
                val n2Float = n2.toFloatOrNull()

                if (n1Float != null && n2Float != null) {
                    serverData["min_soil_moisture"] = n1Float  // Float вместо Int
                    serverData["max_soil_moisture"] = n2Float  // Float вместо Int
                } else {
                    Toast.makeText(this, "Пожалуйста, введите значения для полива", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                // Используем 1000.0f как маркер "параметр не используется"
                serverData["min_soil_moisture"] = 1000.0f  // Float вместо Int
                serverData["max_soil_moisture"] = 1000.0f  // Float вместо Int
            }

            // Обработка влажности воздуха
            if (AirHumiditySwitchState) {
                // ИЗМЕНЕНО: Проверка на -1f вместо -1
                if (minimumAirHumidity != -1f && maximumAirHumidity != -1f) {
                    serverData["min_humidity"] = minimumAirHumidity  // Float
                    serverData["max_humidity"] = maximumAirHumidity  // Float
                } else {
                    Toast.makeText(this, "Введите значения для влажности воздуха", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                serverData["min_humidity"] = 1000.0f  // Float
                serverData["max_humidity"] = 1000.0f  // Float
            }

            // Обработка температуры воздуха
            if (TempSwitchState) {
                // ИЗМЕНЕНО: Проверка на -1f вместо -1
                if (minimumTemp != -1f && maximumTemp != -1f) {
                    serverData["min_temperature"] = minimumTemp  // Float
                    serverData["max_temperature"] = maximumTemp  // Float
                } else {
                    Toast.makeText(this, "Введите значения для температуры воздуха", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                serverData["min_temperature"] = 1000.0f  // Float
                serverData["max_temperature"] = 1000.0f  // Float
            }

            // Обработка освещенности
            if (lightSwitch.isChecked) {
                l1 = lightEditText1.text.toString()
                l2 = lightEditText2.text.toString()

                val l1Float = l1.toFloatOrNull()
                val l2Float = l2.toFloatOrNull()

                if (l1Float != null && l2Float != null) {
                    serverData["min_light_lux"] = l1Float
                    serverData["max_light_lux"] = l2Float
                } else {
                    Toast.makeText(this, "Пожалуйста, введите значения для освещенности", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                serverData["min_light_lux"] = 1000.0f
                serverData["max_light_lux"] = 1000.0f
            }

            // Логируем данные для отправки
            serverData.forEach { (key, value) ->
                Log.d("ServerData", "$key: $value (тип: ${value?.javaClass?.simpleName})")
            }

            // Отправляем данные на сервер
            postRequest(serverData)
        }

        // Обработка кнопки перехода вправо
        vpravoButton.setOnClickListener {
            val intent = Intent(this, MainActivityreadyscript2::class.java)
            // Передаем текущие данные в следующее Activity

            // Для обратной совместимости оставляем старые ключи
            intent.putExtra("wateringswitchState", wateringSwitch.isChecked)
            intent.putExtra("AirHumidityswitchState", AirHumiditySwitchState)
            intent.putExtra("TempswitchState", TempSwitchState)
            intent.putExtra("min_soil_moisture", n1)
            intent.putExtra("max_soil_moisture", n2)
            intent.putExtra("min_humidity", m1)
            intent.putExtra("max_humidity", m2)
            intent.putExtra("min_temperature", t1)
            intent.putExtra("max_temperature", t2)
            intent.putExtra("min_light_lux", l1)
            intent.putExtra("max_light_lux", l2)

            // ИЗМЕНЕНО: Передаем Float значения с новыми ключами
            intent.putExtra("m1Float", minimumAirHumidity)
            intent.putExtra("m2Float", maximumAirHumidity)
            intent.putExtra("t1Float", minimumTemp)
            intent.putExtra("t2Float", maximumTemp)

            // Также передаем название растения
            intent.putExtra("PLANT_NAME", plantName)

            activityResultLauncher.launch(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun postRequest(data: Map<String, Any?>) {
        val client = OkHttpClient()
        val gson = Gson()

        // 1. Получаем сохраненный email
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", "") ?: ""

        Log.d("AUTH_INFO", "Используем email: $email")

        // 2. Создаем полный JSON с email
        val fullData = mutableMapOf<String, Any?>()
        fullData.putAll(data)

        // Добавляем только email
        fullData["username"] = email  // Важно: используем "username" как в сервере

        // 3. Сериализация данных в JSON
        val json = gson.toJson(fullData)
        Log.d("POST_REQUEST", "Отправляемые данные: $json")

        // 4. Создание запроса
        val request = Request.Builder()
            .url("http://192.168.1.107:5000/api/scenarios")
            .post(RequestBody.create("application/json; charset=utf-8".toMediaType(), json))
            .addHeader("Content-Type", "application/json")
            .build()

        // 5. Выполнение запроса
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("POST_REQUEST", "Ошибка: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivityreadyscript,
                        "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: ""
                Log.d("POST_REQUEST", "Ответ: код=${response.code}, тело=$responseData")

                runOnUiThread {
                    when (response.code) {
                        201 -> {
                            try {
                                // Новый формат ответа
                                val jsonObj = JSONObject(responseData)
                                if (jsonObj.getBoolean("success")) {
                                    // Сразу переходим без задержки
                                    val intent = Intent(this@MainActivityreadyscript, MainActivitymenu::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivityreadyscript,
                                    "Сценарий создан, но ошибка парсинга", Toast.LENGTH_SHORT).show()
                                // Даже при ошибке парсинга переходим
                                val intent = Intent(this@MainActivityreadyscript, MainActivitymenu::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                        401 -> {
                            Toast.makeText(this@MainActivityreadyscript,
                                "Ошибка авторизации. Проверьте email.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this@MainActivityreadyscript,
                                "Ошибка сервера: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}