package com.example.plant_care

// Модели данных
data class UserScenario(
    val name: String,
    val minTemp: Float,
    val maxTemp: Float,
    val minSoil: Float,
    val maxSoil: Float,
    val minHum: Float,
    val maxHum: Float,
    val minLight: Float,
    val maxLight: Float
)

data class SensorData(
    val light: Float,
    val soil: Float,
    val temp: Float,
    val humidity: Float,
    val pump: Boolean,
    val timestamp: Long
)