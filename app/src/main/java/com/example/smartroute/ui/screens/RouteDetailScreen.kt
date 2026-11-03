package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun RouteDetailScreen(vm: SmartRouteViewModel, onBack: () -> Unit, onOpenMap: () -> Unit) {
    StatusBarStyle(darkIcons = false)
    val route = vm.selectedRoute
    val rec = vm.recommendationFor(route)
    var tab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(Modifier.padding(bottom = 0.dp)) {
            BackTitleRow("Ruta seleccionada", onBack, trailing = {
                IconButton(onClick = onOpenMap) { Icon(Icons.Filled.Map, "Mapa", tint = Color.White) }
            })
            Spacer(Modifier.height(36.dp))
        }

        AppCard(
            Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-32).dp)
                .fillMaxWidth()
        ) {
            Text(route.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Blue600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${route.minutes} min · ${"%.1f".format(route.km)} km",
                    fontWeight = FontWeight.SemiBold, color = Ink
                )
            }
            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = Blue600) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Detalles") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Alternativas") })
            }
            Spacer(Modifier.height(8.dp))

            if (tab == 0) {
                DetailRow(Icons.Filled.Straighten, "Distancia") { Text("${"%.1f".format(route.km)} km", fontSize = 14.sp) }
                DetailRow(Icons.Filled.Schedule, "Duración") { Text("${route.minutes} min", fontSize = 14.sp) }
                DetailRow(Icons.Filled.Traffic, "Tráfico") {
                    Pill(route.traffic.label, trafficColor(route.traffic))
                }
                DetailRow(Icons.Filled.Cloud, "Clima") {
                    Text("${vm.weather.tempC}° ${vm.weather.condition}", fontSize = 13.sp)
                }
                DetailRow(Icons.Filled.AccessTime, "Hora de salida") { Text(route.departure, fontSize = 14.sp) }
                Spacer(Modifier.height(8.dp))
                Text("Preferencia: ${route.preference.label}", color = Muted, fontSize = 12.sp)
            } else {
                AlternativeCard(
                    "Ruta habitual",
                    "${rec.habitualMinutes} min",
                    "La que sueles utilizar.",
                    highlight = false
                ) {
                    OutlinedButton(onClick = { vm.keepHabitual() }) { Text("Elegir") }
                }
                Spacer(Modifier.height(10.dp))
                AlternativeCard(
                    "${rec.title} (IA)",
                    "${rec.adaptedMinutes} min",
                    rec.reason,
                    highlight = true
                ) {
                    Button(onClick = { vm.acceptAdaptation() }) { Text("Elegir") }
                }
                Spacer(Modifier.height(10.dp))
                Text("Confianza de la IA: ${vm.recommendationFor(route).confidence}%", color = Muted, fontSize = 12.sp)
                vm.learnedMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Success, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-16).dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.toggleFavorite(route.id) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    if (route.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (route.favorite) "Quitar favorito" else "Guardar favorito",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
            ) {
                Icon(Icons.Filled.Delete, null)
                Spacer(Modifier.width(6.dp))
                Text("Eliminar", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, null, tint = Danger) },
            title = { Text("Eliminar ruta") },
            text = {
                if (vm.routes.size <= 1) {
                    Text("Debe existir al menos una ruta guardada. Crea una nueva antes de eliminar esta.")
                } else {
                    Text("¿Seguro que quieres eliminar \"${route.title}\"? Esta acción no se puede deshacer.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        if (vm.deleteRoute(route.id)) onBack()
                    },
                    enabled = vm.routes.size > 1,
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        value()
    }
    HorizontalDivider(color = Blue100)
}

@Composable
private fun AlternativeCard(
    title: String,
    time: String,
    description: String,
    highlight: Boolean,
    action: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) Blue100 else AppBg, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (highlight) Blue600 else Ink)
            Text(description, fontSize = 12.sp, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        action()
    }
}
