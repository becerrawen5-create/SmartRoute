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
import com.example.smartroute.data.NominatimResult
import com.example.smartroute.data.RoutePreference
import com.example.smartroute.data.Transport
import com.example.smartroute.ui.components.*
import com.example.smartroute.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRouteScreen(vm: SmartRouteViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    StatusBarStyle(darkIcons = true)
    var from by remember { mutableStateOf("Mi ubicación") }
    var to by remember { mutableStateOf("") }
    var pref by remember { mutableStateOf(RoutePreference.RAPIDA) }
    var transport by remember { mutableStateOf(Transport.AUTO) }

    val scope = rememberCoroutineScope()
    val canSearch = from.isNotBlank() && to.isNotBlank() && !vm.isCalculatingRoute

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTitleRow("Crear ruta", onBack, tint = Ink)

        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {

                    // --- CAMPO DESDE ---
                    AutocompleteTextField(
                        value = from,
                        onValueChange = {
                            from = it
                            vm.searchAddress(it, isOrigin = true)
                        },
                        label = "Desde",
                        leadingIcon = Icons.Filled.MyLocation,
                        iconTint = Blue600,
                        suggestions = vm.fromSuggestions,
                        onClearSuggestions = { vm.fromSuggestions = emptyList() },
                        onSuggestionSelected = { result ->
                            // ✅ CORREGIDO: Usamos display_name
                            from = result.display_name
                            vm.fromSuggestions = emptyList()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // --- CAMPO HASTA ---
                    AutocompleteTextField(
                        value = to,
                        onValueChange = {
                            to = it
                            vm.searchAddress(it, isOrigin = false)
                        },
                        label = "Hasta",
                        leadingIcon = Icons.Filled.Place,
                        iconTint = Danger,
                        suggestions = vm.toSuggestions,
                        onClearSuggestions = { vm.toSuggestions = emptyList() },
                        onSuggestionSelected = { result ->
                            // ✅ CORREGIDO: Usamos display_name
                            to = result.display_name
                            vm.toSuggestions = emptyList()
                        }
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
                scope.launch {
                    vm.calculateRoute(from, to, transport, pref)
                    onCreated()
                }
            },
            enabled = canSearch,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50)
        ) {
            if (vm.isCalculatingRoute) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Calculando ruta…", fontWeight = FontWeight.Bold)
            } else {
                Text("Buscar ruta", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// --- NUEVO COMPONENTE REUTILIZABLE PARA EL AUTOCOMPLETADO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    iconTint: Color,
    suggestions: List<NominatimResult>,
    onClearSuggestions: () -> Unit,
    onSuggestionSelected: (NominatimResult) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // ESTO ES CLAVE: Si llegan sugerencias nuevas, abrimos el menú a la fuerza
    LaunchedEffect(suggestions) {
        if (suggestions.isNotEmpty()) {
            expanded = true
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (it.length < 3) expanded = false
            },
            label = { Text(label) },
            singleLine = true,
            leadingIcon = { Icon(leadingIcon, null, tint = iconTint) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable) // Asegura que el menú se ancle al texto
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = {
                expanded = false
                onClearSuggestions()
            }
        ) {
            suggestions.forEach { feature ->
                DropdownMenuItem(
                    text = { Text(formatSuggestion(feature), fontSize = 14.sp) },
                    onClick = {
                        onSuggestionSelected(feature)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatSuggestion(result: NominatimResult): String {
    // Nominatim devuelve el nombre completo en display_name
    return result.display_name
}

@Composable
private fun PrefItem(icon: ImageVector, value: RoutePreference, selected: RoutePreference, onSelect: (RoutePreference) -> Unit) {
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