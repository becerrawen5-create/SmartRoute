package com.example.smartroute.network

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String,
    val latitude: Double,
    val longitude: Double
)

interface PhotonApi {
    @GET("api")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("lang") lang: String = "es",
        @Header("User-Agent") userAgent: String = "SmartRoute-Android-OSM"
    ): PhotonResponseDto
}

data class PhotonResponseDto(
    val features: List<PhotonFeatureDto>?
)

data class PhotonFeatureDto(
    val geometry: PhotonGeometryDto?,
    val properties: PhotonPropertiesDto?
)

data class PhotonGeometryDto(
    val coordinates: List<Double>? // [lon, lat]
)

data class PhotonPropertiesDto(
    val name: String?,
    val street: String?,
    val housenumber: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postcode: String?
)

class PlacesRepository(private val context: Context) {
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    private val photonApi: PhotonApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonApi::class.java)
    }

    suspend fun getPredictions(query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        try {
            val response = photonApi.search(query = cleanQuery, limit = 5, lang = "es")
            val features = response.features ?: emptyList()

            if (features.isNotEmpty()) {
                return@withContext features.mapIndexedNotNull { idx, feature ->
                    val coords = feature.geometry?.coordinates
                    if (coords != null && coords.size >= 2) {
                        val lon = coords[0]
                        val lat = coords[1]
                        val props = feature.properties

                        val primary = props?.name ?: props?.street ?: cleanQuery
                        val secondaryParts = listOfNotNull(
                            props?.street.takeIf { it != primary },
                            props?.city,
                            props?.state,
                            props?.country
                        )
                        val secondary = secondaryParts.joinToString(", ")
                        val full = if (secondary.isNotBlank()) "$primary, $secondary" else primary

                        PlaceSuggestion(
                            placeId = "photon_$idx",
                            primaryText = primary,
                            secondaryText = secondary,
                            fullText = full,
                            latitude = lat,
                            longitude = lon
                        )
                    } else null
                }
            } else {
                return@withContext fallbackGeocode(cleanQuery)
            }
        } catch (e: Exception) {
            Log.w("PlacesRepository", "Photon API error: ${e.message}, usando fallback local")
            return@withContext fallbackGeocode(cleanQuery)
        }
    }

    @Suppress("DEPRECATION")
    private fun fallbackGeocode(query: String): List<PlaceSuggestion> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 5)
            results?.mapIndexed { idx, addr ->
                val primary = addr.featureName ?: addr.thoroughfare ?: query
                val secondary = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                PlaceSuggestion(
                    placeId = "geo_$idx",
                    primaryText = primary,
                    secondaryText = secondary,
                    fullText = addr.getAddressLine(0) ?: "$primary, $secondary",
                    latitude = addr.latitude,
                    longitude = addr.longitude
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getReverseGeocode(geoPoint: GeoPoint): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
            val addr = results?.firstOrNull()
            addr?.getAddressLine(0) ?: "${"%.4f".format(geoPoint.latitude)}, ${"%.4f".format(geoPoint.longitude)}"
        } catch (_: Exception) {
            "${"%.4f".format(geoPoint.latitude)}, ${"%.4f".format(geoPoint.longitude)}"
        }
    }
}
