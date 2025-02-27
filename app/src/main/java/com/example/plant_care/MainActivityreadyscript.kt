package com.example.plant_care
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlin.jvm.java
import kotlin.collections.set
import kotlin.io.use
import kotlin.text.isNullOrEmpty
import android.util.Log
import com.google.gson.JsonSyntaxException

class MainActivityreadyscript : AppCompatActivity() {

    data class ServerResponse(
        val status: String,
        val message: Map<String, Any?>
    )

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var wateringSwitch: Switch
    private lateinit var wateringSwitchuv: Switch
    private lateinit var TempSwitch: Switch
    private lateinit var wateringTextView: TextView
    private lateinit var wateringTextViewuv: TextView
    private lateinit var TempTextView: TextView
    private lateinit var wateringn1TextView: TextView
    private lateinit var wateringn2TextView: TextView
    private lateinit var wateringnuv1TextView: TextView
    private lateinit var wateringnuv2TextView: TextView
    private lateinit var TempTextViewt1: TextView
    private lateinit var TempTextViewt2: TextView
    private lateinit var wateringEditText1: EditText
    private lateinit var wateringEditText2: EditText
    private lateinit var wateringEditTextuv1: EditText
    private lateinit var wateringEditTextuv2: EditText
    private lateinit var TempEditText1: EditText
    private lateinit var TempEditText2: EditText
    private lateinit var wateringnButton: Button
    private lateinit var wateringnuvButton: Button
    private lateinit var TempButton: Button
    private lateinit var saveButton: Button
    private lateinit var vpravoButton: ImageButton
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    val textwatering = "    Условия для полива:\n" +
            "   - Если уровень влажности почвы < n1%:\n" +
            "   - Включить насос.\n" +
            "   - Поливать до достижения уровня влажности почвы > n2%.\n" +
            "   - Выключить насос.\n"

    val textwateringuv = "    Условия для отправки уведомления:\n" +
            "   - Если уровень влажности почвы < n.1%:\n" +
            "   - Отправить уведомление.\n" +
            "   - Если уровень влажности почвы > n.2%.\n" +
            "   - Отправить уведомление.\n"

    val textn1 = "введите\nn1:"
    val textn2 = "введите\nn2:"
    val textnuv1 = "введите\nn.1:"
    val textnuv2 = "введит\nn.2:"
    private val REQUEST_CODE = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript)

        var n1 = intent.getStringExtra("n1") ?: "" // влажность почвы
        var n2 = intent.getStringExtra("n2") ?: ""
        var n1v = intent.getStringExtra("n1v") ?: ""// влажность почвы(уведомления)
        var n2v = intent.getStringExtra("n2v") ?: ""
        var m1 = intent.getStringExtra("m1") ?: ""// влажность воздуха
        var m2 = intent.getStringExtra("m2") ?: ""
        var t1 = intent.getStringExtra("t1") ?: "" // температура воздуха
        var t2 = intent.getStringExtra("t2") ?: ""
        var AirHumiditySwitchState = intent.getBooleanExtra("AirHumidityswitchState", false)
        var TempSwitchState = intent.getBooleanExtra("TempswitchState", false)
        var firebaseAuth = FirebaseAuth.getInstance()
        var currentUser: FirebaseUser? = firebaseAuth.currentUser
        var hostId = currentUser?.uid ?: "Пользователь не аутентифицирован"
      //  var hostPassword = intent.getStringExtra("Password")
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        var hostPassword = sharedPreferences.getString("Password", "default_value")
        var setting = mutableMapOf<String, Any?>()
        var minimumAirHumidity = intent.getIntExtra("m1Int", -1)
        var maximumAirHumidity = intent.getIntExtra("m2Int", -1)
        var minimumTemp = intent.getIntExtra("t1Int", -1)
        var maximumTemp = intent.getIntExtra("t2Int", -1)

        setting["minSoilMoist"] = "" // min влажность почвы
        setting["maxSoilMoist"] = "" // max влажность почвы
        setting["minNotifHumid"] = "" // min влажность почвы(уведомления)
        setting["maxNotifHumid"] = "" // max влажность почвы(уведомления)
        setting["hostId"] = hostId
        if (hostPassword != null) {
            // Теперь вы можете безопасно использовать hostPassword как String
            setting["hostPassword"] = hostPassword
        }
        if(minimumAirHumidity != -1){
            setting["minAirHumid"] = minimumAirHumidity // влажность воздуха
        }
        if(minimumAirHumidity != -1){
            setting["maxAirHumid"] = minimumAirHumidity // влажность воздуха
        }
        if(minimumTemp != - 1){
            setting["minAirTemper"] = minimumTemp // температура воздуха
        }
        if(maximumTemp != - 1){
            setting["maxAirTemper"] = maximumTemp // температура воздуха
        }

        wateringSwitch = findViewById(R.id.switch2)
        wateringSwitchuv = findViewById(R.id.switch3)
        wateringTextView = findViewById(R.id.textView13)
        wateringTextViewuv = findViewById(R.id.textView10)
        wateringn1TextView = findViewById(R.id.textView6)
        wateringn2TextView = findViewById(R.id.textView9)
        wateringnuv1TextView = findViewById(R.id.textView11)
        wateringnuv2TextView = findViewById(R.id.textView14)
        wateringEditText1 = findViewById(R.id.editTextText2)
        wateringEditText2 = findViewById(R.id.editTextText3)
        wateringEditTextuv1 = findViewById(R.id.editTextText4)
        wateringEditTextuv2 = findViewById(R.id.editTextText6)
        wateringnButton = findViewById(R.id.button5)
        wateringnuvButton = findViewById(R.id.button7)
        saveButton = findViewById(R.id.button6)
        vpravoButton= findViewById(R.id.imageButton9)

        wateringSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wateringTextView.text = textwatering // Устанавливаем текст при включении
                wateringn1TextView.text = textn1
                wateringn2TextView.text = textn2
                wateringn1TextView.visibility = View.VISIBLE
                wateringn2TextView.visibility = View.VISIBLE
                wateringnButton.visibility = View.VISIBLE // Показываем кнопку
                wateringTextView.visibility = View.VISIBLE // Показываем текст
                wateringEditText1.visibility = View.VISIBLE // Показываем EditText
                wateringEditText2.visibility = View.VISIBLE // Показываем EditText
                wateringnButton.setOnClickListener {
                    n1 = wateringEditText1.text.toString()
                    n2 = wateringEditText2.text.toString()
                    var n1Int = n1.toIntOrNull()
                    var n2Int = n2.toIntOrNull()

                    if(n1Int != null && n2Int != null){
                        setting["minSoilMoist"] = n1Int
                        setting["maxSoilMoist"] = n2Int
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                wateringTextView.visibility = View.GONE // Скрываем текст
                wateringn1TextView.visibility = View.GONE // Скрываем текст
                wateringn2TextView.visibility = View.GONE // Скрываем текст
                wateringEditText1.visibility = View.GONE // Скрываем EditText
                wateringEditText2.visibility = View.GONE // Скрываем EditText
                wateringnButton.visibility = View.GONE // Скрываем кнопку
            }
        }

        wateringSwitchuv.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wateringTextViewuv.text = textwateringuv // Устанавливаем текст при включении
                wateringnuv1TextView.text = textnuv1
                wateringnuv2TextView.text = textnuv2
                wateringnuv1TextView.visibility = View.VISIBLE
                wateringnuv2TextView.visibility = View.VISIBLE
                wateringnuvButton.visibility = View.VISIBLE // Показываем кнопку
                wateringTextViewuv.visibility = View.VISIBLE // Показываем текст
                wateringEditTextuv1.visibility = View.VISIBLE // Показываем EditText
                wateringEditTextuv2.visibility = View.VISIBLE // Показываем EditText
                wateringnuvButton.setOnClickListener {
                    n1v = wateringEditTextuv1.text.toString()
                    n2v = wateringEditTextuv2.text.toString()
                    var nv1Int = n1v.toIntOrNull()
                    var nv2Int = n2v.toIntOrNull()
                    if (nv1Int != null && nv2Int != null){
                        setting["minNotifHumid"] = nv1Int
                        setting["maxNotifHumid"] = nv2Int
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите корректные значения", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                wateringTextViewuv.visibility = View.GONE
                wateringnuv1TextView.visibility = View.GONE
                wateringnuv2TextView.visibility = View.GONE
                wateringTextViewuv.visibility = View.GONE
                wateringnuvButton.visibility = View.GONE
                wateringEditTextuv1.visibility = View.GONE
                wateringEditTextuv2.visibility = View.GONE
            }
        }

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                AirHumiditySwitchState = data?.getBooleanExtra("AirHumidityswitchState", false) ?: false
                TempSwitchState = data?.getBooleanExtra("TempswitchState", false) ?: false
                var wateringswitchState = data?.getBooleanExtra("wateringswitchState", false) ?: false
                var wateringswitchuvState = data?.getBooleanExtra("wateringswitchuvState", false) ?: false
                minimumAirHumidity = data?.getIntExtra("m1Int", -1) ?: -1
                maximumAirHumidity = data?.getIntExtra("m2Int", -1) ?: -1
                minimumTemp = data?.getIntExtra("t1Int", -1) ?: -1
                maximumTemp = data?.getIntExtra("t2Int", -1) ?: -1
                n1 = data?.getStringExtra("n1") ?: ""
                n2 = data?.getStringExtra("n2") ?: ""
                n1v = data?.getStringExtra("n1v") ?: ""
                n2v = data?.getStringExtra("n2v") ?: ""
                m1 = data?.getStringExtra("m1") ?: ""
                m2 = data?.getStringExtra("m2") ?: ""
                t1 = data?.getStringExtra("t1") ?: ""
                t2 = data?.getStringExtra("t2") ?: ""
            }
        }

        saveButton.setOnClickListener {
            if (wateringSwitch.isChecked) {
                n1 = wateringEditText1.text.toString()
                n2 = wateringEditText2.text.toString()

                var n1Int = n1.toIntOrNull()
                var n2Int = n2.toIntOrNull()

                if (n1Int != null && n2Int != null) {
                    setting["minSoilMoist"] = n1Int
                    setting["maxSoilMoist"] = n2Int
                }
                else {
                    Toast.makeText(this, "Пожалуйста, введите значения для полива", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else {
                setting["minSoilMoist"] = 1000
                setting["maxSoilMoist"] = 1000
            }
            if (wateringSwitchuv.isChecked){
                n1v = wateringEditTextuv1.text.toString()
                n2v = wateringEditTextuv2.text.toString()

                var nv1Int = n1v.toIntOrNull()
                var nv2Int = n2v.toIntOrNull()

                if(nv1Int != null && nv2Int != null){
                    setting["minNotifHumid"] = nv1Int
                    setting["maxNotifHumid"] = nv2Int
                }
                else {
                    Toast.makeText(this, "Пожалуйста, введите значения для полива", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else {
                setting["minNotifHumid"] = 1000
                setting["maxNotifHumid"] = 1000
            }
            if(AirHumiditySwitchState){
                if(minimumAirHumidity != -1 && maximumAirHumidity != -1){
                    setting["minAirHumid"] = minimumAirHumidity
                    setting["maxAirHumid"] = maximumAirHumidity
                }
                else {
                    Toast.makeText(this, "Введите значения для влажности воздуха", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else{
                setting["minAirHumid"] = 1000
                setting["maxAirHumid"] = 1000
            }
            if(TempSwitchState){
                if(minimumTemp != -1 && maximumTemp != -1){
                    setting["minAirTemper"] = minimumTemp
                    setting["maxAirTemper"] = maximumTemp
                }
                else {
                    Toast.makeText(this, "Введите значения для температуры воздуха", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else {
                setting["minAirTemper"] = 1000
                setting["maxAirTemper"] = 1000
            }
            setting.forEach { (key, value) ->
                Log.d("Settings", "$key: $value")
            }
            postRequest(setting)
        }

        vpravoButton.setOnClickListener{
            var intent = Intent(this, MainActivityreadyscript2::class.java)
            intent.putExtra("wateringswitchState", wateringSwitch.isChecked)
            intent.putExtra("wateringswitchuvState", wateringSwitchuv.isChecked)
            intent.putExtra("AirHumidityswitchState", AirHumiditySwitchState) // Передаем состояние свитча
            intent.putExtra("TempswitchState", TempSwitchState)
            intent.putExtra("n1", n1)
            intent.putExtra("n2", n2)
            intent.putExtra("n1v", n1v)
            intent.putExtra("n2v", n2v)
            intent.putExtra("m1", m1)
            intent.putExtra("m2", m2)
            intent.putExtra("t1", t1)
            intent.putExtra("t2", t2)
            intent.putExtra("m1Int", minimumAirHumidity)
            intent.putExtra("m2Int", maximumAirHumidity)
            intent.putExtra("t1Int", minimumTemp)
            intent.putExtra("t2Int", maximumTemp)
            activityResultLauncher.launch(intent) // Запускаем вторую активность
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun postRequest(setting: Map<String, Any?>) {
        val client = OkHttpClient()
        val gson = Gson()

        // Сериализация словаря в JSON
        val json = gson.toJson(setting)

        // Создание тела запроса
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        // Создание запроса
        val request = Request.Builder()
            .url("https://www.plantsystem.ru/createScript") // Замените на ваш URL
            .post(requestBody)
            .build()

        // Выполнение запроса
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // Обработка ошибки
                Log.e("POST_REQUEST", "Ошибка при отправке запроса: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("POST_REQUEST", "Ошибка от сервера: ${response.code} - ${response.message}")
                        throw IOException("Unexpected code $response")
                    }

                    // Получаем строку ответа
                    val responseData = response.body?.string() ?: ""

                    // Если ответ пустой
                    if (responseData.isEmpty()) {
                        Log.e("POST_REQUEST", "Ответ пустой")
                        return
                    }

                    // Обрабатываем ответ с кодом 500
                    if (response.code == 500) {
                        try {
                            val errorResponse = gson.fromJson(responseData, ErrorResponse::class.java)
                            Log.e("POST_REQUEST", "Ошибка на сервере: ${errorResponse.error} - ${errorResponse.details}")
                        } catch (e: JsonSyntaxException) {
                            Log.e("POST_REQUEST", "Ошибка при парсинге ошибки: ${e.message}")
                        }
                    }

                    // Обрабатываем ответ с кодом 201
                    if (response.code == 201) {
                        try {
                            // Парсим ответ с двумя фрагментами (JSON и целое число)
                            val tupleResponse = gson.fromJson(responseData, TupleResponse::class.java)

                            // Доступ к данным в ответе
                            Log.d("POST_REQUEST", "Запрос успешно выполнен, ответ: ${tupleResponse.data.message}, script id: ${tupleResponse.data.scriptId}")
                            Log.d("POST_REQUEST", "Status code: ${tupleResponse.statusCode}")
                        } catch (e: JsonSyntaxException) {
                            Log.e("POST_REQUEST", "Ошибка при парсинге успешного ответа: ${e.message}")
                        }
                    }
                }
            }
        })
    }

// Классы для парсинга ответов от сервера

    // Класс для вложенного объекта данных
    data class DataResponse(
        val message: String,
        val scriptId: String // или другой тип, в зависимости от структуры данных
    )

    // Класс для полного ответа от сервера
    data class TupleResponse(
        val data: DataResponse,  // Вложенный объект с данными
        val statusCode: Int      // Статусный код (целое число)
    )

    // Класс для обработки ошибок (если нужно)
    data class ErrorResponse(
        val error: String,
        val details: String
    )
}