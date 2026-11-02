package com.example.smartroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smartroute.navigation.SmartRouteApp
import com.example.smartroute.ui.theme.SmartRouteTheme
// IMPORTA ESTO:
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // En MainActivity.kt onCreate()
        Configuration.getInstance().apply {
            // Usa el nombre del paquete o un string identificable
            userAgentValue = "com.example.smartroute"
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid-tiles").apply { mkdirs() }
        }

        setContent {
            SmartRouteTheme {
                SmartRouteApp()
            }
        }
    }
}