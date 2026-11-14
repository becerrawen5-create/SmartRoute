package com.example.smartroute.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    vm: SmartRouteViewModel,
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    StatusBarStyle(darkIcons = true)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                isLoading = true
                error = null
                vm.loginWithGoogle(idToken) { ok, err ->
                    isLoading = false
                    if (ok) {
                        Toast.makeText(context, "¡Sesión iniciada con Google correctamente!", Toast.LENGTH_SHORT).show()
                        onLoggedIn()
                    } else {
                        val fullErr = err ?: "Fallo al iniciar sesión con Google en Firebase."
                        error = fullErr
                        Toast.makeText(context, fullErr, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                isLoading = false
                val msg = "No se pudo obtener la credencial de la cuenta de Google seleccionada."
                error = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            isLoading = false
            val friendly = when (e.statusCode) {
                10 -> "[Código 10: DEVELOPER_ERROR] La huella SHA-1 de esta máquina no coincide con Firebase Console para com.example.smartroute."
                7 -> "[Código 7: NETWORK_ERROR] Error de conexión a internet o servicios de Google inaccesibles."
                12500 -> "[Código 12500: SIGN_IN_FAILED] Proveedor Google no habilitado en Firebase Authentication."
                12501, 16 -> "Inicio de sesión cancelado."
                else -> "[Código ${e.statusCode}] ${e.localizedMessage ?: "Error en Google Play Services"}"
            }
            error = friendly
            Toast.makeText(context, friendly, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            isLoading = false
            val msg = e.localizedMessage ?: "Error inesperado al conectar con Google."
            error = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

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
            TextButton(onClick = {
                resetEmail = email
                showResetDialog = true
            }) { Text("¿Olvidaste tu contraseña?", fontSize = 12.sp) }
        }

        // Alerta visual de error con detalles exactos de Firebase / Google
        error?.let { errorMsg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = Danger,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Detalle del error:",
                            color = Danger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = errorMsg,
                            color = Danger,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        infoMessage?.let {
            Text(it, color = Success, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "Por favor completa todos los campos"
                    return@Button
                } else {
                    isLoading = true
                    error = null
                    vm.loginWithAuth(email, password) { exito, mensajeError ->
                        isLoading = false
                        if (exito) {
                            Toast.makeText(context, "¡Bienvenido a SmartRoute!", Toast.LENGTH_SHORT).show()
                            onLoggedIn()
                        } else {
                            error = mensajeError ?: "Correo o contraseña incorrectos"
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Iniciar sesión", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("o continúa con", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        // Botón de autenticación con Google y Firebase Credential Manager
        OutlinedButton(
            onClick = {
                if (activity == null) {
                    val msg = "No se pudo acceder a la actividad para iniciar sesión con Google."
                    error = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }

                scope.launch {
                    isLoading = true
                    error = null
                    val authResult = vm.signInWithGoogleCredential(activity)
                    authResult.fold(
                        onSuccess = { idToken ->
                            vm.loginWithGoogle(idToken) { ok, err ->
                                isLoading = false
                                if (ok) {
                                    Toast.makeText(context, "¡Sesión iniciada con Google correctamente!", Toast.LENGTH_SHORT).show()
                                    onLoggedIn()
                                } else {
                                    val fullErr = err ?: "Fallo al iniciar sesión con Google en Firebase."
                                    error = fullErr
                                    Toast.makeText(context, fullErr, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onFailure = { ex ->
                            val isNoCredential = ex.message?.contains("NoCredentialException", ignoreCase = true) == true
                            if (isNoCredential) {
                                // Fallback inteligente: si CredentialManager no detecta cuentas registradas para esta firma,
                                // abrimos el selector nativo clásico de Google Play Services con opción de agregar cuenta
                                try {
                                    val client = vm.getGoogleSignInClient(activity)
                                    googleLauncher.launch(client.signInIntent)
                                } catch (e: Exception) {
                                    isLoading = false
                                    error = "Fallo al abrir selector clásico de Google: ${e.localizedMessage}"
                                }
                            } else {
                                isLoading = false
                                val exactMsg = ex.message ?: "Error al autenticar con Google."
                                error = exactMsg
                                Toast.makeText(context, exactMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Blue900
                )
                Spacer(Modifier.width(8.dp))
                Text("Conectando con Google...", fontWeight = FontWeight.SemiBold)
            } else {
                Text("Continuar con Google", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes una cuenta? Regístrate", color = Blue900, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Column {
                    Text("Ingresa tu correo para enviarte las instrucciones de restablecimiento.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    vm.sendPasswordReset(resetEmail) { ok, msg ->
                        if (ok) infoMessage = msg else error = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
