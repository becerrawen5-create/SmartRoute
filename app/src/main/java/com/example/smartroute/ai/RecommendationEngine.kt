package com.example.smartroute.ai

import com.example.smartroute.data.*

/**
 * Motor de recomendación (versión inicial basada en reglas).
 *
 * Combina el historial del usuario con datos que en producción vendrían de
 * dispositivos IoT (clima, tráfico, GPS) y devuelve una ruta adaptada.
 * Más adelante se puede reemplazar por un modelo de ML sin cambiar la UI.
 */
object RecommendationEngine {

    fun recommend(route: RouteItem, weather: Weather, acceptedAdaptations: Int): Recommendation {
        val title: String
        val reason: String
        val saved: Int

        when {
            route.transport == Transport.CAMINATA -> {
                title = "Ruta peatonal"
                reason = "Prioriza calles peatonales, banquetas amplias y zonas iluminadas."
                saved = 0
            }
            route.transport == Transport.BICI -> {
                title = "Ruta adaptada a bicicleta"
                reason = "Usa ciclovías y evita avenidas con tráfico pesado."
                saved = 2
            }
            weather.rainChance >= 50 -> {
                title = "Ruta con menor exposición"
                reason = "Hay ${weather.rainChance}% de probabilidad de lluvia; se prefieren tramos seguros y cubiertos."
                saved = -3
            }
            route.traffic == TrafficLevel.LENTO -> {
                title = "Ruta alternativa"
                reason = "Tráfico lento en tu ruta habitual; se evitan las avenidas congestionadas."
                saved = 7
            }
            route.traffic == TrafficLevel.MODERADO -> {
                title = "Ruta alternativa"
                reason = "Tráfico moderado; hay una variante con menos semáforos."
                saved = 4
            }
            route.preference == RoutePreference.SEGURA -> {
                title = "Ruta más segura"
                reason = "Prefieres rutas seguras aunque tarden unos minutos más."
                saved = -2
            }
            else -> {
                title = "Ruta favorita"
                reason = "Tráfico normal: tu ruta habitual es la mejor opción."
                saved = 0
            }
        }

        val confidence = (60 + acceptedAdaptations * 5).coerceAtMost(95)
        return Recommendation(
            title = title,
            reason = reason,
            habitualMinutes = route.minutes,
            adaptedMinutes = (route.minutes - saved).coerceAtLeast(5),
            confidence = confidence
        )
    }

    /** Resumen simple de patrones a partir del historial. */
    fun mostFrequentRoute(history: List<HistoryEntry>): Pair<String, Int>? =
        history.groupingBy { it.title }.eachCount().maxByOrNull { it.value }?.toPair()
}
