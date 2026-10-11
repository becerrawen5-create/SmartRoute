package com.example.smartroute

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.smartroute.ai.RecommendationEngine
import com.example.smartroute.data.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import org.mindrot.jbcrypt.BCrypt
import kotlin.math.roundToInt

class SmartRouteViewModel : ViewModel() {
    private final val db = Firebase.firestore
    var userName by mutableStateOf("Ana López")
    var email by mutableStateOf("ana.lopez@gmail.com")

    val routes = mutableStateListOf<RouteItem>().apply { addAll(SampleData.routes) }
    val history: List<HistoryEntry> = SampleData.history
    val weather: Weather = SampleData.weather
    val zones: List<TrafficZone> = SampleData.trafficZones
    val iotSources = mutableStateMapOf<String, Boolean>().apply {
        SampleData.iotSources.forEach { put(it, true) }
    }

    var selectedRouteId by mutableIntStateOf(1)
    var acceptedAdaptations by mutableIntStateOf(0)
    var learnedMessage by mutableStateOf<String?>(null)

    val selectedRoute: RouteItem
        get() = routes.firstOrNull { it.id == selectedRouteId } ?: routes.first()

    val featuredRoute: RouteItem
        get() = routes.firstOrNull { it.favorite } ?: routes.first()

    fun registerUser(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        emailInput: String,
        passInput: String,
        onResult: (Boolean) -> Unit
    ) {
       val hashPassword= BCrypt.hashpw(passInput, BCrypt.gensalt())
        val nuevoUsuario = hashMapOf(
            "Nombre(s)" to nombre,
            "Apellido paterno" to apellidoPaterno,
            "Apellido materno" to apellidoMaterno,
            "email" to emailInput,
            "password" to hashPassword
        )

        db.collection("users")
            .add(nuevoUsuario)
            .addOnSuccessListener { documentReference ->
                Log.d("RegisterDebug", "Usuario registrado con ID: ${documentReference.id}")
                userName = nombre
                email = emailInput
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.w("RegisterDebug", "Error al registrar usuario", e)
                onResult(false)
            }
    }
    /** Metodo para el logearse ya conectado a la base de datos */
    fun login(emailInput: String, passwordInput: String, onResult: (Boolean) -> Unit) {
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            onResult(false)
            return
        }
        Log.d("LoginDebug", "Buscando - Email: '${emailInput.trim()}'")
        db.collection("users")
            .whereEqualTo("email", emailInput.trim())
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val storeHash = document.getString("password") ?: ""
                    if(BCrypt.checkpw(passwordInput, storeHash)){
                        userName = document.getString("Nombre(s)")?:"Usuario"
                        email = document.getString("email")?: emailInput
                        Log.d("LoginDebug", "¡Inicio de sesión exitoso con contraseña encriptada!")
                        onResult(true)
                    }else{
                        // La contraseña no coincide con el hash
                        Log.d("LoginDebug", "Contraseña incorrecta.")
                        onResult(false)
                    }
                } else {
                    Log.d("LoginDebug", "Correo o contraseña incorrectos en la BD.")
                    onResult(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("LoginDebug", "Error al conectar con Firestore", exception)
                onResult(false)
            }
    }

    fun select(id: Int) {
        selectedRouteId = id
        learnedMessage = null
    }

    fun toggleFavorite(id: Int) {
        val i = routes.indexOfFirst { it.id == id }
        if (i >= 0) routes[i] = routes[i].copy(favorite = !routes[i].favorite)
    }

    fun recommendationFor(route: RouteItem): Recommendation =
        RecommendationEngine.recommend(route, weather, acceptedAdaptations)

    /** El usuario elige la ruta adaptada: el sistema "aprende" la preferencia. */
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

    fun addRoute(from: String, to: String, transport: Transport, pref: RoutePreference): Int {
        val id = (routes.maxOfOrNull { it.id } ?: 0) + 1
        val km = 3.0 + ((from.length + to.length) % 12)
        val minutes = (km / transport.speedKmh * 60).roundToInt().coerceAtLeast(5)
        routes.add(
            RouteItem(
                id = id,
                from = from.ifBlank { "Mi ubicación" },
                to = to.ifBlank { "Destino" },
                minutes = minutes,
                km = km,
                transport = transport,
                favorite = false,
                traffic = TrafficLevel.FLUIDO,
                preference = pref
            )
        )
        selectedRouteId = id
        learnedMessage = null
        return id
    }

    fun setIot(name: String, enabled: Boolean) {
        iotSources[name] = enabled
    }
}
