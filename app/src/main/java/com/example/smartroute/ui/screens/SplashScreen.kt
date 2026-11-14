package com.example.smartroute.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.R
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.StatusBarStyle
import com.example.smartroute.ui.theme.Ink
import com.example.smartroute.ui.theme.Yellow

@Composable
fun SplashScreen(
    vm: SmartRouteViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    StatusBarStyle(darkIcons = false)
    val hasSession = vm.auth.currentUser != null

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.smartroute_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Button(
                onClick = {
                    if (hasSession) {
                        onNavigateToHome()
                    } else {
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow,
                    contentColor = Ink
                )
            ) {
                val label = if (hasSession) {
                    val name = vm.userName.ifBlank { "Usuario" }.substringBefore(' ')
                    "Continuar como $name"
                } else {
                    "Comenzar"
                }
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}