package com.example.smartroute.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroute.SmartRouteViewModel
import com.example.smartroute.ui.components.BrandText
import com.example.smartroute.ui.components.SmartLogo
import com.example.smartroute.ui.components.StatusBarStyle
import com.example.smartroute.ui.theme.*

@Composable
fun LoginScreen(vm: SmartRouteViewModel, onLoggedIn: () -> Unit, onNavigateToRegister: () -> Unit) {
    StatusBarStyle(darkIcons = true)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartLogo(size = 84.dp)
        BrandText(fontSize = 30.sp, color = Blue900)
        Spacer(Modifier.height(20.dp))
        Text("Inicia sesión para continuar", fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar contraseña")
                }
            },
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Recordarme", fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { }) { Text("¿Olvidaste tu contraseña?", fontSize = 12.sp) }
        }
        error?.let {
            Text(it, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = {
                if(email.isBlank() || password.isBlank()){
                    error = "Rellena los campos por favor"
                }else{
                    vm.login(email, password){ exito ->
                        if (exito){
                            onLoggedIn()
                        }else{
                            error = "El correo no esta registrado en la base de datos"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(16.dp))
        Text("o continúa con", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onLoggedIn, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Google")
            }
            OutlinedButton(onClick = onLoggedIn, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Apple")
            }
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes una cuenta? Registrate", color = Blue900, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
