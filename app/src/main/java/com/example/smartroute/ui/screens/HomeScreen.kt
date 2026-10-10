package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun HomeScreen(
    vm: SmartRouteViewModel,
    onOpenMap: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenTraffic: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRoute: (Int) -> Unit
) {
    StatusBarStyle(darkIcons = false)
    val route = vm.featuredRoute
    val rec = vm.recommendationFor(route)
    val w = vm.weather

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Menu, null, tint = Color.White)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Place, null, tint = Yellow)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Hola, ${vm.userName.substringBefore(' ')} 👋",
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold
            )
            Text("¿A dónde vamos hoy?", color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpenMap)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Muted)
                Spacer(Modifier.width(10.dp))
                Text("Buscar destino o dirección", color = Muted, fontSize = 14.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickAction(Icons.Filled.Directions, "Rutas", "Inteligentes", Blue600, Blue100, onOpenMap, Modifier.weight(1f))
            QuickAction(Icons.Filled.Cloud, "Clima", "en tiempo real", Color(0xFF3A9BD9), Color(0xFFDDF0FB), onOpenWeather, Modifier.weight(1f))
            QuickAction(Icons.Filled.Traffic, "Tráfico", "en tiempo real", Danger, Color(0xFFFDE4E4), onOpenTraffic, Modifier.weight(1f))
            QuickAction(Icons.Filled.Star, "Favoritos", "Tus rutas", Warning, Color(0xFFFFF1CC), onOpenFavorites, Modifier.weight(1f))
        }

        AppCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Text("Tu ruta recomendada", fontWeight = FontWeight.Bold, color = Ink)
            Text("Basada en tu historial y condiciones actuales", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(route.title, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("${rec.adaptedMinutes} min · ${"%.1f".format(route.km)} km", color = Muted, fontSize = 12.sp)
                }
                Button(onClick = { onOpenRoute(route.id) }, shape = RoundedCornerShape(50)) {
                    Text("Ver ruta", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(rec.title, color = Blue600, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(rec.reason, color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))

        AppCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Text(w.city, color = Muted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, null, tint = Yellow, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${w.tempC}°", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(w.condition, color = Muted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Lluvia ${w.rainChance}%", fontSize = 12.sp, color = Ink)
                    Text("Tráfico ${route.traffic.label}", fontSize = 12.sp, color = Ink)
                    Text("Aire ${w.airQuality}", fontSize = 12.sp, color = Ink)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    sub: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(sub, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center)
    }
}
