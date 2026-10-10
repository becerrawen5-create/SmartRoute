package com.example.smartroute.data

object SampleData {

    val routes = listOf(
        RouteItem(1, "Casa", "Universidad", 22, 8.4, Transport.AUTO, true, TrafficLevel.MODERADO),
        RouteItem(2, "Casa", "Trabajo", 28, 12.6, Transport.AUTO, false, TrafficLevel.FLUIDO),
        RouteItem(3, "Gym", "Casa", 15, 6.2, Transport.BICI, true, TrafficLevel.FLUIDO, RoutePreference.SEGURA, "6:30 p.m."),
        RouteItem(4, "Centro", "Casa", 18, 7.9, Transport.AUTO, false, TrafficLevel.LENTO)
    )

    val history = listOf(
        HistoryEntry(1, "Hoy", "Casa", "Universidad", 22, 8.4, "7:12 a.m.", Transport.AUTO),
        HistoryEntry(2, "Ayer", "Gym", "Casa", 15, 6.2, "6:45 p.m.", Transport.BICI),
        HistoryEntry(3, "22 sep 2025", "Casa", "Trabajo", 28, 12.6, "8:20 a.m.", Transport.AUTO),
        HistoryEntry(4, "20 sep 2025", "Centro", "Casa", 18, 7.9, "7:10 p.m.", Transport.AUTO),
        HistoryEntry(5, "20 sep 2025", "Casa", "Universidad", 24, 8.4, "7:20 a.m.", Transport.AUTO)
    )

    val weather = Weather(
        city = "Ciudad de México",
        tempC = 22,
        condition = "Parcialmente nublado",
        humidity = 56,
        windKmh = 12,
        uv = "Moderado",
        rainChance = 10,
        airQuality = "Buena",
        hourly = listOf(
            HourForecast("Ahora", 22),
            HourForecast("12 p.m.", 24),
            HourForecast("3 p.m.", 26),
            HourForecast("6 p.m.", 23),
            HourForecast("9 p.m.", 19)
        ),
        daily = listOf(
            DayForecast("Lun", 28, 14),
            DayForecast("Mar", 27, 15),
            DayForecast("Mié", 26, 14)
        )
    )

    val trafficZones = listOf(
        TrafficZone("Av. Insurgentes", TrafficLevel.LENTO, 18),
        TrafficZone("Periférico", TrafficLevel.MODERADO, 12)
    )

    val iotSources = listOf(
        "Smartphone / GPS",
        "Smartwatch",
        "Sensores ambientales",
        "Sensores de tráfico",
        "Vehículo conectado"
    )
}
