package com.example.smartroute.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


// Nominatim devuelve una lista directa de resultados, no un objeto "features"
data class NominatimResult(
    val place_id: Long,
    val display_name: String,
    val lat: String,
    val lon: String
)

interface GeocodingService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        // Opcional: puedes pedir los nombres en español si están disponibles
        @Query("accept-language") lang: String = "es"
    ): List<NominatimResult>
}

object GeocodingClient {
    // 1. Creamos un cliente OkHttp con un interceptor
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                // 2. Añadimos el User-Agent requerido
                .header("User-Agent", "SmartRoute/1.0 (tu-correo@ejemplo.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            // 3. Asignamos el cliente a Retrofit
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }
}