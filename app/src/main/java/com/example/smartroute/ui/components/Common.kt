package com.example.smartroute.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.smartroute.data.TrafficLevel
import com.example.smartroute.ui.theme.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Proveedor de Tiles público de Esri World Street Map.
 * Rápido, nítido, sin marcas de agua, sin API Key y gratuito.
 * Formato de URL de ArcGIS: {baseUrl}/{z}/{y}/{x}
 */
val EsriWorldStreetMapTileSource = object : OnlineTileSourceBase(
    "EsriWorldStreetMap",
    0,
    19,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
    }
}

@Composable
fun RealMap(
    modifier: Modifier = Modifier,
    startPoint: GeoPoint? = null,
    endPoint: GeoPoint? = null,
    startLabel: String = "Origen",
    endLabel: String = "Destino",
    routePoints: List<GeoPoint> = emptyList(),
    polylineColor: Color = Blue600,
    userLocation: GeoPoint? = null,
    showControls: Boolean = true,
    isStartDraggable: Boolean = false,
    onStartDragEnd: ((GeoPoint) -> Unit)? = null,
    isEndDraggable: Boolean = false,
    onEndDragEnd: ((GeoPoint) -> Unit)? = null,
    onMapClick: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Instancia de MapView de osmdroid usando Esri World Street Map (sin marcas de agua ni API Key)
    val mapView = remember {
        MapView(context).apply {
            setTileSource(EsriWorldStreetMapTileSource)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
            val fallbackPos = userLocation ?: startPoint ?: GeoPoint(19.432608, -99.133209)
            controller.setCenter(fallbackPos)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Actualizar overlays y renderizado del mapa
    LaunchedEffect(
        startPoint,
        endPoint,
        routePoints,
        userLocation,
        polylineColor,
        hasLocationPermission
    ) {
        mapView.overlays.clear()

        // 1. Overlay de eventos táctiles para clic en mapa
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    onMapClick?.invoke(p)
                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        mapView.overlays.add(eventsOverlay)

        // 2. Capa de ubicación en tiempo real del usuario
        if (hasLocationPermission) {
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                enableMyLocation()
            }
            mapView.overlays.add(myLocationOverlay)
        }

        // 3. Trazado de ruta con Polyline
        if (routePoints.isNotEmpty()) {
            val polyline = Polyline(mapView).apply {
                setPoints(routePoints)
                outlinePaint.color = polylineColor.toArgb()
                outlinePaint.strokeWidth = 14f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }
            mapView.overlays.add(polyline)
        } else if (startPoint != null && endPoint != null) {
            val directLine = Polyline(mapView).apply {
                setPoints(listOf(startPoint, endPoint))
                outlinePaint.color = polylineColor.copy(alpha = 0.8f).toArgb()
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            mapView.overlays.add(directLine)
        }

        // 4. Marcador de Inicio
        if (startPoint != null) {
            val startMarker = Marker(mapView).apply {
                position = startPoint
                title = startLabel
                snippet = "Punto de partida"
                isDraggable = isStartDraggable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (isStartDraggable) {
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker?) {}
                        override fun onMarkerDragStart(marker: Marker?) {}
                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let { onStartDragEnd?.invoke(it) }
                        }
                    })
                }
            }
            mapView.overlays.add(startMarker)
        }

        // 5. Marcador de Fin
        if (endPoint != null) {
            val endMarker = Marker(mapView).apply {
                position = endPoint
                title = endLabel
                snippet = "Punto de destino"
                isDraggable = isEndDraggable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (isEndDraggable) {
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker?) {}
                        override fun onMarkerDragStart(marker: Marker?) {}
                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let { onEndDragEnd?.invoke(it) }
                        }
                    })
                }
            }
            mapView.overlays.add(endMarker)
        }

        // 6. Centrado automático de la cámara y límites (BoundingBox)
        try {
            if (routePoints.size >= 2) {
                val minLat = routePoints.minOf { it.latitude }
                val maxLat = routePoints.maxOf { it.latitude }
                val minLon = routePoints.minOf { it.longitude }
                val maxLon = routePoints.maxOf { it.longitude }
                val box = BoundingBox(maxLat + 0.005, maxLon + 0.005, minLat - 0.005, minLon - 0.005)
                mapView.zoomToBoundingBox(box, true, 120)
            } else if (startPoint != null && endPoint != null && startPoint != endPoint) {
                val minLat = minOf(startPoint.latitude, endPoint.latitude)
                val maxLat = maxOf(startPoint.latitude, endPoint.latitude)
                val minLon = minOf(startPoint.longitude, endPoint.longitude)
                val maxLon = maxOf(startPoint.longitude, endPoint.longitude)
                val box = BoundingBox(maxLat + 0.006, maxLon + 0.006, minLat - 0.006, minLon - 0.006)
                mapView.zoomToBoundingBox(box, true, 120)
            } else if (startPoint != null) {
                mapView.controller.animateTo(startPoint)
                mapView.controller.setZoom(16.0)
            } else if (userLocation != null) {
                mapView.controller.animateTo(userLocation)
                mapView.controller.setZoom(16.0)
            }
        } catch (_: Exception) {
            val center = startPoint ?: userLocation ?: GeoPoint(19.432608, -99.133209)
            mapView.controller.setCenter(center)
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

/** Cambia el color de los iconos de la barra de estado según el fondo de la pantalla. */
@Composable
fun StatusBarStyle(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
    }
}

@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Blue900, Blue800, Blue600)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
fun BackTitleRow(
    title: String,
    onBack: () -> Unit,
    tint: Color = Color.White,
    trailing: @Composable () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = tint)
        }
        Text(title, color = tint, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SmartLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Yellow.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = Yellow, modifier = Modifier.size(size * 0.72f))
    }
}

@Composable
fun BrandText(fontSize: TextUnit, color: Color = Color.White) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append("SmartRoute ") }
            withStyle(SpanStyle(color = Yellow)) { append("AI") }
        },
        fontSize = fontSize,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

fun trafficColor(level: TrafficLevel): Color = when (level) {
    TrafficLevel.FLUIDO -> Success
    TrafficLevel.MODERADO -> Warning
    TrafficLevel.LENTO -> Danger
}

fun formatDuration(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    showRoute: Boolean = true,
    showTraffic: Boolean = false
) {
    Canvas(modifier.background(Color(0xFFE9EFE4))) {
        val w = size.width
        val h = size.height
        val street = Color.White

        for (i in 0..6) {
            val x = w * i / 6f
            drawLine(street, Offset(x, 0f), Offset(x + (i - 3) * 14f, h), strokeWidth = if (i % 3 == 0) 16f else 9f)
        }
        for (j in 0..8) {
            val y = h * j / 8f
            drawLine(street, Offset(0f, y), Offset(w, y + (j - 4) * 10f), strokeWidth = if (j % 4 == 0) 16f else 9f)
        }
        drawRect(Color(0xFFCDE6C6), Offset(w * 0.62f, h * 0.58f), Size(w * 0.24f, h * 0.13f))
        drawRect(Color(0xFFCDE0F5), Offset(w * 0.05f, h * 0.18f), Size(w * 0.2f, h * 0.1f))

        if (showTraffic) {
            fun seg(color: Color, a: Offset, b: Offset) =
                drawLine(color, a, b, strokeWidth = 12f, cap = StrokeCap.Round)
            seg(Success, Offset(0f, h * 0.25f), Offset(w * 0.5f, h * 0.25f + 10f))
            seg(Warning, Offset(w * 0.5f, h * 0.25f + 10f), Offset(w, h * 0.25f + 30f))
            seg(Danger, Offset(w * 0.33f, 0f), Offset(w * 0.33f + 2f, h * 0.55f))
            seg(Success, Offset(w * 0.33f + 2f, h * 0.55f), Offset(w * 0.33f + 8f, h))
            seg(Danger, Offset(0f, h * 0.62f), Offset(w * 0.6f, h * 0.62f + 8f))
            seg(Warning, Offset(w * 0.6f, h * 0.62f + 8f), Offset(w, h * 0.62f + 20f))
            seg(Success, Offset(w * 0.83f, 0f), Offset(w * 0.83f + 15f, h))
            seg(Warning, Offset(w * 0.16f, 0f), Offset(w * 0.16f - 15f, h * 0.7f))
        }

        if (showRoute) {
            val start = Offset(w * 0.15f, h * 0.82f)
            val end = Offset(w * 0.82f, h * 0.22f)
            val p = Path().apply {
                moveTo(start.x, start.y)
                lineTo(w * 0.15f, h * 0.66f)
                lineTo(w * 0.35f, h * 0.6f)
                lineTo(w * 0.42f, h * 0.42f)
                lineTo(w * 0.75f, h * 0.4f)
                lineTo(end.x, end.y)
            }
            drawPath(p, Blue600, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Color.White, 20f, start)
            drawCircle(Blue600, 13f, start)
            drawCircle(Danger, 16f, end)
            drawCircle(Color.White, 6f, end)
        }
    }
}
