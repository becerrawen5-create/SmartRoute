package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.smartroute.data.RoutePreference
import com.example.smartroute.data.Transport
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun ProfileScreen(vm: SmartRouteViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = true)

    var activeDialog by remember { mutableStateOf<String?>(null) }

    // Estados para edición de perfil
    var editName by remember { mutableStateOf(vm.userName) }
    var editEmail by remember { mutableStateOf(vm.email) }

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
                Pill("Usuario Activo", Success)
            }
        }
        Spacer(Modifier.height(20.dp))

        items.forEach { (icon, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (label == "Mi perfil") {
                            editName = vm.userName
                            editEmail = vm.email
                        }
                        activeDialog = label
                    }
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

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { vm.logout(context, onFinished = onLogout) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }

    // Diálogos de configuración
    when (activeDialog) {
        "Mi perfil" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Mi perfil") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Correo electrónico") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        vm.updateProfile(editName, editEmail)
                        activeDialog = null
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        "Preferencias de ruta" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Preferencias de ruta predeterminadas") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Modo de ruta preferido:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        RoutePreference.entries.forEach { pref ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.defaultPreference = pref }
                            ) {
                                RadioButton(
                                    selected = vm.defaultPreference == pref,
                                    onClick = { vm.defaultPreference = pref }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(pref.label, fontSize = 14.sp)
                            }
                        }
                        HorizontalDivider()
                        Text("Transporte preferido:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Transport.entries.forEach { t ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.defaultTransport = t }
                            ) {
                                RadioButton(
                                    selected = vm.defaultTransport == t,
                                    onClick = { vm.defaultTransport = t }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(t.label, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        vm.updatePreferences(vm.defaultPreference, vm.defaultTransport)
                        activeDialog = null
                    }) {
                        Text("Guardar")
                    }
                }
            )
        }
        "Notificaciones" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Configuración de notificaciones") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Alertas de tráfico en tiempo real", modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Switch(
                                checked = vm.trafficAlertsEnabled,
                                onCheckedChange = { vm.trafficAlertsEnabled = it }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Avisos meteorológicos y lluvia", modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Switch(
                                checked = vm.weatherAlertsEnabled,
                                onCheckedChange = { vm.weatherAlertsEnabled = it }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sugerencias de rutas alternativas", modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Switch(
                                checked = vm.recommendationAlertsEnabled,
                                onCheckedChange = { vm.recommendationAlertsEnabled = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        vm.updateAlertSettings(vm.trafficAlertsEnabled, vm.weatherAlertsEnabled, vm.recommendationAlertsEnabled)
                        activeDialog = null
                    }) {
                        Text("Guardar")
                    }
                }
            )
        }
        "Privacidad y seguridad" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                icon = { Icon(Icons.Filled.Shield, null, tint = Blue600) },
                title = { Text("Privacidad y seguridad") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Cifrado de credenciales: Autenticación segura de extremo a extremo mediante Firebase Authentication.")
                        Text("• Datos de ubicación y telemetría: Utilizados exclusivamente para el motor de recomendación en tiempo real.")
                        Text("• Sensores IoT: Control total para habilitar o deshabilitar cualquier fuente en cualquier momento.")
                    }
                },
                confirmButton = {
                    Button(onClick = { activeDialog = null }) {
                        Text("Entendido")
                    }
                }
            )
        }
        "Soporte" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                icon = { Icon(Icons.AutoMirrored.Filled.Help, null, tint = Blue600) },
                title = { Text("Centro de Soporte") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("¿Tienes dudas o problemas con SmartRoute AI?", fontWeight = FontWeight.SemiBold)
                        Text("Correo: soporte@smartroute.ai")
                        Text("Horario de atención: 24/7 con asistencia inteligente.")
                        Text("Reporte de incidencias viales: Directo en la pestaña Tráfico.")
                    }
                },
                confirmButton = {
                    Button(onClick = { activeDialog = null }) {
                        Text("Entendido")
                    }
                }
            )
        }
        "Acerca de SmartRoute AI" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                icon = { Icon(Icons.Filled.Info, null, tint = Blue600) },
                title = { Text("Acerca de SmartRoute AI") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SmartRoute AI v1.0", fontWeight = FontWeight.Bold, color = Ink)
                        Text("App que guarda rutas favoritas y las adapta con IA según historial, clima, tráfico y datos IoT.")
                        Spacer(Modifier.height(4.dp))
                        Text("Desarrollado con Android Kotlin, Jetpack Compose y Material 3.", color = Muted, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { activeDialog = null }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}