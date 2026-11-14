package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.data.RouteItem
import com.example.smartroute.data.TrafficLevel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*
import org.osmdroid.util.GeoPoint

@Composable
fun TrafficScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = true)

    val activeRoute = vm.selectedRoute
    val report = vm.currentTrafficReport

    LaunchedEffect(activeRoute?.id) {
        if (activeRoute != null) {
            vm.fetchTrafficForRoute(context, activeRoute)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        BackTitleRow(
            title = "Tráfico en tiempo real",
            onBack = onBack,
            tint = Ink,
            trailing = {
                if (activeRoute != null) {
                    IconButton(onClick = { vm.fetchTrafficForRoute(context, activeRoute) }) {
                        Icon(Icons.Filled.Refresh, "Actualizar tráfico", tint = Blue600)
                    }
                }
            }
        )

        if (vm.routes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Directions, null, tint = Blue600, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(14.dp))
                Text("Sin rutas guardadas en tu cuenta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Guarda una ruta con origen y destino para consultar el tráfico en tiempo real y el tiempo de viaje con OSRM y OpenStreetMap.",
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, shape = RoundedCornerShape(50)) {
                    Text("Volver")
                }
            }
            return
        }

        // Selector de rutas guardadas del usuario autenticado
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Tus rutas guardadas:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.routes.forEach { r ->
                    val isSelected = r.id == vm.selectedRouteId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            vm.select(r.id)
                            vm.fetchTrafficForRoute(context, r)
                        },
                        label = { Text(r.title, fontSize = 12.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Mapa de OpenStreetMap con la ruta real y polilínea de tráfico
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val startPoint = activeRoute?.let { GeoPoint(it.startLat, it.startLng) }
            val endPoint = activeRoute?.let { GeoPoint(it.endLat, it.endLng) }
            val points = report?.points ?: emptyList()

            val polyColor = when (report?.level ?: activeRoute?.traffic ?: TrafficLevel.FLUIDO) {
                TrafficLevel.FLUIDO -> Success
                TrafficLevel.MODERADO -> Warning
                TrafficLevel.LENTO -> Danger
            }

            RealMap(
                modifier = Modifier.fillMaxSize(),
                startPoint = startPoint,
                endPoint = endPoint,
                startLabel = activeRoute?.from ?: "Origen",
                endLabel = activeRoute?.to ?: "Destino",
                routePoints = points,
                polylineColor = polyColor
            )

            if (vm.isLoadingTraffic) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Blue600)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculando ruta con OSRM (OpenStreetMap)…", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Barra de leyendas
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Legend("Fluido (sin demora)", Success)
            Legend("Moderado (3-7 min)", Warning)
            Legend("Lento (>8 min)", Danger)
        }

        // Panel inferior con comparativa de tiempos
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            if (activeRoute != null && report != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppBg)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = trafficColor(report.level))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Nivel: Tráfico ${report.level.label}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Ink
                            )
                            Spacer(Modifier.weight(1f))
                            Pill(report.level.label, trafficColor(report.level))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Tiempo con tráfico", fontSize = 11.sp, color = Muted)
                                Text(
                                    "${report.trafficDurationMin} min",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.level == TrafficLevel.LENTO) Danger else Ink
                                )
                            }
                            Column {
                                Text("Tiempo normal", fontSize = 11.sp, color = Muted)
                                Text("${report.normalDurationMin} min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                            }
                            Column {
                                Text("Demora estimada", fontSize = 11.sp, color = Muted)
                                Text(
                                    if (report.delayMin > 0) "+${report.delayMin} min" else "0 min",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.delayMin > 0) Danger else Success
                                )
                            }
                        }
                        if (report.statusMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                report.statusMessage,
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Selecciona una ruta para evaluar congestión vial y estimaciones en tiempo real.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Muted)
    }
}
