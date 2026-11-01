package com.example.smartroute

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialException
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.lifecycle.ViewModel
import com.example.smartroute.ai.RecommendationEngine
import com.example.smartroute.data.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
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
    //METODOS DEL LOGIN CON AUTENTICACION
    val auth = FirebaseAuth.getInstance()
    fun registerWithAuth(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        emailInput: String,
        passwordInput: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val hashPassword = BCrypt.hashpw(passwordInput, BCrypt.gensalt())
        val nuevoUsuario = hashMapOf(
            "Nombre(s)" to nombre,
            "Apellido paterno" to apellidoPaterno,
            "Apellido materno" to apellidoMaterno,
            "email" to emailInput,
            "password" to hashPassword
        )

        auth.createUserWithEmailAndPassword(emailInput, passwordInput)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                db.collection("users")
                    .add(nuevoUsuario)
                    .addOnSuccessListener { documentReference ->
                        Log.d("RegisterDebug", "Usuario registrado en Firestore con ID: ${documentReference.id}")
                        userName = nombre
                        email = emailInput

                        user?.sendEmailVerification()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onResult(true, null)
                            } else {
                                onResult(false, "Se creó el usuario pero falló el envío del correo")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("RegisterDebug", "Error al registrar usuario en Firestore", e)
                        onResult(false, "Error al guardar datos de usuario en la base de datos")
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    fun loginWithAuth(emailInput: String, passwordInput: String, onResult: (Boolean, String?) -> Unit) {
        if (emailInput.isBlank() || passwordInput.isBlank()) {
            onResult(false, "Por favor completa todos los campos")
            return
        }

        auth.signInWithEmailAndPassword(emailInput, passwordInput)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null && user.isEmailVerified) {
                    Log.d("LoginDebug", "Buscando en Firestore - Email: '${emailInput.trim()}'")
                    db.collection("users")
                        .whereEqualTo("email", emailInput.trim())
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val document = documents.documents[0]
                                userName = document.getString("Nombre(s)") ?: "Usuario"
                                email = document.getString("email") ?: emailInput
                                Log.d("LoginDebug", "¡Inicio de sesión exitoso con Auth y datos cargados de Firestore!")
                            } else {
                                email = emailInput
                            }
                            onResult(true, null)
                        }
                        .addOnFailureListener { exception ->
                            Log.w("LoginDebug", "Error al conectar con Firestore", exception)
                            email = emailInput
                            onResult(true, null)
                        }
                } else {
                    auth.signOut()
                    onResult(false, "Por favor verifica tu correo")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }
    //METODO PARA VALIDACIONES DE CONTRASEÑA
    fun validatePasswordSecurity(password: String): String? {
        if(password.length<10){
            return "La contraseña debe tner al menos 10 caracteres"
        }
        val mayuscula =password.any{it.isUpperCase()}
        val minuscula = password.any { it.isLowerCase() }
        val numero = password.any { it.isDigit() }
        val simbolo = password.any { it.isLetterOrDigit() }

        if(!mayuscula) return "Debe tener al menos una letra mayuscula"
        if(!minuscula) return "Debe tener al menos una letra minuscula"
        if(!numero) return "Debe tener al menos un numero"
        if (!simbolo) return "Debe tener al menos un simolo especial (ej. @, _, ?, #"

        for( i in 0 until password.length-2){
            val c1 = password[i]
            val c2 = password[i+1]
            val c3 = password[i+2]

            if(c1.isDigit() && c2.isDigit() && c3.isDigit()){
                val secuenciaAscendente = (c2-c1 == 1 && c3-c2 ==1)
                val secuenciaDescendente = (c1-c2 == 1 && c2-c3 ==1)
                val repetido = (c1==c2 && c2==c3)

                if(secuenciaAscendente || secuenciaDescendente || repetido){
                    return "No se permiten números consecutivos (ej. 123) ni repetidos (ej. 111)."
                }
            }
        }
        return null
    }
    //METODO DE REGISTRO CON GOOGLE
    fun loginWithGoogle(idToken: String, onResult: (Boolean, String?)-> Unit){
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if(user == null){
                    onResult(false, "No se pudo obtener el usuairo")
                    return@addOnSuccessListener
                }

                userName = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                email = user.email ?: ""

                val ref = db.collection("users").document(user.uid)
                ref.get().addOnSuccessListener {
                    doc ->
                    if(!doc.exists()){
                        ref.set(
                            hashMapOf(
                                "Nombre(s)" to (user.displayName?: ""),
                                "email" to (user.email ?: ""),
                                "uid" to user.uid
                            )
                        ).addOnFailureListener {
                            Log.w("GoogleLogin", "No se pudo guardar en Firestore",it)
                        }
                    }
                }
                onResult(true, null)
            }
            .addOnFailureListener { e->
                onResult(false, e.localizedMessage)
            }
    }
    suspend fun signInWithGoogleCredential(activity: Activity, webClientId: String): String?{
        val credentialManager = CredentialManager.create(activity)

        // Configuración para cuentas autorizadas
        val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        return try{
            //solicitud de credenciales
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )
            //token id de google
            handleCredentialResult(result)
        }catch (e: GetCredentialException){
            // Imprime el tipo exacto de error en el Logcat para depurar
            android.util.Log.e("AuthError", "Error tipo: ${e.type}, Mensaje: ${e.message}")
            null
        }
    }
    fun handleCredentialResult(result: GetCredentialResponse): String? {
        val credential = result.credential
        return when(credential){
            is CustomCredential ->{
                if(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken

                        googleIdToken
                    }catch (e: GoogleIdTokenParsingException){
                        null
                    }
                }else{
                    null
                }
            }
            else -> null
        }
    }
    //FIN DE LOS METODOS DEL LOGIN
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
