package com.example.smartroute.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Transport(val label: String, val icon: ImageVector, val speedKmh: Double) {
    AUTO("Auto", Icons.Filled.DirectionsCar, 23.0),
    BICI("Bici", Icons.Filled.DirectionsBike, 15.0),
    CAMINATA("Caminata", Icons.Filled.DirectionsWalk, 5.0),
    TRANSPORTE("Transporte", Icons.Filled.DirectionsBus, 14.0)
}

enum class TrafficLevel(val label: String) {
    FLUIDO("Fluido"),
    MODERADO("Moderado"),
    LENTO("Lento")
}

enum class RoutePreference(val label: String) {
    RAPIDA("Más rápida"),
    SEGURA("Más segura"),
    EVITAR_TRAFICO("Evitando tráfico")
}

data class RouteItem(
    val id: Int,
    val from: String,
    val to: String,
    val minutes: Int,
    val km: Double,
    val transport: Transport,
    val favorite: Boolean,
    val traffic: TrafficLevel,
    val preference: RoutePreference = RoutePreference.RAPIDA,
    val departure: String = "7:30 a.m."
) {
    val title: String get() = "$from → $to"
}

data class HistoryEntry(
    val id: Int,
    val day: String,
    val from: String,
    val to: String,
    val minutes: Int,
    val km: Double,
    val time: String,
    val transport: Transport
) {
    val title: String get() = "$from → $to"
}

data class HourForecast(val label: String, val temp: Int)
data class DayForecast(val day: String, val max: Int, val min: Int)

data class Weather(
    val city: String,
    val tempC: Int,
    val condition: String,
    val humidity: Int,
    val windKmh: Int,
    val uv: String,
    val rainChance: Int,
    val airQuality: String,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>
)

data class TrafficZone(val name: String, val level: TrafficLevel, val delayMin: Int)

data class Recommendation(
    val title: String,
    val reason: String,
    val habitualMinutes: Int,
    val adaptedMinutes: Int,
    val confidence: Int
)
