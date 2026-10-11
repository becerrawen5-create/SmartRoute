package com.example.smartroute.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.smartroute.ui.theme.Blue900
import com.example.smartroute.ui.theme.Danger
import com.example.smartroute.ui.theme.Ink
import java.nio.file.WatchEvent

@Composable
fun RegisterScreen(
    vm: SmartRouteViewModel,
    onRegistered:()-> Unit,
    onBackToLogin:()-> Unit
) {
    StatusBarStyle(darkIcons = true)

    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartLogo(size = 72.dp)
        BrandText(fontSize = 26.sp, color = Blue900)
        Spacer(Modifier.height(12.dp))
        Text("Crea una nueva cuenta", fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {nombre = it; error=null},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {Text("Nombre(s)")},
            leadingIcon = { Icon(Icons.Filled.Person, null) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apellidoPaterno,
            onValueChange = {apellidoPaterno = it; error=null},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {Text("Apellido paterno")},
            leadingIcon = { Icon(Icons.Filled.Person, null) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apellidoMaterno,
            onValueChange = {apellidoMaterno = it; error= null},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {Text("Apellido materno")},
            leadingIcon = { Icon(Icons.Filled.Person, null) }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {email = it; error=null},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {Text("Correo electronico")},
            leadingIcon = { Icon(Icons.Filled.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {password = it; error= null},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {Text("Contraseña")},
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = {showPass = !showPass}) {
                    Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar contraseña")
                }
            },
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(20.dp))

        error?.let {
            Text(it, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = {
                if(nombre.isBlank() || apellidoPaterno.isBlank() || apellidoMaterno.isBlank() || email.isBlank()|| password.isBlank()){
                    error="Por favor completa los campos obligatorios"
                    return@Button
                }
                isLoading = true
                vm.registerUser(nombre, apellidoPaterno, apellidoMaterno, email, password){
                    exito-> isLoading=false
                    if(exito){
                        onRegistered()
                    }else{
                        error="Error al registrar el usuario"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if(isLoading){
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }else{
                Text("Registrarse", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("¿Ya tienes una cuenta? Inicia sesion", color = Blue900, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

