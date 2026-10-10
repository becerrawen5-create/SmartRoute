package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ai.RecommendationEngine
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun HistoryScreen(vm: SmartRouteViewModel) {
    StatusBarStyle(darkIcons = true)
    var filter by remember { mutableIntStateOf(0) }
    val filters = listOf("Todas", "Favoritas", "Estadísticas")
    val favoriteTitles = vm.routes.filter { it.favorite }.map { it.title }.toSet()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Historial de rutas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { i, label ->
                val selected = i == filter
                Text(
                    label,
                    color = if (selected) Color.White else Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Blue600 else AppBg)
                        .clickable { filter = i }
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (filter == 2) {
            val total = vm.history.size
            val km = vm.history.sumOf { it.km }
            val avg = if (total > 0) vm.history.sumOf { it.minutes } / total else 0
            val top = RecommendationEngine.mostFrequentRoute(vm.history)
            StatCard("Recorridos", "$total")
            StatCard("Distancia total", "${"%.1f".format(km)} km")
            StatCard("Duración promedio", "$avg min")
            StatCard("Ruta más frecuente", top?.let { "${it.first} (${it.second}x)" } ?: "—")
            StatCard("Adaptaciones aceptadas", "${vm.acceptedAdaptations}")
        } else {
            val list = vm.history.filter { filter == 0 || it.title in favoriteTitles }
            if (list.isEmpty()) {
                Text("No hay recorridos para mostrar.", color = Muted)
            }
            list.groupBy { it.day }.forEach { (day, entries) ->
                Text(day, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                entries.forEach { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Blue100),
                            contentAlignment = Alignment.Center
                        ) { Icon(e.transport.icon, null, tint = Blue600) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            Text("${e.minutes} min · ${"%.1f".format(e.km)} km", color = Muted, fontSize = 12.sp)
                        }
                        Text(e.time, color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, color = Ink)
    }
}
