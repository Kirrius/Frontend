package com.example.plant_care

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private val textexit = "Пожалуйста, подключитесь к Wi-Fi 'ESP32_AP'"

    private val TAG = "NetworkCheck"
    private lateinit var wifiManager: WifiManager

    // Диапазон IP (192.168.4.x)
    private val esp32IpRangeStart = 33859712 // 192.168.4.0
    private val esp32IpRangeEnd = 33859839   // 192.168.4.127

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Сначала проверим, залогинен ли пользователь
        val prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val skip = intent.getBooleanExtra("skipLoginCheck", false)
        Log.d(TAG, "onCreate: isLoggedIn=$isLoggedIn skip=$skip")

        if (!isLoggedIn && !skip) {
            // Перенаправляем на экран логина
            val loginIntent = Intent(this, MainActivityLogin::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
            return
        }

        // Проверяем, было ли уже успешное подключение к Wi-Fi ESP32
        val isWifiConnected = prefs.getBoolean("isWifiConnected", false)

        if (isWifiConnected) {
            // Если было успешное подключение - сразу переходим в главное меню
            val menuIntent = Intent(this, MainActivitymenu::class.java)
            menuIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(menuIntent)
            finish()
            return
        }

        // Если успешного подключения еще не было, проверяем текущее подключение
        wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (isConnectedToEsp32()) {
            // Если подключены к ESP32 - переходим в MainActivityInput для настройки
            startActivity(Intent(this, MainActivityInput::class.java))
            finish()
        } else {
            // Если не подключены - показываем экран подключения
            setContentView(R.layout.activity_main)
            textView = findViewById(R.id.textView20)
            textView.visibility = View.GONE
        }
    }

    private fun isConnectedToEsp32(): Boolean {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            val currentIp = wifiInfo.ipAddress
            Log.d(TAG, "Raw IP: $currentIp")
            currentIp in esp32IpRangeStart..esp32IpRangeEnd
        } catch (e: Exception) {
            Log.e(TAG, "Network check error", e)
            false
        }
    }

    fun startMenu(v: View) {
        if (isConnectedToEsp32()) {
            textView.visibility = View.GONE
            startActivity(Intent(this, MainActivityInput::class.java))
            finish()
        } else {
            textView.text = textexit
            textView.visibility = View.VISIBLE
        }
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }
}