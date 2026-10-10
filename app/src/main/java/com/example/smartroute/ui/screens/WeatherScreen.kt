package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun WeatherScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    StatusBarStyle(darkIcons = false)
    val w = vm.weather

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader {
            BackTitleRow("Clima", onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(w.city, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text("Ahora", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, null, tint = Yellow, modifier = Modifier.size(64.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${w.tempC}°", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Text(w.condition, color = Color.White.copy(alpha = 0.9f))
                }
            }
            Spacer(Modifier.height(24.dp))
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
            Text("Hoy", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                w.hourly.forEach { h ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(h.label, fontSize = 11.sp, color = Muted)
                        Icon(Icons.Filled.Cloud, null, tint = Blue600, modifier = Modifier.padding(vertical = 6.dp))
                        Text("${h.temp}°", fontWeight = FontWeight.SemiBold, color = Ink)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Pronóstico próximos días", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp)) {
                w.daily.forEach { d ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(d.day, modifier = Modifier.weight(1f), color = Ink)
                        Icon(Icons.Filled.Cloud, null, tint = Blue600)
                        Spacer(Modifier.width(16.dp))
                        Text("${d.max}° / ${d.min}°", color = Ink)
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
        Icon(icon, null, tint = Blue600, modifier = Modifier.size(18.dp))
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
    }
}
