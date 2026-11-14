package com.example.smartroute.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Transport(val label: String, val icon: ImageVector, val speedKmh: Double) {
    AUTO("Auto", Icons.Filled.DirectionsCar, 23.0),
    BICI("Bici", Icons.AutoMirrored.Filled.DirectionsBike, 15.0),
    CAMINATA("Caminata", Icons.AutoMirrored.Filled.DirectionsWalk, 5.0),
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
    val id: Int = 0,
    val uid: String = "",
    val from: String = "",
    val to: String = "",
    val startLat: Double = 19.432608,
    val startLng: Double = -99.133209,
    val endLat: Double = 19.432608,
    val endLng: Double = -99.133209,
    val minutes: Int = 0,
    val km: Double = 0.0,
    val transport: Transport = Transport.AUTO,
    val favorite: Boolean = false,
    val traffic: TrafficLevel = TrafficLevel.FLUIDO,
    val preference: RoutePreference = RoutePreference.RAPIDA,
    val departure: String = "7:30 a.m.",
    val firestoreDocId: String = ""
) {
    val title: String get() = "$from → $to"
}

data class HistoryEntry(
    val id: Int = 0,
    val uid: String = "",
    val day: String = "Hoy",
    val from: String = "",
    val to: String = "",
    val startLat: Double = 19.432608,
    val startLng: Double = -99.133209,
    val endLat: Double = 19.432608,
    val endLng: Double = -99.133209,
    val minutes: Int = 0,
    val km: Double = 0.0,
    val time: String = "",
    val transport: Transport = Transport.AUTO,
    val firestoreDocId: String = ""
) {
    val title: String get() = "$from → $to"
}

data class HourForecast(val label: String = "", val temp: Int = 0, val condition: String = "")
data class DayForecast(val day: String = "", val max: Int = 0, val min: Int = 0, val condition: String = "")

data class Weather(
    val city: String = "Ciudad de México",
    val tempC: Int = 22,
    val condition: String = "Despejado",
    val humidity: Int = 50,
    val windKmh: Int = 10,
    val uv: String = "Moderado",
    val rainChance: Int = 5,
    val airQuality: String = "Buena",
    val hourly: List<HourForecast> = emptyList(),
    val daily: List<DayForecast> = emptyList(),
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332
)

data class TrafficZone(
    val name: String = "",
    val level: TrafficLevel = TrafficLevel.FLUIDO,
    val delayMin: Int = 0
)

data class TrafficReport(
    val routeTitle: String = "",
    val normalDurationMin: Int = 0,
    val trafficDurationMin: Int = 0,
    val delayMin: Int = 0,
    val distanceKm: Double = 0.0,
    val level: TrafficLevel = TrafficLevel.FLUIDO,
    val points: List<org.osmdroid.util.GeoPoint> = emptyList(),
    val statusMessage: String = ""
)

data class Recommendation(
    val title: String = "",
    val reason: String = "",
    val habitualMinutes: Int = 0,
    val adaptedMinutes: Int = 0,
    val confidence: Int = 0
)
