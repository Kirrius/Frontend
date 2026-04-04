package com.example.plant_care

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SensorsAdapter(
    private val sensors: MutableMap<String, Any>
) : RecyclerView.Adapter<SensorsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.sensorName)
        val valueTextView: TextView = view.findViewById(R.id.sensorValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = sensors.entries.elementAt(position)
        val key = entry.key
        val value = entry.value

        val formattedName = formatSensorName(key)
        holder.nameTextView.text = formattedName

        val formattedValue = formatSensorValue(key, value)
        holder.valueTextView.text = formattedValue
    }

    override fun getItemCount(): Int = sensors.size

    private fun formatSensorName(key: String): String {
        return when {
            // Освещённость 1
            key == "light_1" -> "💡 Освещённость 1"
            // Освещённость 2
            key == "light_2" -> "💡 Освещённость 2"
            // Простая освещённость
            key == "light" -> "💡 Освещённость"
            // Освещённость с номером (light_0, light_1 и т.д.)
            key.startsWith("light_") -> {
                val num = key.replace("light_", "")
                "💡 Освещённость $num"
            }
            // Влажность почвы
            key == "soil" -> "🌱 Влажность почвы"
            // Влажность почвы с номером
            key.startsWith("soil_") -> {
                val num = key.replace("soil_", "")
                "🌱 Влажность почвы $num"
            }
            // Температура
            key == "temp" -> "🌡️ Температура"
            // Температура от SHT датчика
            key.startsWith("temp_") -> {
                val addr = key.replace("temp_", "")
                "🌡️ Температура (0x$addr)"
            }
            // Влажность воздуха
            key == "humidity" -> "💧 Влажность воздуха"
            // Влажность воздуха от SHT датчика
            key.startsWith("hum_") -> {
                val addr = key.replace("hum_", "")
                "💧 Влажность воздуха (0x$addr)"
            }
            // SHT температура (старый формат)
            key.startsWith("sht_temp") -> {
                val addr = key.replace("sht_temp_", "")
                "🌡️ Температура (0x$addr)"
            }
            // SHT влажность (старый формат)
            key.startsWith("sht_hum") -> {
                val addr = key.replace("sht_hum_", "")
                "💧 Влажность воздуха (0x$addr)"
            }
            // Любые другие датчики
            else -> {
                key.replace("_", " ").split(" ")
                    .joinToString(" ") { word ->
                        if (word.isNotEmpty()) word.substring(0, 1).uppercase() + word.substring(1)
                        else word
                    }
            }
        }
    }

    private fun formatSensorValue(key: String, value: Any): String {
        val numValue = when (value) {
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Float -> value.toDouble()
            is Double -> value
            else -> return value.toString()
        }

        return when {
            key.contains("temp") -> String.format("%.1f °C", numValue)
            key.contains("hum") -> String.format("%.1f %%", numValue)
            key.contains("light") -> String.format("%.0f лк", numValue)
            key.contains("soil") -> String.format("%.0f %%", numValue)
            else -> String.format("%.2f", numValue)
        }
    }
}