package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.smartroute.data.Transport
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun MapRoutesScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    StatusBarStyle(darkIcons = true)
    val route = vm.selectedRoute
    var transport by remember(route.id) { mutableStateOf(route.transport) }
    var navigating by remember(route.id) { mutableStateOf(false) }
    val minutes = vm.estimateMinutes(route, transport)

    Box(Modifier.fillMaxSize()) {
        MapCanvas(Modifier.fillMaxSize())

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
                            Text(route.from, fontSize = 14.sp, color = Ink)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, null, tint = Danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(route.to, fontSize = 14.sp, color = Ink)
                        }
                    }
                    Icon(Icons.Filled.SwapVert, null, tint = Muted)
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
                Text("Ruta recomendada", color = Muted, fontSize = 12.sp)
                Text(
                    "${formatDuration(minutes)} · ${"%.1f".format(route.km)} km",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = trafficColor(route.traffic), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tráfico ${route.traffic.label.lowercase()}", fontSize = 12.sp, color = Muted)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { navigating = !navigating },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = if (navigating) Danger else Blue600)
                ) {
                    Icon(if (navigating) Icons.Filled.Close else Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (navigating) "Detener" else "Iniciar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
