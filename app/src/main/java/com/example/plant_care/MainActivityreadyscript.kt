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

class MainActivityreadyscript : AppCompatActivity() {

    data class ServerResponse(
        val status: String,
        val message: Map<String, Any?>
    )

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var wateringSwitch: Switch
    private lateinit var wateringSwitchuv: Switch
    private lateinit var wateringTextView: TextView
    private lateinit var wateringTextViewuv: TextView
    private lateinit var wateringn1TextView: TextView
    private lateinit var wateringn2TextView: TextView
    private lateinit var wateringnuv1TextView: TextView
    private lateinit var wateringnuv2TextView: TextView
    private lateinit var wateringEditText1: EditText
    private lateinit var wateringEditText2: EditText
    private lateinit var wateringEditTextuv1: EditText
    private lateinit var wateringEditTextuv2: EditText
    private lateinit var wateringnButton: Button
    private lateinit var wateringnuvButton: Button
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

    val textn1 = "введите n1:"
    val textn2 = "введите n2:"
    val textnuv1 = "введите n.1:"
    val textnuv2 = "введит n.2:"
    private val REQUEST_CODE = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activityreadyscript)

        var n1 = "" // влажность почвы
        var n2 = ""
        var n1v = ""// влажность почвы(уведомления)
        var n2v = ""
        var m1Int = -1 // влажность воздуха
        var m2Int = -1
        var firebaseAuth = FirebaseAuth.getInstance()
        var currentUser: FirebaseUser? = firebaseAuth.currentUser
        var hostId = currentUser?.uid ?: "Пользователь не аутентифицирован"
        var hostPassword = intent.getStringExtra("Password")
        var setting = mutableMapOf<String, Any?>()
        var minimumAirHumidity = intent.getStringExtra("m1Int")
        var maximumAirHumidity = intent.getStringExtra("m2Int")
        setting["minimumSoilMoisture"] = "" // min влажность почвы
        setting["maximumSoilMoisture"] = "" // max влажность почвы
        setting["minimumNotificationHumidity"] = "" // min влажность почвы(уведомления)
        setting["maximumNotificationHumidity"] = "" // max влажность почвы(уведомления)
        setting["hostId"] = hostId
        if (hostPassword != null) {
            // Теперь вы можете безопасно использовать hostPassword как String
            setting["hostPassword"] = hostPassword
        }
        if(minimumAirHumidity != null){
            setting["minimumAirHumidity"] = minimumAirHumidity // влажность воздуха
        }
        if(minimumAirHumidity != null){
            setting["maximumAirHumidity"] = minimumAirHumidity // влажность воздуха
        }
        setting["minimumAirHumidity"] = minimumAirHumidity // влажность воздуха
        setting["maximumAirHumidity"] = maximumAirHumidity
        setting["minimumAirTemperature"] = 0 // темпераутра воздуха
        setting["maximumAirTemperature"] = 0

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
            if (wateringSwitch.isChecked) {
                wateringTextView.text = textwatering // Устанавливаем текст при включении
                wateringn1TextView.text = textn1
                wateringn2TextView.text = textn2
                wateringn1TextView.visibility = View.VISIBLE
                wateringn2TextView.visibility = View.VISIBLE
                wateringnButton.visibility = View.VISIBLE // Показываем кнопку
                wateringTextView.visibility = View.VISIBLE // Показываем текст
                wateringEditText1.visibility = View.VISIBLE // Показываем EditText
                wateringEditText2.visibility = View.VISIBLE // Показываем EditText
                wateringEditText1.setText(n1)
                wateringEditText2.setText(n2)
                wateringnButton.setOnClickListener {
                    n1 = wateringEditText1.text.toString()
                    n2 = wateringEditText2.text.toString()
                    var n1Int = n1.toIntOrNull()
                    var n2Int = n2.toIntOrNull()

                    if(n1Int != null && n2Int != null){
                        setting["minimumSoilMoisture"] = n1Int
                        setting["maximumSoilMoisture"] = n2Int
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
                        setting["minimumNotificationHumidity"] = nv1Int
                        setting["maximumNotificationHumidity"] = nv2Int
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
                var wateringswitchState = data?.getBooleanExtra("wateringswitchState", false) ?: false
                wateringSwitch.isChecked = wateringswitchState
                var wateringswitchuvState = data?.getBooleanExtra("wateringswitchuvState", false) ?: false
                n1 = data?.getStringExtra("n1") ?: ""
                n2 = data?.getStringExtra("n2") ?: ""
                n1v = data?.getStringExtra("n1v") ?: ""
                n2v = data?.getStringExtra("n2v") ?: ""
            }
        }

        saveButton.setOnClickListener {
            if (wateringSwitch.isChecked) {
                n1 = wateringEditText1.text.toString()
                n2 = wateringEditText2.text.toString()

                var n1Int = n1.toIntOrNull()
                var n2Int = n2.toIntOrNull()

                if (n1Int != null && n2Int != null) {
                    setting["minimumSoilMoisture"] = n1Int
                    setting["maximumSoilMoisture"] = n2Int
                }
                else {
                    Toast.makeText(this, "Пожалуйста, введите значения для полива", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else {
                setting["minimumSoilMoisture"] = 1000
                setting["maximumSoilMoisture"] = 1000
            }
            if (wateringSwitchuv.isChecked){
                n1v = wateringEditTextuv1.text.toString()
                n2v = wateringEditTextuv2.text.toString()

                var nv1Int = n1v.toIntOrNull()
                var nv2Int = n2v.toIntOrNull()

                if(nv1Int != null && nv2Int != null){
                    setting["minimumNotificationHumidity"] = nv1Int
                    setting["maximumNotificationHumidity"] = nv2Int
                }
                else {
                    Toast.makeText(this, "Пожалуйста, введите значения для полива", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            else {
                setting["minimumNotificationHumidity"] = 1000
                setting["maximumNotificationHumidity"] = 1000
            }
            postRequest(setting)
        }

        vpravoButton.setOnClickListener{
            var intent = Intent(this, MainActivityreadyscript2::class.java)

            intent.putExtra("wateringswitchState", wateringSwitch.isChecked)
            intent.putExtra("wateringswitchuvState", wateringSwitchuv.isChecked)
            intent.putExtra("n1", n1)
            intent.putExtra("n2", n2)
            intent.putExtra("n1v", n1v)
            intent.putExtra("n2v", n2v)
            intent.putExtra("m1", m1Int)
            intent.putExtra("m2", m2Int)
            activityResultLauncher.launch(intent) // Запускаем вторую активность
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    /*
    fun updateInterfaceVisibility(setting: MutableMap<String, Any?>,isChecked: Boolean, n1: String, n2: String) {
        if (isChecked) {
            wateringTextView.text = textwatering // Устанавливаем текст при включении
            wateringn1TextView.text = textn1
            wateringn2TextView.text = textn2
            wateringn1TextView.visibility = View.VISIBLE
            wateringn2TextView.visibility = View.VISIBLE
            wateringnButton.visibility = View.VISIBLE // Показываем кнопку
            wateringTextView.visibility = View.VISIBLE // Показываем текст
            wateringEditText1.visibility = View.VISIBLE // Показываем EditText
            wateringEditText1.setText(n1)
            wateringEditText2.visibility = View.VISIBLE // Показываем EditText
            wateringEditText2.setText(n2)
            return
        }
        return
    }
     */


    private fun postRequest(setting: Map<String, Any?>) { // Указываем, что setting — это словарь
        val client = OkHttpClient()
        val gson = Gson()

        // Сериализация словаря в JSON
        val json = gson.toJson(setting)

        // Создание тела запроса
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        // Создание запроса
        val request = Request.Builder()
            .url("https://yourapi.com/endpoint") // Замените на ваш URL
            .post(requestBody)
            .build()

        // Выполнение запроса
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // Обработка ошибки
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    // Обработка успешного ответа
                    val responseData = response.body?.string()
                    val serverResponse = gson.fromJson(responseData, ServerResponse::class.java)

                }
            }
        })
    }
}