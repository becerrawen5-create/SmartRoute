package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun ProfileScreen(vm: SmartRouteViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = true)
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.Person to "Mi perfil",
        Icons.Filled.Tune to "Preferencias de ruta",
        Icons.Filled.Notifications to "Notificaciones",
        Icons.Filled.Shield to "Privacidad y seguridad",
        Icons.AutoMirrored.Filled.Help to "Soporte",
        Icons.Filled.Info to "Acerca de SmartRoute AI"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Blue900),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(vm.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text(vm.email, color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Pill("Usuario Premium", Warning)
            }
        }
        Spacer(Modifier.height(20.dp))

        items.forEach { (icon, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Ink)
                Spacer(Modifier.width(14.dp))
                Text(label, modifier = Modifier.weight(1f), color = Ink)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
            }
            HorizontalDivider(color = Blue100)
        }

        Spacer(Modifier.height(20.dp))
        Text("Dispositivos y datos IoT", fontWeight = FontWeight.Bold, color = Ink)
        Text("Elige qué fuentes usa la IA para adaptar tus rutas.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        vm.iotSources.keys.toList().forEach { name ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(name, modifier = Modifier.weight(1f), color = Ink, fontSize = 14.sp)
                Switch(
                    checked = vm.iotSources[name] == true,
                    onCheckedChange = { vm.setIot(name, it) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = {vm.logout(context, onFinished = onLogout)}, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
        Spacer(Modifier.height(8.dp))
    }
}
