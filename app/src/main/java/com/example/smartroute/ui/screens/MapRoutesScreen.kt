package com.example.smartroute.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.smartroute.data.Transport
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*
import org.osmdroid.util.GeoPoint

@Composable
fun MapRoutesScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = true)

    // Inicializar seguimiento dinámico de ubicación GPS en tiempo real
    LaunchedEffect(Unit) {
        vm.initLocation(context)
    }

    val route = vm.selectedRoute

    if (route == null) {
        Box(Modifier.fillMaxSize()) {
            RealMap(
                modifier = Modifier.fillMaxSize(),
                userLocation = vm.currentUserLocation,
                showControls = true
            )
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sin ruta seleccionada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Crea o selecciona una ruta en tu cuenta para ver el trazado en OpenStreetMap (osmdroid) y habilitar la navegación en tiempo real desde tu ubicación GPS actual.",
                        color = Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onBack, shape = RoundedCornerShape(50)) {
                        Text("Regresar")
                    }
                }
            }
        }
        return
    }

    var transport by remember(route.id) { mutableStateOf(route.transport) }
    var navigating by remember(route.id) { mutableStateOf(false) }
    val minutes = vm.estimateMinutes(route, transport)

    val startPoint = remember(route.startLat, route.startLng) {
        GeoPoint(route.startLat, route.startLng)
    }
    val endPoint = remember(route.endLat, route.endLng) {
        GeoPoint(route.endLat, route.endLng)
    }

    // Consultar tráfico real y polilínea para la ruta seleccionada vía OSRM
    LaunchedEffect(route.id) {
        vm.fetchTrafficForRoute(context, route)
    }

    val trafficReport = vm.currentTrafficReport
    val routePoints = trafficReport?.points ?: emptyList()
    val polyColor = when (trafficReport?.level ?: route.traffic) {
        com.example.smartroute.data.TrafficLevel.FLUIDO -> Success
        com.example.smartroute.data.TrafficLevel.MODERADO -> Warning
        com.example.smartroute.data.TrafficLevel.LENTO -> Danger
    }

    Box(Modifier.fillMaxSize()) {
        RealMap(
            modifier = Modifier.fillMaxSize(),
            startPoint = startPoint,
            endPoint = endPoint,
            startLabel = route.from,
            endLabel = route.to,
            routePoints = routePoints,
            polylineColor = polyColor,
            userLocation = vm.currentUserLocation,
            showControls = true
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MyLocation, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(route.from, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, null, tint = Danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(route.to, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = {
                        vm.addRoute(
                            from = route.to,
                            to = route.from,
                            startLat = route.endLat,
                            startLng = route.endLng,
                            endLat = route.startLat,
                            endLng = route.startLng,
                            transport = transport,
                            pref = route.preference,
                            onSuccess = {
                                Toast.makeText(context, "Ruta invertida guardada en Firebase", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }) {
                        Icon(Icons.Filled.SwapVert, "Invertir ruta", tint = Blue600)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Transport.entries.forEach { t ->
                    val selected = t == transport
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Blue600 else Color.White)
                            .clickable { transport = t }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(t.icon, null, tint = if (selected) Color.White else Ink)
                        Text(
                            formatDuration(vm.estimateMinutes(route, t)),
                            fontSize = 11.sp,
                            color = if (selected) Color.White else Ink
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (navigating) "Navegación en curso..." else "Ruta en OpenStreetMap (OSRM)",
                    color = if (navigating) Blue600 else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (navigating) FontWeight.Bold else FontWeight.Normal
                )
                val distDisplay = trafficReport?.distanceKm ?: route.km
                val durDisplay = trafficReport?.trafficDurationMin ?: minutes
                Text(
                    "${formatDuration(durDisplay)} · ${"%.1f".format(distDisplay)} km",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentTraffic = trafficReport?.level ?: route.traffic
                    Icon(Icons.Filled.Warning, null, tint = trafficColor(currentTraffic), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tráfico ${currentTraffic.label.lowercase()}", fontSize = 12.sp, color = Muted)
                    if ((trafficReport?.delayMin ?: 0) > 0) {
                        Text(" (+${trafficReport?.delayMin} min demora)", fontSize = 12.sp, color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (navigating) {
                            vm.recordTrip(route, durDisplay)
                            Toast.makeText(context, "¡Recorrido finalizado y guardado en tu historial!", Toast.LENGTH_SHORT).show()
                        }
                        navigating = !navigating
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = if (navigating) Danger else Blue600)
                ) {
                    Icon(if (navigating) Icons.Filled.Close else Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (navigating) "Detener navegación" else "Iniciar navegación", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
