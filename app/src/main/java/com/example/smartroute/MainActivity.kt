package com.example.smartroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smartroute.navigation.SmartRouteApp
import com.example.smartroute.ui.theme.SmartRouteTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Configuración temprana de osmdroid con User-Agent único y caché dedicada antes de instanciar MapView
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "$packageName/1.0 (Android App; SmartRoute AI)"
        osmConfig.osmdroidBasePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")

        setContent {
            SmartRouteTheme {
                SmartRouteApp()
            }
        }
    }
}
