package com.example.smartroute.ui.components

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.smartroute.data.TrafficLevel
import com.example.smartroute.ui.theme.*

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

/** Mapa estilizado dibujado con Canvas (sin necesidad de API key). */
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
