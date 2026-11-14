package com.example.smartroute.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

class LocationHelper(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Verifica si el hardware de GPS o los servicios de ubicación están activos en el dispositivo.
     */
    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GeoPoint? {
        if (!hasLocationPermission()) {
            Log.w("LocationHelper", "Permisos de ubicación no concedidos.")
            return null
        }

        if (!isLocationServiceEnabled()) {
            Log.w("LocationHelper", "El sensor de GPS/Ubicación está apagado en los ajustes del dispositivo.")
            // Intentamos obtener la última ubicación conocida en caché aunque el sensor esté apagado
            return try {
                fusedClient.lastLocation.await()?.let { GeoPoint(it.latitude, it.longitude) }
            } catch (e: Exception) {
                null
            }
        }

        return try {
            val location: Location? = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await() ?: fusedClient.lastLocation.await()

            location?.let {
                Log.d("LocationHelper", "Ubicación GPS obtenida: ${it.latitude}, ${it.longitude}")
                GeoPoint(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error al obtener ubicación con FusedLocationProviderClient", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(intervalMs: Long = 5000L): Flow<GeoPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.w("LocationHelper", "Sin permisos de ubicación para actualizaciones en tiempo real.")
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(2000L)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error al registrar requestLocationUpdates", e)
            close(e)
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
