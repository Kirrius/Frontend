package com.example.plant_care

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivityInput : AppCompatActivity() {

    private lateinit var myWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_input)

        myWebView = findViewById(R.id.webview)

        // Включаем JavaScript (если нужно)
        myWebView.settings.javaScriptEnabled = true

        // Настраиваем клиент для обработки загрузки страниц
        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Проверяем, загрузилась ли страница
                if (url == "http://192.168.4.1/save") {
                    // Сохраняем флаг успешного подключения в SharedPreferences
                    val prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("isWifiConnected", true).apply()

                    // Переходим в главное меню с очисткой стека
                    val intent = Intent(this@MainActivityInput, MainActivitymenu::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
            }
        }

        // Загружаем страницу
        myWebView.loadUrl("http://192.168.4.1")
    }

    override fun onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack()
        } else {
            // При нажатии "Назад" также переходим в главное меню
            val intent = Intent(this, MainActivitymenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}