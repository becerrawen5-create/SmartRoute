package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.data.RoutePreference
import com.example.smartroute.data.Transport
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun CreateRouteScreen(vm: SmartRouteViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    StatusBarStyle(darkIcons = true)
    var from by remember { mutableStateOf("Mi ubicación") }
    var to by remember { mutableStateOf("") }
    var pref by remember { mutableStateOf(RoutePreference.RAPIDA) }
    var transport by remember { mutableStateOf(Transport.AUTO) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTitleRow("Crear ruta", onBack, tint = Ink)

        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = from, onValueChange = { from = it },
                        label = { Text("Desde") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.MyLocation, null, tint = Blue600) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = to, onValueChange = { to = it },
                        label = { Text("Hasta") }, placeholder = { Text("Universidad") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Place, null, tint = Danger) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(onClick = { val t = from; from = to; to = t }) {
                    Icon(Icons.Filled.SwapVert, "Invertir", tint = Blue600)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Preferencias", color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PrefItem(Icons.Filled.Speed, RoutePreference.RAPIDA, pref) { pref = it }
            PrefItem(Icons.Filled.Shield, RoutePreference.SEGURA, pref) { pref = it }
            PrefItem(Icons.Filled.Traffic, RoutePreference.EVITAR_TRAFICO, pref) { pref = it }
        }

        Spacer(Modifier.height(16.dp))
        Text("Medio de transporte", fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Transport.entries.forEach { t ->
                val selected = t == transport
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Blue600 else AppBg)
                        .clickable { transport = t }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(t.icon, null, tint = if (selected) Color.White else Ink)
                    Text(t.label, fontSize = 11.sp, color = if (selected) Color.White else Ink)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                vm.addRoute(from, to, transport, pref)
                onCreated()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50)
        ) { Text("Buscar ruta", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrefItem(
    icon: ImageVector,
    value: RoutePreference,
    selected: RoutePreference,
    onSelect: (RoutePreference) -> Unit
) {
    val isSelected = value == selected
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(value) }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) Blue100 else AppBg)
                .border(if (isSelected) 2.dp else 0.dp, if (isSelected) Blue600 else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = if (isSelected) Blue600 else Muted) }
        Spacer(Modifier.height(4.dp))
        Text(value.label, fontSize = 11.sp, color = Ink)
    }
}
