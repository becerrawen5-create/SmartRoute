package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun WeatherScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = false)

    // Coordenadas objetivo: la ruta seleccionada o primera ruta guardada
    val activeRoute = vm.selectedRoute
    val targetLat = activeRoute?.startLat ?: 19.432608
    val targetLng = activeRoute?.startLng ?: -99.133209
    val targetName = activeRoute?.from?.ifBlank { null }

    // Cargar clima en tiempo real desde Open-Meteo API para las coordenadas exactas
    LaunchedEffect(targetLat, targetLng) {
        vm.fetchWeatherForCoordinates(context, targetLat, targetLng, targetName)
    }

    val w = vm.weather

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader {
            BackTitleRow("Clima en tiempo real", onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = w.city,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            Text(
                "Coordenadas: ${"%.4f".format(targetLat)}, ${"%.4f".format(targetLng)}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))

            if (vm.isLoadingWeather) {
                Row(
                    modifier = Modifier.padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando servicio meteorológico en vivo…", color = Color.White, fontSize = 13.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weatherIcon = when {
                        w.condition.contains("lluvia", ignoreCase = true) || w.rainChance >= 50 -> Icons.Filled.WaterDrop
                        w.condition.contains("nublado", ignoreCase = true) -> Icons.Filled.Cloud
                        w.condition.contains("tormenta", ignoreCase = true) -> Icons.Filled.Thunderstorm
                        else -> Icons.Filled.WbSunny
                    }
                    val iconTint = if (weatherIcon == Icons.Filled.WbSunny) Yellow else Color.White
                    Icon(weatherIcon, null, tint = iconTint, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("${w.tempC}°C", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                        Text(w.condition, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
                    }
                }
            }

            // Selector rápido de ubicaciones de rutas guardadas
            if (vm.routes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Ver clima en tus rutas guardadas:", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vm.routes.forEach { r ->
                        val isSelected = r.id == vm.selectedRouteId
                        AssistChip(
                            onClick = {
                                vm.select(r.id)
                                vm.fetchWeatherForCoordinates(context, r.startLat, r.startLng, r.from)
                            },
                            label = {
                                Text(
                                    r.from,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Blue900 else Color.White
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) Yellow else Color.White.copy(alpha = 0.25f)
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        vm.weatherError?.let { err ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-20).dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.1f))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Danger)
                    Spacer(Modifier.width(8.dp))
                    Text(err, color = Danger, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.fetchWeatherForCoordinates(context, targetLat, targetLng, targetName)
                    }) {
                        Text("Reintentar")
                    }
                }
            }
        }

        AppCard(Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(Icons.Filled.WaterDrop, "Humedad", "${w.humidity}%")
                Stat(Icons.Filled.Air, "Viento", "${w.windKmh} km/h")
                Stat(Icons.Filled.WbSunny, "UV", w.uv)
                Stat(Icons.Filled.Cloud, "Lluvia", "${w.rainChance}%")
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offset(y = (-8).dp)) {
            Text("Pronóstico hoy por horas", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            if (w.hourly.isEmpty()) {
                Text("Cargando pronóstico por horas…", color = Muted, fontSize = 12.sp)
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    w.hourly.take(5).forEach { h ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(h.label, fontSize = 11.sp, color = Muted)
                            val icon = when {
                                h.condition.contains("lluvia", ignoreCase = true) -> Icons.Filled.WaterDrop
                                h.condition.contains("nublado", ignoreCase = true) -> Icons.Filled.Cloud
                                else -> Icons.Filled.WbSunny
                            }
                            Icon(icon, null, tint = Blue600, modifier = Modifier.padding(vertical = 6.dp).size(20.dp))
                            Text("${h.temp}°", fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Pronóstico próximos días", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            if (w.daily.isEmpty()) {
                Text("Cargando pronóstico de días…", color = Muted, fontSize = 12.sp)
            } else {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    w.daily.forEach { d ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(d.day, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Medium)
                            Text(d.condition, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(horizontal = 8.dp))
                            Icon(Icons.Filled.Cloud, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("${d.max}° / ${d.min}°", color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Blue600, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
    }
}
