
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*

@Composable
fun FavoritesScreen(
    vm: SmartRouteViewModel,
    onOpenRoute: (Int) -> Unit,
    onAddRoute: () -> Unit
) {
    StatusBarStyle(darkIcons = false)

    LaunchedEffect(Unit) {
        vm.loadUserRoutes {
            vm.loadFavorites()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ){
        GradientHeader {
            Text(
                "Mis rutas",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Administra tus rutas guardadas",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                "Rutas favoritas",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            val favorites = vm.routes.filter { it.favorite }

            if (favorites.isEmpty()) {
                Text(
                    text = "Aún no tienes rutas favoritas. Presiona el corazón en una ruta para guardarla.",
                    color = Muted,
                    fontSize = 14.sp
                )
            }

            favorites.forEach { route ->
                RouteCard(
                    route = route,
                    vm = vm,
                    onOpenRoute = onOpenRoute
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = Muted.copy(alpha = 0.25f))

            Spacer(Modifier.height(8.dp))
            Text(
                "Todas mis rutas",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            val allRoutes = vm.routes.toList()

            if (allRoutes.isEmpty()) {
                Text(
                    "Todavía no tienes rutas registradas.",
                    color = Muted,
                    fontSize = 14.sp
                )
            }

            allRoutes.forEach { route ->
                RouteCard(
                    route = route,
                    vm = vm,
                    onOpenRoute = onOpenRoute
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAddRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Añadir ruta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: com.example.smartroute.data.RouteItem,
    vm: SmartRouteViewModel,
    onOpenRoute: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenRoute(route.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Blue900),
                contentAlignment = Alignment.Center
            ) {
                Icon(route.transport.icon, null, tint = Color.White)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    route.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Text(
                    "${route.minutes} min · ${"%.1f".format(route.km)} km",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { vm.toggleFavorite(route.id) }
            ) {
                Icon(
                    if (route.favorite) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (route.favorite) Danger else Muted
                )
            }
        }
    }
}
