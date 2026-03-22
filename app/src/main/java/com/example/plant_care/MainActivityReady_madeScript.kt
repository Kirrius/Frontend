package com.example.plant_care

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivityReady_madeScript : AppCompatActivity() {
    private lateinit var scenariosRecyclerView: RecyclerView
    private lateinit var sharedPref: SharedPreferences
    private lateinit var confirmSelectionBtn: Button
    private lateinit var cancelSelectionBtn: Button
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var adapter: ScenarioAdapter
    private lateinit var progressDialog: ProgressDialog

    private var plantName: String = ""
    private var userEmail: String = ""

    private val scenarioIdMap = mapOf(
        "Тропики" to 12,
        "Зеленая оазис" to 13,
        "Экзотический сад" to 14,
        "Моховой уголок" to 15,
        "Комнатный уют" to 16,
        "Теплый тропик" to 17,
        "Лесная тень" to 18,
        "Светлый уголок" to 19,
        "Солнечный сад" to 20,
        "Холодный уют" to 21,
        "Умеренный климат" to 22,
        "Сухая пустыня" to 23,
        "Моховой лес" to 24,
        "Сухой оазис" to 25,
        "Пустынный цветок" to 26,
        "Холодная пустыня" to 27,
        "Сухая равнина" to 28,
        "Жаркая пустыня" to 29
    )

    private var pendingScenario: Scenario? = null

    companion object {
        const val PREFS_NAME = "PlantCarePrefs"
        const val KEY_SELECTED_SCENARIO = "selected_scenario"
        const val BASE_URL = "http://172.20.10.4:5000"
    }

    private val scenarios = listOf(
        Scenario("Тропики", "Высокая влажность почвы, высокая влажность воздуха, низкая температура", "Фиттония, папоротники, маранта"),
        Scenario("Зеленая оазис", "Высокая влажность почвы, высокая влажность воздуха, средняя температура", "Ароидные, селагинелла, бегония"),
        Scenario("Экзотический сад", "Высокая влажность почвы, высокая влажность воздуха, высокая температура", "Тропические орхидеи, водяные лилии"),
        Scenario("Моховой уголок", "Высокая влажность почвы, средняя влажность воздуха, низкая температура", "Мхи, хойя"),
        Scenario("Комнатный уют", "Высокая влажность почвы, средняя влажность воздуха, средняя температура", "Спатифиллум, фиалка"),
        Scenario("Теплый тропик", "Высокая влажность почвы, средняя влажность воздуха, высокая температура", "Кала, драцена"),
        Scenario("Лесная тень", "Средняя влажность почвы, высокая влажность воздуха, низкая температура", "Мхи, некоторые виды папоротников"),
        Scenario("Светлый уголок", "Средняя влажность почвы, высокая влажность воздуха, средняя температура", "Фиалки, хойя"),
        Scenario("Солнечный сад", "Средняя влажность почвы, высокая влажность воздуха, высокая температура", "Бегонии, орхидеи"),
        Scenario("Холодный уют", "Средняя влажность почвы, средняя влажность воздуха, низкая температура", "Эпифиты, кактусы"),
        Scenario("Умеренный климат", "Средняя влажность почвы, средняя влажность воздуха, средняя температура", "Мезофиты, хлорофитум"),
        Scenario("Сухая пустыня", "Средняя влажность почвы, средняя влажность воздуха, высокая температура", "Кактусы, суккуленты"),
        Scenario("Моховой лес", "Низкая влажность почвы, высокая влажность воздуха, низкая температура", "Некоторые виды мхов, эпифиты"),
        Scenario("Сухой оазис", "Низкая влажность почвы, высокая влажность воздуха, средняя температура", "Бромелии, кактусы"),
        Scenario("Пустынный цветок", "Низкая влажность почвы, высокая влажность воздуха, высокая температура", "Некоторые виды суккулентов"),
        Scenario("Холодная пустыня", "Низкая влажность почвы, средняя влажность воздуха, низкая температура", "Кактусы, суккуленты"),
        Scenario("Сухая равнина", "Низкая влажность почвы, средняя влажность воздуха, средняя температура", "Кактусы, суккуленты"),
        Scenario("Жаркая пустыня", "Низкая влажность почвы, средняя влажность воздуха, высокая температура", "Кактусы, суккуленты")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ready_made_script)

        plantName = intent.getStringExtra("PLANT_NAME") ?: "Неизвестное растение"
        sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userEmail = sharedPref.getString("email", "") ?: ""

        Log.w("MainActivityReadyScript", "Полученный email: $userEmail")
        Log.w("MainActivityReadyScript", "Название растения: $plantName")

        sharedPref.edit().putString("email", userEmail).apply()

        supportActionBar?.title = "Выбор сценария для: $plantName"

        scenariosRecyclerView = findViewById(R.id.scenariosRecyclerView)
        confirmSelectionBtn = findViewById(R.id.confirmSelectionBtn)
        cancelSelectionBtn = findViewById(R.id.cancelSelectionBtn)
        buttonsLayout = findViewById(R.id.buttonsLayout)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Отправка данных на сервер...")
            setCancelable(false)
        }

        setupRecyclerView()
        setupButtons()
        checkExistingSelection()
    }

    private fun setupButtons() {
        confirmSelectionBtn.setOnClickListener {
            pendingScenario?.let { scenario ->
                assignScenarioToUser(scenario)
            }
        }

        cancelSelectionBtn.setOnClickListener {
            clearPendingSelection()
        }
    }

    private fun setupRecyclerView() {
        scenariosRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ScenarioAdapter(scenarios) { position ->
            showScenarioDetails(scenarios[position])
        }
        scenariosRecyclerView.adapter = adapter
    }

    private fun checkExistingSelection() {
        val selectedScenarioName = sharedPref.getString(KEY_SELECTED_SCENARIO, null)
        if (selectedScenarioName != null) {
            scenarios.find { it.name == selectedScenarioName }?.let { scenario ->
                pendingScenario = scenario
                updateButtonsVisibility(true)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showScenarioDetails(scenario: Scenario) {
        AlertDialog.Builder(this)
            .setTitle(scenario.name)
            .setMessage(
                "Условия: ${scenario.conditions}\n\n" +
                        "Подходящие растения: ${scenario.plants}"
            )
            .setPositiveButton("Выбрать этот сценарий") { _, _ ->
                setPendingScenario(scenario)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setPendingScenario(scenario: Scenario) {
        pendingScenario = scenario
        updateButtonsVisibility(true)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Выбран сценарий: ${scenario.name}", Toast.LENGTH_SHORT).show()
    }

    private fun clearPendingSelection() {
        pendingScenario = null
        updateButtonsVisibility(false)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Выбор сценария отменён", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtonsVisibility(show: Boolean) {
        buttonsLayout.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun assignScenarioToUser(scenario: Scenario) {
        val scenarioId = scenarioIdMap[scenario.name] ?: run {
            Toast.makeText(this, "Ошибка: ID сценария не найден", Toast.LENGTH_LONG).show()
            return
        }

        progressDialog.show()
        SendScenarioTask().execute(userEmail, scenarioId.toString(), plantName, scenario.name)
    }

    private inner class SendScenarioTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            val userEmail = params[0]
            val scenarioId = params[1]
            val plantName = params[2]
            val scenarioName = params[3]

            try {
                val url = URL("$BASE_URL/api/user/scenarios")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Важно: device_id должен совпадать с тем, что отправляет ESP32
                val json = """
                    {
                        "username": "$userEmail",
                        "scenario_id": $scenarioId,
                        "plant_name": "$plantName",
                        "device_id": "ESP32_PlantMonitor"
                    }
                """.trimIndent()

                val outputStream: OutputStream = connection.outputStream
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                return "$responseCode|$response"

            } catch (e: Exception) {
                Log.e("SendScenarioTask", "Ошибка сети: ${e.message}")
                return "Error|${e.message}"
            }
        }

        override fun onPostExecute(result: String) {
            progressDialog.dismiss()

            if (result.startsWith("Error|")) {
                Toast.makeText(this@MainActivityReady_madeScript, "Ошибка сети: ${result.substring(6)}", Toast.LENGTH_LONG).show()
                return
            }

            val parts = result.split("|", limit = 2)
            if (parts.size < 2) {
                Toast.makeText(this@MainActivityReady_madeScript, "Некорректный ответ сервера", Toast.LENGTH_LONG).show()
                return
            }

            val responseCode = parts[0].toIntOrNull() ?: 0
            val responseText = parts[1]

            try {
                val jsonResponse = JSONObject(responseText)
                val success = jsonResponse.optBoolean("success", false)
                val message = jsonResponse.optString("message", "")

                if (success && responseCode in 200..299) {
                    pendingScenario?.let { scenario ->
                        // Сохраняем имя выбранного сценария
                        sharedPref.edit().putString(KEY_SELECTED_SCENARIO, scenario.name).apply()

                        // Сохраняем правильный device_id для получения уведомлений
                        sharedPref.edit().putString("device_id", "ESP32_PlantMonitor").apply()
                        Log.d("MainActivityReady", "Сохранён device_id: ESP32_PlantMonitor")

                        Toast.makeText(
                            this@MainActivityReady_madeScript,
                            "Сценарий '${scenario.name}' успешно привязан к растению '$plantName'",
                            Toast.LENGTH_LONG
                        ).show()

                        navigateToMenu()
                    }
                } else {
                    Toast.makeText(this@MainActivityReady_madeScript, "Ошибка: $message", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivityReady_madeScript, "Ответ сервера: $responseText", Toast.LENGTH_LONG).show()
                Log.e("SendScenarioTask", "Ошибка парсинга JSON: ${e.message}")
            }
        }
    }

    private fun navigateToMenu() {
        val intent = Intent(this, MainActivitymenu::class.java)
        startActivity(intent)
        finish()
    }

    data class Scenario(
        val name: String,
        val conditions: String,
        val plants: String
    )

    inner class ScenarioAdapter(
        private val scenarios: List<Scenario>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ScenarioAdapter.ScenarioViewHolder>() {

        inner class ScenarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.scenarioNameTextView)
            val rootView: View = itemView.findViewById(R.id.rootLayout)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scenario, parent, false)
            return ScenarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
            val scenario = scenarios[position]
            holder.nameTextView.text = scenario.name

            val isSelected = pendingScenario?.name == scenario.name
            holder.rootView.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(this@MainActivityReady_madeScript, R.color.selected_item_color)
                else Color.TRANSPARENT
            )

            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }

        override fun getItemCount() = scenarios.size
    }
}