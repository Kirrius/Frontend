package com.example.plant_care

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivitymenu : AppCompatActivity() {

    private lateinit var scenariosRecyclerView: RecyclerView
    private lateinit var emptyListTextView: TextView
    private lateinit var adapter: ScenariosAdapter
    private val scenariosList = mutableListOf<UserScenario>()

    companion object {
        const val BASE_URL = "https://plant-care.up.railway.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_activitymenu)

        scenariosRecyclerView = findViewById(R.id.scenariosRecyclerView)
        emptyListTextView = findViewById(R.id.emptyListTextView)

        scenariosRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ScenariosAdapter(scenariosList)
        scenariosRecyclerView.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserScenarios()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Уведомления не будут показываться", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserScenarios() {
        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val email = sharedPref.getString("email", null)

        if (email.isNullOrEmpty()) {
            showEmptyList(true)
            Toast.makeText(this, "Email не найден. Войдите в систему.", Toast.LENGTH_SHORT).show()
            return
        }

        GetUserScenariosTask().execute(email)
    }

    private fun showEmptyList(show: Boolean) {
        emptyListTextView.visibility = if (show) View.VISIBLE else View.GONE
        scenariosRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private inner class GetUserScenariosTask : AsyncTask<String, Void, List<UserScenario>?>() {
        override fun doInBackground(vararg params: String): List<UserScenario>? {
            val email = params[0]
            try {
                val url = URL("$BASE_URL/api/user/scenarios?username=$email")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    Log.e("GetUserScenarios", "HTTP error: $responseCode")
                    return null
                }

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                if (!jsonObject.getBoolean("success")) {
                    return null
                }

                val scenariosArray = jsonObject.getJSONArray("scenarios_of_user")
                val result = mutableListOf<UserScenario>()

                for (i in 0 until scenariosArray.length()) {
                    val obj = scenariosArray.getJSONObject(i)
                    val displayName = if (obj.has("display_name") && !obj.isNull("display_name")) {
                        obj.getString("display_name")
                    } else {
                        obj.getString("scenario_name")
                    }

                    Log.d("GetUserScenarios", "Display name: $displayName")

                    val scenario = UserScenario(
                        name = displayName,
                        minTemp = obj.optDouble("min_temperature", 1000.0).toFloat(),
                        maxTemp = obj.optDouble("max_temperature", 1000.0).toFloat(),
                        minSoil = obj.optDouble("min_soil_moisture", 1000.0).toFloat(),
                        maxSoil = obj.optDouble("max_soil_moisture", 1000.0).toFloat(),
                        minHum = obj.optDouble("min_humidity", 1000.0).toFloat(),
                        maxHum = obj.optDouble("max_humidity", 1000.0).toFloat(),
                        minLight = obj.optDouble("min_light_lux", 1000.0).toFloat(),
                        maxLight = obj.optDouble("max_light_lux", 1000.0).toFloat()
                    )
                    result.add(scenario)
                }
                return result

            } catch (e: Exception) {
                Log.e("GetUserScenarios", "Error: ${e.message}")
                return null
            }
        }

        override fun onPostExecute(result: List<UserScenario>?) {
            if (result != null && result.isNotEmpty()) {
                scenariosList.clear()
                scenariosList.addAll(result)
                adapter.notifyDataSetChanged()
                showEmptyList(false)
                Toast.makeText(this@MainActivitymenu,
                    "Загружено ${result.size} сценариев", Toast.LENGTH_SHORT).show()
            } else {
                scenariosList.clear()
                adapter.notifyDataSetChanged()
                showEmptyList(true)
                if (result == null) {
                    Toast.makeText(this@MainActivitymenu,
                        "Ошибка загрузки сценариев", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun dob(v: View) {
        val intent = Intent(this, MainActivity2::class.java)
        startActivity(intent)
    }

    fun par(v: View) {
        val intent = Intent(this, MainActivityParametr::class.java)
        startActivity(intent)
    }

    inner class ScenariosAdapter(
        private val scenarios: List<UserScenario>
    ) : RecyclerView.Adapter<ScenariosAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.scenarioNameTextView)
            val paramsTextView: TextView = itemView.findViewById(R.id.scenarioParamsTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_scenario, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val scenario = scenarios[position]
            holder.nameTextView.text = scenario.name

            val params = mutableListOf<String>()

            if (scenario.minSoil != 1000.0f && scenario.maxSoil != 1000.0f) {
                params.add("💧 Влажность почвы: ${scenario.minSoil}%–${scenario.maxSoil}%")
            }
            if (scenario.minHum != 1000.0f && scenario.maxHum != 1000.0f) {
                params.add("💨 Влажность воздуха: ${scenario.minHum}%–${scenario.maxHum}%")
            }
            if (scenario.minTemp != 1000.0f && scenario.maxTemp != 1000.0f) {
                params.add("🌡️ Температура воздуха: ${scenario.minTemp}°C–${scenario.maxTemp}°C")
            }
            if (scenario.minLight != 1000.0f && scenario.maxLight != 1000.0f) {
                params.add("☀️ Освещённость: ${scenario.minLight}–${scenario.maxLight} лк")
            }

            holder.paramsTextView.text = if (params.isNotEmpty()) {
                params.joinToString("\n")
            } else {
                "Нет заданных параметров"
            }
        }

        override fun getItemCount(): Int = scenarios.size
    }
}