package com.example.smartroute.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.data.RouteItem
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun FavoritesScreen(vm: SmartRouteViewModel, onOpenRoute: (Int) -> Unit, onAddRoute: () -> Unit) {
    StatusBarStyle(darkIcons = false)

    // Ruta pendiente de eliminar (para el diálogo de confirmación)
    var pendingDelete by remember { mutableStateOf<RouteItem?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader {
            Text("Rutas favoritas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Tus lugares y rutas más frecuentes", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.routes.forEach { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenRoute(route.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(Blue900),
                            contentAlignment = Alignment.Center
                        ) { Icon(route.transport.icon, null, tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(route.title, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("${route.minutes} min · ${"%.1f".format(route.km)} km", color = Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { vm.toggleFavorite(route.id) }) {
                            Icon(
                                if (route.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (route.favorite) Danger else Muted
                            )
                        }
                        // ↓ Nuevo botón de eliminar ↓
                        IconButton(onClick = { pendingDelete = route }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar ruta",
                                tint = Danger
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAddRoute,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Añadir ruta", fontWeight = FontWeight.Bold)
            }
        }
    }

    // ↓ Diálogo de confirmación (fuera del Column) ↓
    pendingDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Filled.Delete, null, tint = Danger) },
            title = { Text("Eliminar ruta") },
            text = {
                if (vm.routes.size <= 1) {
                    Text("Debe existir al menos una ruta guardada. Crea una nueva antes de eliminar esta.")
                } else {
                    Text("¿Eliminar \"${route.title}\"? Esta acción no se puede deshacer.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteRoute(route.id)
                        pendingDelete = null
                    },
                    enabled = vm.routes.size > 1,
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}