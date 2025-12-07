package com.example.plant_care

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

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
                    startActivity(Intent(this@MainActivityInput, MainActivitymenu::class.java))
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
            super.onBackPressed()
        }
    }
}