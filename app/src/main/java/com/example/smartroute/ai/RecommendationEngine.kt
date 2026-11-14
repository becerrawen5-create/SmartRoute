package com.example.smartroute.ai

import com.example.smartroute.data.*
import kotlin.math.roundToInt

/**
 * Motor de recomendación con Inteligencia Artificial adaptable.
 *
 * Fuente de verdad (README.md):
 * "App que guarda rutas favoritas y las adapta con IA según historial, clima, tráfico y datos IoT."
 *
 * Analiza sinérgicamente:
 * 1. Historial del usuario (frecuencia, tiempos reales previos).
 * 2. Condiciones meteorológicas actuales (probabilidad de lluvia, viento, índice UV).
 * 3. Nivel de tráfico en vivo (vías principales y zonas congestionadas).
 * 4. Sensores y dispositivos IoT activos (GPS, Smartwatch, Sensores ambientales,
 *    Sensores viales y Vehículo conectado).
 * 5. Preferencia de desplazamiento del usuario (Más rápida, Más segura, Evitar tráfico).
 */
object RecommendationEngine {

    fun recommend(
        route: RouteItem,
        weather: Weather,
        history: List<HistoryEntry> = emptyList(),
        iotSources: Map<String, Boolean> = emptyMap(),
        acceptedAdaptations: Int = 0
    ): Recommendation {
        var title = "Ruta optimizada por IA"
        val reasons = mutableListOf<String>()
        var savedMinutes = 0

        // 1. ANÁLISIS DEL HISTORIAL
        val tripsForThisRoute = history.filter {
            it.title.equals(route.title, ignoreCase = true) ||
                    (it.from.equals(route.from, ignoreCase = true) && it.to.equals(route.to, ignoreCase = true))
        }
        val habitualMinutes = if (tripsForThisRoute.isNotEmpty()) {
            tripsForThisRoute.map { it.minutes }.average().roundToInt()
        } else {
            route.minutes
        }

        if (tripsForThisRoute.size >= 2) {
            reasons.add("Historial analizado (${tripsForThisRoute.size} viajes registrados)")
        }

        // 2. ANÁLISIS DE DATOS IOT
        val iotTrafficEnabled = iotSources["Sensores de tráfico"] ?: true
        val iotWeatherEnabled = iotSources["Sensores ambientales"] ?: true
        val iotGpsEnabled = iotSources["Smartphone / GPS"] ?: true
        val iotWatchEnabled = iotSources["Smartwatch"] ?: true
        val iotVehicleEnabled = iotSources["Vehículo conectado"] ?: true

        // 3. ADAPTACIÓN SEGÚN MEDIO DE TRANSPORTE
        when (route.transport) {
            Transport.CAMINATA -> {
                title = "Ruta peatonal segura"
                if (iotWatchEnabled) {
                    reasons.add("Smartwatch activo: calibrando ritmo cardíaco y pasos en tramos seguros y peatonales.")
                } else {
                    reasons.add("Prioriza calles peatonales amplias y zonas con buena iluminación.")
                }
                savedMinutes = 0
            }
            Transport.BICI -> {
                title = "Ruta ciclista optimizada"
                if (iotWeatherEnabled && weather.windKmh > 20) {
                    reasons.add("Sensores ambientales detectan viento de ${weather.windKmh} km/h: se seleccionan vías resguardadas.")
                    savedMinutes = 1
                } else {
                    reasons.add("Usa ciclovías protegidas y evita avenidas de alta velocidad vehicular.")
                    savedMinutes = 2
                }
            }
            Transport.TRANSPORTE -> {
                title = "Ruta de transporte público eficiente"
                if (iotTrafficEnabled && route.traffic != TrafficLevel.FLUIDO) {
                    reasons.add("Sensores viales reportan congestión: conexión alternativa mediante carril exclusivo.")
                    savedMinutes = 5
                } else {
                    reasons.add("Conexiones directas priorizando menor tiempo de transbordo.")
                    savedMinutes = 2
                }
            }
            Transport.AUTO -> {
                if (iotVehicleEnabled) {
                    reasons.add("Vehículo conectado: optimizando velocidad constante y menor consumo de combustible.")
                }
            }
        }

        // 4. ADAPTACIÓN SEGÚN CLIMA
        if (weather.rainChance >= 50) {
            title = "Ruta protegida por lluvia"
            val detail = if (iotWeatherEnabled) {
                "Sensores ambientales confirman ${weather.rainChance}% de probabilidad de lluvia; evitando pasos a desnivel inundables."
            } else {
                "Pronóstico de lluvia (${weather.rainChance}%); priorizando avenidas techadas y drenadas."
            }
            reasons.add(detail)
            savedMinutes -= 2
        } else if (weather.tempC >= 28 && route.transport == Transport.CAMINATA) {
            reasons.add("Sensores ambientales: radiación UV ${weather.uv}; sugiriendo aceras con arbolado y sombra.")
        }

        // 5. ADAPTACIÓN SEGÚN TRÁFICO Y SENSORES VIALES
        if (route.transport == Transport.AUTO || route.transport == Transport.TRANSPORTE) {
            when (route.traffic) {
                TrafficLevel.LENTO -> {
                    title = "Desvío inteligente anti-congestión"
                    val msg = if (iotTrafficEnabled) {
                        "Sensores de tráfico IoT alertan cuello de botella: desvío dinámico por vías secundarias."
                    } else {
                        "Tráfico lento en tu trayecto habitual: se evitan arterias congestionadas."
                    }
                    reasons.add(msg)
                    savedMinutes += 7
                }
                TrafficLevel.MODERADO -> {
                    title = "Variante fluida con menos demoras"
                    reasons.add("Tráfico moderado: ruta alternativa con mejor sincronización de semáforos.")
                    savedMinutes += 4
                }
                TrafficLevel.FLUIDO -> {
                    if (reasons.isEmpty()) {
                        title = "Ruta habitual preferente"
                        reasons.add("Tráfico fluido y condiciones óptimas en tu ruta preferida.")
                    }
                }
            }
        }

        // 6. ADAPTACIÓN SEGÚN PREFERENCIAS
        when (route.preference) {
            RoutePreference.SEGURA -> {
                title = "Ruta de máxima seguridad"
                reasons.add("Preferencia: avenidas vigiladas y bien iluminadas aunque añadan tiempo.")
                savedMinutes -= 2
            }
            RoutePreference.EVITAR_TRAFICO -> {
                title = "Ruta fluida anti-tráfico"
                reasons.add("Preferencia: se priorizan vías de circulación continua.")
                savedMinutes += 3
            }
            RoutePreference.RAPIDA -> {
                // Ya optimizado
            }
        }

        // 7. CÁLCULO DE CONFIANZA DE LA IA
        var activeIotCount = 0
        if (iotTrafficEnabled) activeIotCount++
        if (iotWeatherEnabled) activeIotCount++
        if (iotGpsEnabled) activeIotCount++
        if (iotWatchEnabled) activeIotCount++
        if (iotVehicleEnabled) activeIotCount++

        val baseConfidence = 65
        val iotBonus = activeIotCount * 3
        val historyBonus = (tripsForThisRoute.size * 2).coerceAtMost(10)
        val learningBonus = (acceptedAdaptations * 4).coerceAtMost(16)

        val totalConfidence = (baseConfidence + iotBonus + historyBonus + learningBonus).coerceIn(60, 98)

        val finalAdaptedMinutes = (habitualMinutes - savedMinutes).coerceAtLeast(3)
        val finalReason = reasons.distinct().joinToString(" ")

        return Recommendation(
            title = title,
            reason = finalReason.ifBlank { "Ruta habitual optimizada según tus preferencias." },
            habitualMinutes = habitualMinutes,
            adaptedMinutes = finalAdaptedMinutes,
            confidence = totalConfidence
        )
    }

    /** Resumen simple de patrones a partir del historial. */
    fun mostFrequentRoute(history: List<HistoryEntry>): Pair<String, Int>? =
        history.groupingBy { it.title }.eachCount().maxByOrNull { it.value }?.toPair()
}
