package com.example.smartroute
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smartroute.navigation.SmartRouteApp
import com.example.smartroute.ui.theme.SmartRouteTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRouteTheme {
                SmartRouteApp()
            }
        }
    }
}
