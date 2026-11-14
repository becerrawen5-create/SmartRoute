package com.example.smartroute.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.data.RoutePreference
import com.example.smartroute.data.Transport
import com.example.smartroute.network.PlaceSuggestion
import com.example.smartroute.network.PlacesRepository
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

@Composable
fun CreateRouteScreen(vm: SmartRouteViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    val context = LocalContext.current
    StatusBarStyle(darkIcons = true)

    val placesRepo = remember { PlacesRepository(context) }
    val scope = rememberCoroutineScope()

    var fromText by remember { mutableStateOf("Mi ubicación actual") }
    var toText by remember { mutableStateOf("") }

    // Coordenadas dinámicas obtenidas del sensor GPS (GeoPoint osmdroid)
    var fromPoint by remember { mutableStateOf<GeoPoint?>(vm.currentUserLocation) }
    var toPoint by remember { mutableStateOf<GeoPoint?>(null) }

    var pref by remember { mutableStateOf(vm.defaultPreference) }
    var transport by remember { mutableStateOf(vm.defaultTransport) }

    // Estado del autocompletado de Photon (Komoot / OpenStreetMap)
    var activeField by remember { mutableStateOf<String?>("to") } // "from" o "to"
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var isSearchingPlaces by remember { mutableStateOf(false) }

    // Obtener la ubicación GPS real y dinámica de alta precisión al abrir la pantalla
    LaunchedEffect(Unit) {
        vm.initLocation(context)
        vm.refreshLocation(context) { loc ->
            if (loc != null && (fromPoint == null || fromText == "Mi ubicación actual")) {
                fromPoint = loc
            }
        }
    }

    // Actualizar fromPoint si currentUserLocation cambia y fromText sigue siendo la ubicación actual
    LaunchedEffect(vm.currentUserLocation) {
        if (fromText == "Mi ubicación actual" && vm.currentUserLocation != null) {
            fromPoint = vm.currentUserLocation
        }
    }

    // Búsqueda en tiempo real con debounce de 400ms consultando la API pública de Photon (Komoot)
    LaunchedEffect(fromText, toText, activeField) {
        val query = if (activeField == "from") fromText else if (activeField == "to") toText else ""
        if (query.length >= 2 && activeField != null && query != "Mi ubicación actual" && !query.startsWith("Punto en mapa")) {
            delay(400) // Debounce de 400ms
            isSearchingPlaces = true
            suggestions = placesRepo.getPredictions(query)
            isSearchingPlaces = false
        } else {
            suggestions = emptyList()
        }
    }

    val canSearch = fromText.isNotBlank() && toText.isNotBlank() && fromPoint != null && toPoint != null && !vm.isCalculatingRoute

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
                        value = fromText,
                        onValueChange = {
                            fromText = it
                            activeField = "from"
                        },
                        label = { Text("Desde (Origen)") },
                        singleLine = true,
                        leadingIcon = {
                            IconButton(onClick = {
                                vm.refreshLocation(context) { loc ->
                                    if (loc != null) {
                                        fromPoint = loc
                                        fromText = "Mi ubicación actual"
                                        Toast.makeText(
                                            context,
                                            "📍 GPS fijado: ${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Verifica los permisos de ubicación GPS", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.MyLocation, "Usar GPS", tint = Blue600)
                            }
                        },
                        trailingIcon = {
                            if (fromText.isNotBlank()) {
                                IconButton(onClick = { fromText = ""; fromPoint = null; activeField = "from" }) {
                                    Icon(Icons.Filled.Close, "Limpiar", tint = Muted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    fromPoint?.let {
                        Text(
                            text = "Coords GPS: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}",
                            fontSize = 11.sp,
                            color = Blue600,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = toText,
                        onValueChange = {
                            toText = it
                            activeField = "to"
                        },
                        label = { Text("Hasta (Destino)") },
                        placeholder = { Text("Toca el mapa o busca dirección...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Place, null, tint = Danger) },
                        trailingIcon = {
                            if (toText.isNotBlank()) {
                                IconButton(onClick = { toText = ""; toPoint = null; activeField = "to" }) {
                                    Icon(Icons.Filled.Close, "Limpiar", tint = Muted)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    toPoint?.let {
                        Text(
                            text = "Coords Destino: ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}",
                            fontSize = 11.sp,
                            color = Danger,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    val tText = fromText; fromText = toText; toText = tText
                    val tLoc = fromPoint; fromPoint = toPoint; toPoint = tLoc
                }) {
                    Icon(Icons.Filled.SwapVert, "Invertir", tint = Blue600)
                }
            }
        }

        // Sugerencias en tiempo real de Photon (OpenStreetMap) sin API Key
        if (suggestions.isNotEmpty() && activeField != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Blue100.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = Blue600, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Sugerencias de OpenStreetMap (Photon):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue900
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSearchingPlaces) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val geo = GeoPoint(suggestion.latitude, suggestion.longitude)
                                    if (activeField == "from") {
                                        fromText = suggestion.fullText
                                        fromPoint = geo
                                    } else {
                                        toText = suggestion.fullText
                                        toPoint = geo
                                    }
                                    Toast.makeText(
                                        context,
                                        "Ubicación seleccionada: ${"%.5f".format(geo.latitude)}, ${"%.5f".format(geo.longitude)}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    activeField = null
                                    suggestions = emptyList()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Place, null, tint = Blue600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(suggestion.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
                                if (suggestion.secondaryText.isNotBlank()) {
                                    Text(suggestion.secondaryText, fontSize = 11.sp, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Vista previa interactiva de osmdroid con selección táctil y arrastre de marcadores
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Puntos en OpenStreetMap", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "Toca el mapa o arrastra los pines",
                fontSize = 11.sp,
                color = Blue600,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            RealMap(
                modifier = Modifier.fillMaxSize(),
                startPoint = fromPoint,
                endPoint = toPoint,
                startLabel = fromText.ifBlank { "Origen" },
                endLabel = toText.ifBlank { "Destino" },
                userLocation = vm.currentUserLocation,
                showControls = true,
                isStartDraggable = true,
                onStartDragEnd = { newPos ->
                    fromPoint = newPos
                    fromText = "Punto fijado (${"%.4f".format(newPos.latitude)}, ${"%.4f".format(newPos.longitude)})"
                },
                isEndDraggable = true,
                onEndDragEnd = { newPos ->
                    toPoint = newPos
                    toText = "Punto fijado (${"%.4f".format(newPos.latitude)}, ${"%.4f".format(newPos.longitude)})"
                },
                onMapClick = { clickedPos ->
                    if (activeField == "from") {
                        fromPoint = clickedPos
                        fromText = "Punto en mapa (${"%.4f".format(clickedPos.latitude)}, ${"%.4f".format(clickedPos.longitude)})"
                        activeField = "to"
                    } else {
                        toPoint = clickedPos
                        toText = "Punto en mapa (${"%.4f".format(clickedPos.latitude)}, ${"%.4f".format(clickedPos.longitude)})"
                    }
                    Toast.makeText(
                        context,
                        "Coordenadas: ${"%.5f".format(clickedPos.latitude)}, ${"%.5f".format(clickedPos.longitude)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Preferencias de viaje", color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PrefItem(Icons.Filled.Speed, RoutePreference.RAPIDA, pref) { pref = it }
            PrefItem(Icons.Filled.Shield, RoutePreference.SEGURA, pref) { pref = it }
            PrefItem(Icons.Filled.Traffic, RoutePreference.EVITAR_TRAFICO, pref) { pref = it }
        }

        Spacer(Modifier.height(14.dp))
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

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val origin = fromPoint
                val dest = toPoint

                if (origin == null || dest == null) {
                    Toast.makeText(context, "Por favor selecciona un origen y destino válidos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    val id = vm.calculateRoute(
                        from = fromText,
                        to = toText,
                        startLat = origin.latitude,
                        startLng = origin.longitude,
                        endLat = dest.latitude,
                        endLng = dest.longitude,
                        transport = transport,
                        pref = pref
                    )
                    if (id > 0) {
                        Toast.makeText(
                            context,
                            "¡Ruta guardada exitosamente!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onCreated()
                    } else {
                        Toast.makeText(context, "No se pudo registrar la ruta. Verifica tu sesión.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = canSearch,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50)
        ) {
            if (vm.isCalculatingRoute) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("Guardando ruta con OSRM…", fontWeight = FontWeight.Bold)
            } else {
                Text("Buscar y guardar ruta", fontWeight = FontWeight.Bold)
            }
        }

        if ((fromPoint == null || toPoint == null) && !vm.isCalculatingRoute) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Busca una dirección o toca sobre el mapa para establecer origen y destino.",
                color = Muted,
                fontSize = 12.sp
            )
        }

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
