package com.example.smartroute.network

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.smartroute.data.DayForecast
import com.example.smartroute.data.HourForecast
import com.example.smartroute.data.Weather
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoDto
}

data class OpenMeteoDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val current: CurrentDto?,
    val hourly: HourlyDto?,
    val daily: DailyDto?
)

data class CurrentDto(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val relativeHumidity: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    val precipitation: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double
)

data class HourlyDto(
    val time: List<String>?,
    @SerializedName("temperature_2m") val temperature2m: List<Double>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?
)

data class DailyDto(
    val time: List<String>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>?,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>?
)

object WeatherApiClient {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val api: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    fun wmoCodeToCondition(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55 -> "Llovizna"
        61 -> "Lluvia ligera"
        63 -> "Lluvia moderada"
        65 -> "Lluvia fuerte"
        71, 73, 75 -> "Nevada"
        77 -> "Granizo fino"
        80, 81, 82 -> "Chubascos"
        85, 86 -> "Chubascos de nieve"
        95 -> "Tormenta eléctrica"
        96, 99 -> "Tormenta con granizo"
        else -> "Nublado variable"
    }

    suspend fun fetchWeather(context: Context, lat: Double, lng: Double, locationName: String? = null): Weather =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getForecast(latitude = lat, longitude = lng)
                val current = response.current

                val resolvedCity = locationName?.ifBlank { null } ?: resolveCityName(context, lat, lng)

                val temp = current?.temperature?.roundToInt() ?: 22
                val humidity = current?.relativeHumidity ?: 50
                val wind = current?.windSpeed?.roundToInt() ?: 10
                val code = current?.weatherCode ?: 0
                val condition = wmoCodeToCondition(code)
                val rainChance = if ((current?.precipitation ?: 0.0) > 0.0) {
                    ((current?.precipitation ?: 0.0) * 20.0).roundToInt().coerceIn(10, 95)
                } else if (code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95)) {
                    70
                } else if (code in listOf(1, 2, 3)) {
                    20
                } else {
                    5
                }

                // Generar pronóstico por horas a partir de datos reales
                val hourlyList = mutableListOf<HourForecast>()
                val hTimes = response.hourly?.time ?: emptyList()
                val hTemps = response.hourly?.temperature2m ?: emptyList()
                val hCodes = response.hourly?.weatherCode ?: emptyList()

                val nowHour = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.getDefault()).format(Date())
                var startIndex = hTimes.indexOfFirst { it.startsWith(nowHour) }
                if (startIndex < 0) startIndex = 0

                for (i in startIndex until (startIndex + 6).coerceAtMost(hTimes.size)) {
                    val rawTime = hTimes[i]
                    val hourLabel = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(rawTime)
                        if (i == startIndex) "Ahora" else SimpleDateFormat("h a.m.", Locale.getDefault()).format(parsed ?: Date())
                    } catch (_: Exception) {
                        "Hora"
                    }
                    val t = hTemps.getOrNull(i)?.roundToInt() ?: temp
                    val c = wmoCodeToCondition(hCodes.getOrNull(i) ?: 0)
                    hourlyList.add(HourForecast(label = hourLabel, temp = t, condition = c))
                }

                // Generar pronóstico de días
                val dailyList = mutableListOf<DayForecast>()
                val dTimes = response.daily?.time ?: emptyList()
                val dMax = response.daily?.temperatureMax ?: emptyList()
                val dMin = response.daily?.temperatureMin ?: emptyList()
                val dCodes = response.daily?.weatherCode ?: emptyList()

                val dayFormat = SimpleDateFormat("EEE", Locale("es", "MX"))
                val parseDayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                for (i in 0 until dTimes.size.coerceAtMost(5)) {
                    val rawDay = dTimes[i]
                    val dayName = try {
                        val d = parseDayFormat.parse(rawDay)
                        if (i == 0) "Hoy" else dayFormat.format(d ?: Date()).replaceFirstChar { it.uppercase() }
                    } catch (_: Exception) {
                        "Día"
                    }
                    val maxT = dMax.getOrNull(i)?.roundToInt() ?: (temp + 3)
                    val minT = dMin.getOrNull(i)?.roundToInt() ?: (temp - 5)
                    val cond = wmoCodeToCondition(dCodes.getOrNull(i) ?: 0)
                    dailyList.add(DayForecast(day = dayName, max = maxT, min = minT, condition = cond))
                }

                Weather(
                    city = resolvedCity,
                    tempC = temp,
                    condition = condition,
                    humidity = humidity,
                    windKmh = wind,
                    uv = if (temp > 25) "Alto" else "Moderado",
                    rainChance = rainChance,
                    airQuality = if (temp > 28) "Regular" else "Buena",
                    hourly = hourlyList,
                    daily = dailyList,
                    latitude = lat,
                    longitude = lng
                )
            } catch (e: Exception) {
                Log.e("WeatherApiClient", "Error al consultar OpenMeteo: ${e.message}", e)
                throw e
            }
        }

    @Suppress("DEPRECATION")
    private fun resolveCityName(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Ubicación actual"
            } else {
                "Ubicación (${"%.2f".format(lat)}, ${"%.2f".format(lng)})"
            }
        } catch (_: Exception) {
            "Ubicación actual"
        }
    }
}
