package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
fun TrafficScreen(vm: SmartRouteViewModel, onBack: () -> Unit) {
    StatusBarStyle(darkIcons = true)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.statusBarsPadding()) {
            BackTitleRow("Tráfico", onBack, tint = Ink)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            MapCanvas(Modifier.fillMaxSize(), showRoute = false, showTraffic = true)
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Legend("Fluido", Success)
            Legend("Moderado", Warning)
            Legend("Lento", Danger)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Text("Zonas con mayor tráfico", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(8.dp))
            vm.zones.forEach { z ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(AppBg, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = trafficColor(z.level))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(z.name, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("${z.level.label} · ${z.delayMin} min", color = Muted, fontSize = 12.sp)
                    }
                }
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
