package com.example.plant_care

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivityReady_madeScript : AppCompatActivity() {
    private lateinit var scenariosRecyclerView: RecyclerView
    private lateinit var sharedPref: SharedPreferences
    private lateinit var clearSelectionBtn: Button
    private lateinit var adapter: ScenarioAdapter

    companion object {
        const val PREFS_NAME = "PlantCarePrefs"
        const val KEY_SELECTED_SCENARIO = "selected_scenario"
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

        sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        scenariosRecyclerView = findViewById(R.id.scenariosRecyclerView)
        clearSelectionBtn = findViewById(R.id.clearSelectionBtn)

        setupRecyclerView()
        setupClearButton()
    }

    private fun setupClearButton() {
        clearSelectionBtn.setOnClickListener {
            clearScenarioSelection()
        }
        updateClearButtonVisibility()
    }

    private fun clearScenarioSelection() {
        sharedPref.edit {
            remove(KEY_SELECTED_SCENARIO)
        }
        adapter.notifyDataSetChanged()
        updateClearButtonVisibility()
        Toast.makeText(this, "Выбор сценария отменён", Toast.LENGTH_SHORT).show()
    }

    private fun updateClearButtonVisibility() {
        clearSelectionBtn.visibility = if (sharedPref.contains(KEY_SELECTED_SCENARIO)) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setupRecyclerView() {
        scenariosRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ScenarioAdapter(scenarios) { position ->
            showScenarioDetails(scenarios[position])
        }
        scenariosRecyclerView.adapter = adapter
    }

    private fun showScenarioDetails(scenario: Scenario) {
        AlertDialog.Builder(this)
            .setTitle(scenario.name)
            .setMessage(
                "Условия: ${scenario.conditions}\n\n" +
                        "Подходящие растения: ${scenario.plants}"
            )
            .setPositiveButton("Выбрать этот сценарий") { _, _ ->
                saveSelectedScenario(scenario)
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Отменить выбор") { _, _ ->
                clearScenarioSelection()
            }
            .show()
    }

    private fun saveSelectedScenario(scenario: Scenario) {
        sharedPref.edit {
            putString(KEY_SELECTED_SCENARIO, scenario.name)
        }
        adapter.notifyDataSetChanged()
        updateClearButtonVisibility()
        Toast.makeText(this, "Выбран сценарий: ${scenario.name}", Toast.LENGTH_LONG).show()
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

            val isSelected = sharedPref.getString(KEY_SELECTED_SCENARIO, null) == scenario.name
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