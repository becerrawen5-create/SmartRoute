package com.example.smartroute

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartroute.ai.RecommendationEngine
import com.example.smartroute.data.*
import com.example.smartroute.network.DirectionsApiClient
import com.example.smartroute.network.LocationHelper
import com.example.smartroute.network.WeatherApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class SmartRouteViewModel : ViewModel() {
    private val db = Firebase.firestore
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Identificador del usuario autenticado en la base de datos
    var currentUserId by mutableStateOf<String?>(auth.currentUser?.uid)
        private set

    // Datos del usuario autenticado (se cargan en tiempo real desde Firestore)
    var userName by mutableStateOf(auth.currentUser?.displayName ?: "")
    var email by mutableStateOf(auth.currentUser?.email ?: "")

    // Ubicación GPS dinámica en tiempo real (GeoPoint de osmdroid)
    var currentUserLocation by mutableStateOf<GeoPoint?>(null)
        private set

    private var locationHelper: LocationHelper? = null

    // Colecciones reactivas filtradas exclusivamente por el usuario activo (inician vacías)
    val routes = mutableStateListOf<RouteItem>()
    val history = mutableStateListOf<HistoryEntry>()

    // Telemetría en tiempo real desde Firestore y APIs externas (Open-Meteo y OSRM)
    var weather by mutableStateOf(Weather())
    var isLoadingWeather by mutableStateOf(false)
        private set
    var weatherError by mutableStateOf<String?>(null)
        private set

    val zones = mutableStateListOf<TrafficZone>()
    var currentTrafficReport by mutableStateOf<TrafficReport?>(null)
        private set
    var isLoadingTraffic by mutableStateOf(false)
        private set

    // Fuentes IoT (controladas por usuario y sincronizadas con Firestore)
    val iotSources = mutableStateMapOf<String, Boolean>().apply {
        put("Smartphone / GPS", true)
        put("Smartwatch", true)
        put("Sensores ambientales", true)
        put("Sensores de tráfico", true)
        put("Vehículo conectado", true)
    }

    // Preferencias del usuario en BD
    var defaultPreference by mutableStateOf(RoutePreference.RAPIDA)
    var defaultTransport by mutableStateOf(Transport.AUTO)
    var trafficAlertsEnabled by mutableStateOf(true)
    var weatherAlertsEnabled by mutableStateOf(true)
    var recommendationAlertsEnabled by mutableStateOf(true)

    // Estados de UI y selección
    var selectedRouteId by mutableIntStateOf(0)
    var acceptedAdaptations by mutableIntStateOf(0)
    var learnedMessage by mutableStateOf<String?>(null)

    var isCalculatingRoute by mutableStateOf(false)
        private set
    var isLoadingData by mutableStateOf(false)
        private set

    // Referencias a listeners de Firestore para desasociar en logout
    private var userListener: ListenerRegistration? = null
    private var routesListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    init {
        // Si ya hay sesión activa persistida en Firebase Auth, vincular de inmediato
        auth.currentUser?.uid?.let { uid ->
            attachUserListeners(uid)
        }
    }

    val selectedRoute: RouteItem?
        get() = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()

    val featuredRoute: RouteItem?
        get() = routes.firstOrNull { it.favorite } ?: routes.firstOrNull()

    // ==========================================
    // GEOLOCALIZACIÓN DINÁMICA CON FusedLocationProviderClient
    // ==========================================
    fun initLocation(context: Context) {
        if (locationHelper == null) {
            locationHelper = LocationHelper(context)
        }
        val helper = locationHelper ?: return
        if (!helper.hasLocationPermission()) return

        viewModelScope.launch {
            val immediate = helper.getCurrentLocation()
            if (immediate != null) {
                currentUserLocation = immediate
            }

            helper.requestLocationUpdates(5000L).collect { updatedLocation ->
                currentUserLocation = updatedLocation
            }
        }
    }

    fun refreshLocation(context: Context, onResult: ((GeoPoint?) -> Unit)? = null) {
        if (locationHelper == null) {
            locationHelper = LocationHelper(context)
        }
        val helper = locationHelper ?: return
        viewModelScope.launch {
            val loc = helper.getCurrentLocation()
            if (loc != null) {
                currentUserLocation = loc
            }
            onResult?.invoke(loc ?: currentUserLocation)
        }
    }

    // ==========================================
    // ESCUCHA EN TIEMPO REAL DE FIRESTORE (POR USUARIO)
    // ==========================================
    fun attachUserListeners(userId: String) {
        currentUserId = userId
        isLoadingData = true

        // 1. Perfil del usuario (users/{userId})
        userListener?.remove()
        userListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreUser", "Error al escuchar usuario: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("Nombre(s)") ?: ""
                    val paterno = snapshot.getString("Apellido paterno") ?: ""
                    val fullName = if (paterno.isNotBlank()) "$nombre $paterno" else nombre
                    userName = fullName.ifBlank { snapshot.getString("email")?.substringBefore("@") ?: (auth.currentUser?.displayName ?: "Usuario") }
                    email = snapshot.getString("email") ?: (auth.currentUser?.email ?: "")

                    snapshot.getString("defaultPreference")?.let {
                        try { defaultPreference = RoutePreference.valueOf(it) } catch (_: Exception) {}
                    }
                    snapshot.getString("defaultTransport")?.let {
                        try { defaultTransport = Transport.valueOf(it) } catch (_: Exception) {}
                    }
                    snapshot.getBoolean("trafficAlertsEnabled")?.let { trafficAlertsEnabled = it }
                    snapshot.getBoolean("weatherAlertsEnabled")?.let { weatherAlertsEnabled = it }
                    snapshot.getBoolean("recommendationAlertsEnabled")?.let { recommendationAlertsEnabled = it }

                    val remoteIot = snapshot.get("iotSources") as? Map<*, *>
                    if (remoteIot != null) {
                        remoteIot.forEach { (k, v) ->
                            if (k is String && v is Boolean) {
                                iotSources[k] = v
                            }
                        }
                    }
                }
            }

        // 2. Rutas del usuario (users/{userId}/routes)
        routesListener?.remove()
        routesListener = db.collection("users").document(userId).collection("routes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                isLoadingData = false
                if (error != null) {
                    Log.e("FirestoreRoutes", "Error al escuchar rutas del usuario: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    routes.clear()
                    for (doc in snapshots.documents) {
                        val id = doc.getLong("id")?.toInt() ?: 0
                        val uid = doc.getString("uid") ?: userId
                        val from = doc.getString("from") ?: ""
                        val to = doc.getString("to") ?: ""
                        val startLat = doc.getDouble("startLat") ?: (currentUserLocation?.latitude ?: 19.432608)
                        val startLng = doc.getDouble("startLng") ?: (currentUserLocation?.longitude ?: -99.133209)
                        val endLat = doc.getDouble("endLat") ?: startLat
                        val endLng = doc.getDouble("endLng") ?: startLng
                        val minutes = doc.getLong("minutes")?.toInt() ?: 0
                        val km = doc.getDouble("km") ?: 0.0
                        val transportStr = doc.getString("transport") ?: Transport.AUTO.name
                        val transport = try { Transport.valueOf(transportStr) } catch (_: Exception) { Transport.AUTO }
                        val favorite = doc.getBoolean("favorite") ?: false
                        val trafficStr = doc.getString("traffic") ?: TrafficLevel.FLUIDO.name
                        val traffic = try { TrafficLevel.valueOf(trafficStr) } catch (_: Exception) { TrafficLevel.FLUIDO }
                        val prefStr = doc.getString("preference") ?: RoutePreference.RAPIDA.name
                        val pref = try { RoutePreference.valueOf(prefStr) } catch (_: Exception) { RoutePreference.RAPIDA }
                        val departure = doc.getString("departure") ?: "7:30 a.m."

                        routes.add(
                            RouteItem(
                                id = id,
                                uid = uid,
                                from = from,
                                to = to,
                                startLat = startLat,
                                startLng = startLng,
                                endLat = endLat,
                                endLng = endLng,
                                minutes = minutes,
                                km = km,
                                transport = transport,
                                favorite = favorite,
                                traffic = traffic,
                                preference = pref,
                                departure = departure,
                                firestoreDocId = doc.id
                            )
                        )
                    }
                    if (routes.isNotEmpty() && routes.none { it.id == selectedRouteId }) {
                        selectedRouteId = routes.first().id
                    }
                }
            }

        // 3. Historial del usuario (users/{userId}/history)
        historyListener?.remove()
        historyListener = db.collection("users").document(userId).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirestoreHistory", "Error al escuchar historial: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    history.clear()
                    for (doc in snapshots.documents) {
                        val id = doc.getLong("id")?.toInt() ?: 0
                        val uid = doc.getString("uid") ?: userId
                        val day = doc.getString("day") ?: "Hoy"
                        val from = doc.getString("from") ?: ""
                        val to = doc.getString("to") ?: ""
                        val startLat = doc.getDouble("startLat") ?: (currentUserLocation?.latitude ?: 19.432608)
                        val startLng = doc.getDouble("startLng") ?: (currentUserLocation?.longitude ?: -99.133209)
                        val endLat = doc.getDouble("endLat") ?: startLat
                        val endLng = doc.getDouble("endLng") ?: startLng
                        val minutes = doc.getLong("minutes")?.toInt() ?: 0
                        val km = doc.getDouble("km") ?: 0.0
                        val time = doc.getString("time") ?: ""
                        val transportStr = doc.getString("transport") ?: Transport.AUTO.name
                        val transport = try { Transport.valueOf(transportStr) } catch (_: Exception) { Transport.AUTO }

                        history.add(
                            HistoryEntry(
                                id = id,
                                uid = uid,
                                day = day,
                                from = from,
                                to = to,
                                startLat = startLat,
                                startLng = startLng,
                                endLat = endLat,
                                endLng = endLng,
                                minutes = minutes,
                                km = km,
                                time = time,
                                transport = transport,
                                firestoreDocId = doc.id
                            )
                        )
                    }
                }
            }
    }

    // ==========================================
    // AUTENTICACIÓN FIREBASE Y GOOGLE CREDENTIAL MANAGER
    // ==========================================
    fun loginWithAuth(emailInput: String, passwordInput: String, onResult: (Boolean, String?) -> Unit) {
        val cleanEmail = emailInput.trim()
        val cleanPass = passwordInput.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            onResult(false, "Por favor completa todos los campos")
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    syncUserProfile(user) {
                        attachUserListeners(user.uid)
                        onResult(true, null)
                    }
                } else {
                    onResult(false, "No se pudo iniciar sesión con las credenciales proporcionadas")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("LoginAuth", "Credenciales inválidas en Firebase: ${exception.localizedMessage}")
                val userFriendlyMessage = when {
                    exception.message?.contains("no user record", ignoreCase = true) == true ||
                            exception.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "No existe ninguna cuenta registrada con este correo."
                    exception.message?.contains("password is invalid", ignoreCase = true) == true ||
                            exception.message?.contains("wrong-password", ignoreCase = true) == true ->
                        "Contraseña incorrecta. Verifica tus datos."
                    exception.message?.contains("badly formatted", ignoreCase = true) == true ->
                        "El formato del correo electrónico no es válido."
                    else -> exception.localizedMessage ?: "Credenciales inválidas. Verifica tu correo y contraseña."
                }
                onResult(false, userFriendlyMessage)
            }
    }

    fun registerWithAuth(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        emailInput: String,
        passwordInput: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val cleanEmail = emailInput.trim()
        val cleanPass = passwordInput.trim()

        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    val nuevoUsuario = hashMapOf(
                        "Nombre(s)" to nombre.trim(),
                        "Apellido paterno" to apellidoPaterno.trim(),
                        "Apellido materno" to apellidoMaterno.trim(),
                        "email" to cleanEmail,
                        "uid" to user.uid,
                        "createdAt" to System.currentTimeMillis(),
                        "defaultPreference" to RoutePreference.RAPIDA.name,
                        "defaultTransport" to Transport.AUTO.name,
                        "trafficAlertsEnabled" to true,
                        "weatherAlertsEnabled" to true,
                        "recommendationAlertsEnabled" to true,
                        "iotSources" to hashMapOf(
                            "Smartphone / GPS" to true,
                            "Smartwatch" to true,
                            "Sensores ambientales" to true,
                            "Sensores de tráfico" to true,
                            "Vehículo conectado" to true
                        )
                    )

                    db.collection("users").document(user.uid)
                        .set(nuevoUsuario)
                        .addOnSuccessListener {
                            attachUserListeners(user.uid)
                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.w("RegisterDebug", "Error al guardar perfil en Firestore: ${e.message}", e)
                            attachUserListeners(user.uid)
                            onResult(true, null)
                        }
                } else {
                    onResult(false, "No se pudo crear la cuenta de usuario")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage ?: "Error al registrar usuario en Firebase")
            }
    }

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val serverClientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            "927563966222-3ph7vlbe8npfugkuj0kqskr3adrkpm1b.apps.googleusercontent.com"
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    suspend fun signInWithGoogleCredential(activity: Activity): Result<String> {
        val credentialManager = CredentialManager.create(activity)
        val serverClientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            "927563966222-3ph7vlbe8npfugkuj0kqskr3adrkpm1b.apps.googleusercontent.com"
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(request = request, context = activity)
            val idToken = handleCredentialResult(result)
            if (idToken != null) {
                Result.success(idToken)
            } else {
                Result.failure(Exception("No se pudo extraer el idToken de las credenciales de Google."))
            }
        } catch (e: NoCredentialException) {
            val msg = "[NoCredentialException] No hay ninguna cuenta de Google activa en este dispositivo o emulador. Agrega una cuenta en Ajustes > Contraseñas y Cuentas."
            Log.e("AuthError", msg, e)
            Result.failure(Exception(msg))
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Inicio de sesión cancelado por el usuario."))
        } catch (e: GetCredentialCustomException) {
            Log.e("AuthError", "GetCredentialCustomException: tipo=${e.type}, msg=${e.message}", e)
            val isDevError = e.message?.contains("10", ignoreCase = true) == true ||
                    e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ||
                    e.type.contains("DEVELOPER_ERROR", ignoreCase = true)
            val friendly = if (isDevError) {
                "[Error 10: DEVELOPER_ERROR] La huella digital SHA-1 de tu máquina de desarrollo no coincide con los clientes OAuth en Firebase Console para el paquete com.example.smartroute."
            } else {
                "[${e.type}] ${e.message ?: "Error al consultar credenciales de Google."}"
            }
            Result.failure(Exception(friendly))
        } catch (e: GetCredentialException) {
            val apiEx = e.cause as? ApiException
            val statusCode = apiEx?.statusCode
            val codeDesc = when (statusCode) {
                10 -> "Código 10 (DEVELOPER_ERROR): Falta registrar la huella SHA-1 de esta máquina en Firebase Console para com.example.smartroute."
                7 -> "Código 7 (NETWORK_ERROR): Fallo de conexión de red al validar con Google Play Services."
                16 -> "Código 16 (CANNOT_BE_RESOLVED): Google Play Services no pudo resolver la solicitud."
                12500 -> "Código 12500 (SIGN_IN_FAILED): Verifica que Google Sign-In esté habilitado en Firebase Authentication y el SHA-1 registrado."
                null -> "[${e.type}] ${e.localizedMessage ?: "Error al obtener credenciales"}"
                else -> "Código $statusCode: ${e.localizedMessage ?: "Error de autenticación Google"}"
            }
            Log.e("AuthError", "GetCredentialException: $codeDesc", e)
            Result.failure(Exception(codeDesc))
        } catch (e: ApiException) {
            val friendly = when (e.statusCode) {
                10 -> "[Código 10: DEVELOPER_ERROR] Huella digital SHA-1 no registrada en Firebase Console para com.example.smartroute."
                7 -> "[Código 7: NETWORK_ERROR] Error de conexión a internet o servicios de Google inaccesibles."
                16 -> "[Código 16: CANNOT_BE_RESOLVED] Google Play Services no pudo resolver la solicitud."
                12500 -> "[Código 12500: SIGN_IN_FAILED] Proveedor Google no habilitado en Firebase Authentication."
                else -> "[Código ${e.statusCode}] ${e.localizedMessage ?: "Error en Google Play Services"}"
            }
            Log.e("AuthError", "ApiException: $friendly", e)
            Result.failure(Exception(friendly))
        } catch (e: Exception) {
            val msg = "[${e.javaClass.simpleName}] ${e.localizedMessage ?: "Error inesperado al autenticar con Google"}"
            Log.e("AuthError", msg, e)
            Result.failure(Exception(msg))
        }
    }

    private fun handleCredentialResult(result: GetCredentialResponse): String? {
        val credential = result.credential
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        googleIdTokenCredential.idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("AuthError", "Fallo al parsear GoogleIdTokenCredential", e)
                        null
                    }
                } else null
            }
            else -> null
        }
    }

    fun loginWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    onResult(false, "No se pudo obtener el usuario autenticado de Firebase.")
                    return@addOnSuccessListener
                }

                syncUserProfile(user) {
                    attachUserListeners(user.uid)
                    onResult(true, null)
                }
            }
            .addOnFailureListener { e ->
                val fbCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
                val msg = if (fbCode.isNotBlank()) "[Firebase $fbCode] ${e.localizedMessage}" else e.localizedMessage
                Log.e("AuthError", "Error en signInWithCredential: $msg", e)
                onResult(false, msg ?: "Fallo al autenticar en Firebase con la credencial de Google.")
            }
    }

    private fun syncUserProfile(user: FirebaseUser, onFinished: () -> Unit) {
        val userDocRef = db.collection("users").document(user.uid)
        userDocRef.get().addOnSuccessListener { doc ->
            val displayName = user.displayName ?: ""
            val parts = displayName.split(" ")
            val nombre = if (parts.isNotEmpty()) parts.first() else "Usuario"
            val paterno = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

            val profileData = hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "lastLoginAt" to System.currentTimeMillis()
            )

            if (!doc.exists()) {
                profileData["Nombre(s)"] = nombre
                profileData["Apellido paterno"] = paterno
                profileData["Apellido materno"] = ""
                profileData["createdAt"] = System.currentTimeMillis()
                profileData["defaultPreference"] = RoutePreference.RAPIDA.name
                profileData["defaultTransport"] = Transport.AUTO.name
                profileData["trafficAlertsEnabled"] = true
                profileData["weatherAlertsEnabled"] = true
                profileData["recommendationAlertsEnabled"] = true
                profileData["iotSources"] = hashMapOf(
                    "Smartphone / GPS" to true,
                    "Smartwatch" to true,
                    "Sensores ambientales" to true,
                    "Sensores de tráfico" to true,
                    "Vehículo conectado" to true
                )
            }

            userDocRef.set(profileData, SetOptions.merge())
                .addOnCompleteListener { onFinished() }
        }.addOnFailureListener {
            onFinished()
        }
    }

    fun logout(context: Context, onFinished: () -> Unit) {
        userListener?.remove()
        routesListener?.remove()
        historyListener?.remove()

        auth.signOut()
        currentUserId = null
        userName = ""
        email = ""
        routes.clear()
        history.clear()
        selectedRouteId = 0
        currentTrafficReport = null

        viewModelScope.launch {
            try {
                CredentialManager.create(context.applicationContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Logout", "No se puede limpiar el estado de credenciales", e)
            }
            onFinished()
        }
    }

    fun sendPasswordReset(emailInput: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = emailInput.trim()
        if (cleanEmail.isBlank()) {
            onResult(false, "Ingresa tu correo para restablecer la contraseña.")
            return
        }
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnSuccessListener {
                onResult(true, "Se ha enviado un enlace de recuperación a $cleanEmail")
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage ?: "No se pudo enviar el correo de recuperación.")
            }
    }

    fun validatePasswordSecurity(password: String): String? {
        if (password.length < 10) {
            return "La contraseña debe tener al menos 10 caracteres"
        }
        val mayuscula = password.any { it.isUpperCase() }
        val minuscula = password.any { it.isLowerCase() }
        val numero = password.any { it.isDigit() }
        val simbolo = password.any { !it.isLetterOrDigit() }

        if (!mayuscula) return "Debe tener al menos una letra mayúscula"
        if (!minuscula) return "Debe tener al menos una letra minúscula"
        if (!numero) return "Debe tener al menos un número"
        if (!simbolo) return "Debe tener al menos un símbolo especial (ej. @, _, ?, #)"

        for (i in 0 until password.length - 2) {
            val c1 = password[i]
            val c2 = password[i + 1]
            val c3 = password[i + 2]

            if (c1.isDigit() && c2.isDigit() && c3.isDigit()) {
                val secuenciaAscendente = (c2 - c1 == 1 && c3 - c2 == 1)
                val secuenciaDescendente = (c1 - c2 == 1 && c2 - c3 == 1)
                val repetido = (c1 == c2 && c2 == c3)

                if (secuenciaAscendente || secuenciaDescendente || repetido) {
                    return "No se permiten números consecutivos (ej. 123) ni repetidos (ej. 111)."
                }
            }
        }
        return null
    }

    // ==========================================
    // OPERACIONES CRUD EN FIRESTORE CON COORDENADAS EXACTAS Y UID
    // ==========================================
    suspend fun calculateRoute(
        from: String,
        to: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        transport: Transport,
        pref: RoutePreference
    ): Int {
        isCalculatingRoute = true
        return try {
            addRoute(
                from = from,
                to = to,
                startLat = startLat,
                startLng = startLng,
                endLat = endLat,
                endLng = endLng,
                transport = transport,
                pref = pref
            )
        } finally {
            isCalculatingRoute = false
        }
    }

    fun addRoute(
        from: String,
        to: String,
        startLat: Double = currentUserLocation?.latitude ?: 19.432608,
        startLng: Double = currentUserLocation?.longitude ?: -99.133209,
        endLat: Double = (currentUserLocation?.latitude ?: 19.432608) + 0.015,
        endLng: Double = (currentUserLocation?.longitude ?: -99.133209) + 0.02,
        transport: Transport = defaultTransport,
        pref: RoutePreference = defaultPreference,
        onSuccess: ((Int) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ): Int {
        val user = auth.currentUser
        val uid = user?.uid ?: currentUserId
        if (uid.isNullOrBlank()) {
            val err = IllegalStateException("Debes haber iniciado sesión con Firebase para guardar rutas.")
            Log.e("FirestoreAddRoute", err.message ?: "")
            onFailure?.invoke(err)
            return 0
        }

        val id = (routes.maxOfOrNull { it.id } ?: 0) + 1
        val baseKm = DirectionsApiClient.calculateHaversineKm(startLat, startLng, endLat, endLng) * 1.28

        val traffic = when (pref) {
            RoutePreference.RAPIDA -> if (weather.rainChance >= 50) TrafficLevel.MODERADO else TrafficLevel.FLUIDO
            RoutePreference.SEGURA -> TrafficLevel.MODERADO
            RoutePreference.EVITAR_TRAFICO -> TrafficLevel.FLUIDO
        }

        val baseMinutes = (baseKm / transport.speedKmh * 60).roundToInt().coerceAtLeast(3)
        val prefFactor = when (pref) {
            RoutePreference.RAPIDA -> 1.00
            RoutePreference.SEGURA -> 1.18
            RoutePreference.EVITAR_TRAFICO -> 1.12
        }
        val prefMinutes = (baseMinutes * prefFactor).roundToInt()
        val rainExtra = if (weather.rainChance >= 50) 4 else 0
        val trafficExtra = when (traffic) {
            TrafficLevel.FLUIDO -> 0
            TrafficLevel.MODERADO -> 3
            TrafficLevel.LENTO -> 8
        }
        val finalMinutes = (prefMinutes + rainExtra + trafficExtra).coerceAtLeast(3)

        val routeMap = hashMapOf(
            "id" to id,
            "uid" to uid,
            "from" to from.ifBlank { "Mi ubicación" },
            "to" to to.ifBlank { "Destino" },
            "startLat" to startLat,
            "startLng" to startLng,
            "endLat" to endLat,
            "endLng" to endLng,
            "minutes" to finalMinutes,
            "km" to (Math.round(baseKm * 10.0) / 10.0),
            "transport" to transport.name,
            "favorite" to false,
            "traffic" to traffic.name,
            "preference" to pref.name,
            "departure" to "7:30 a.m.",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).collection("routes")
            .add(routeMap)
            .addOnSuccessListener {
                selectedRouteId = id
                learnedMessage = null
                onSuccess?.invoke(id)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreAddRoute", "Error al guardar ruta en Firestore", e)
                onFailure?.invoke(e)
            }

        selectedRouteId = id
        return id
    }

    fun select(id: Int) {
        selectedRouteId = id
        learnedMessage = null
    }

    fun toggleFavorite(id: Int) {
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        val item = routes.firstOrNull { it.id == id } ?: return
        val newFav = !item.favorite

        if (item.firestoreDocId.isNotBlank()) {
            db.collection("users").document(uid).collection("routes").document(item.firestoreDocId)
                .update("favorite", newFav)
        } else {
            db.collection("users").document(uid).collection("routes")
                .whereEqualTo("id", id)
                .get()
                .addOnSuccessListener { docs ->
                    for (d in docs) {
                        d.reference.update("favorite", newFav)
                    }
                }
        }
    }

    fun deleteRoute(id: Int): Boolean {
        val uid = auth.currentUser?.uid ?: currentUserId ?: return false
        val item = routes.firstOrNull { it.id == id } ?: return false

        if (item.firestoreDocId.isNotBlank()) {
            db.collection("users").document(uid).collection("routes").document(item.firestoreDocId).delete()
        } else {
            db.collection("users").document(uid).collection("routes")
                .whereEqualTo("id", id)
                .get()
                .addOnSuccessListener { docs ->
                    for (d in docs) {
                        d.reference.delete()
                    }
                }
        }
        return true
    }

    fun recordTrip(route: RouteItem, minutes: Int? = null) {
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        val nextId = (history.maxOfOrNull { it.id } ?: 0) + 1
        val finalMinutes = minutes ?: route.minutes
        val now = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())

        val historyMap = hashMapOf(
            "id" to nextId,
            "uid" to uid,
            "day" to "Hoy",
            "from" to route.from,
            "to" to route.to,
            "startLat" to route.startLat,
            "startLng" to route.startLng,
            "endLat" to route.endLat,
            "endLng" to route.endLng,
            "minutes" to finalMinutes,
            "km" to route.km,
            "time" to now,
            "transport" to route.transport.name,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).collection("history")
            .add(historyMap)
            .addOnSuccessListener {
                Log.d("FirestoreHistory", "Viaje registrado exitosamente en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHistory", "Error al guardar viaje en Firestore: ${e.message}", e)
            }
    }

    fun updateProfile(newName: String, newEmail: String) {
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        if (newName.isNotBlank()) userName = newName.trim()
        if (newEmail.isNotBlank()) email = newEmail.trim()

        db.collection("users").document(uid)
            .update(
                mapOf(
                    "Nombre(s)" to userName,
                    "email" to email
                )
            )
    }

    fun updatePreferences(pref: RoutePreference, transport: Transport) {
        defaultPreference = pref
        defaultTransport = transport
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "defaultPreference" to pref.name,
                    "defaultTransport" to transport.name
                )
            )
    }

    fun updateAlertSettings(traffic: Boolean, weather: Boolean, recs: Boolean) {
        trafficAlertsEnabled = traffic
        weatherAlertsEnabled = weather
        recommendationAlertsEnabled = recs
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "trafficAlertsEnabled" to traffic,
                    "weatherAlertsEnabled" to weather,
                    "recommendationAlertsEnabled" to recs
                )
            )
    }

    fun setIot(name: String, enabled: Boolean) {
        iotSources[name] = enabled
        val uid = auth.currentUser?.uid ?: currentUserId ?: return
        db.collection("users").document(uid)
            .update("iotSources.$name", enabled)
    }

    fun recommendationFor(route: RouteItem): Recommendation =
        RecommendationEngine.recommend(
            route = route,
            weather = weather,
            history = history,
            iotSources = iotSources,
            acceptedAdaptations = acceptedAdaptations
        )

    fun acceptAdaptation() {
        acceptedAdaptations += 1
        learnedMessage = "¡Listo! SmartRoute AI aprendió tu preferencia."
    }

    fun keepHabitual() {
        learnedMessage = "Mantendremos tu ruta habitual como preferida."
    }

    fun estimateMinutes(route: RouteItem, transport: Transport): Int =
        if (transport == route.transport) route.minutes
        else (route.minutes * route.transport.speedKmh / transport.speedKmh).roundToInt().coerceAtLeast(3)

    // ==========================================
    // CLIMA REAL VÍA OPEN-METEO API (100% GRATUITA Y SIN API KEY)
    // ==========================================
    fun fetchWeatherForCoordinates(context: Context, lat: Double, lng: Double, locationName: String? = null) {
        viewModelScope.launch {
            isLoadingWeather = true
            weatherError = null
            try {
                val realWeather = WeatherApiClient.fetchWeather(context, lat, lng, locationName)
                weather = realWeather
            } catch (e: Exception) {
                Log.e("WeatherVM", "Fallo al consultar clima con Open-Meteo: ${e.message}", e)
                weatherError = "No se pudo actualizar el clima en tiempo real (${e.localizedMessage})"
            } finally {
                isLoadingWeather = false
            }
        }
    }

    // ==========================================
    // TRÁFICO Y POLILÍNEA VÍA OSRM (100% GRATUITA Y SIN API KEY)
    // ==========================================
    fun fetchTrafficForRoute(context: Context, route: RouteItem) {
        viewModelScope.launch {
            isLoadingTraffic = true
            try {
                val report = DirectionsApiClient.fetchTrafficRoute(
                    startLat = route.startLat,
                    startLng = route.startLng,
                    endLat = route.endLat,
                    endLng = route.endLng,
                    routeTitle = route.title
                )
                currentTrafficReport = report

                // Actualizar zonas reactivas a partir del reporte real de OSRM
                zones.clear()
                zones.add(TrafficZone(route.from, TrafficLevel.FLUIDO, 1))
                zones.add(TrafficZone("Tramo hacia ${route.to}", report.level, report.delayMin))
            } catch (e: Exception) {
                Log.e("TrafficVM", "Error al consultar OSRM: ${e.message}", e)
            } finally {
                isLoadingTraffic = false
            }
        }
    }
}
