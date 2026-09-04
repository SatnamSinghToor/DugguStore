package com.duggustore.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class RoutePoint(val latitude: Double, val longitude: Double)

data class RouteResult(
    val points: List<RoutePoint>,
    val distanceMetres: Double,
    val durationSeconds: Double
)

/**
 * Turn-by-turn road geometry from OSRM's public demo server — free, no API
 * key or billing account, real road-network routing rather than a straight
 * line between two points. It's a shared, rate-limited demo instance
 * (documented as best-effort by the OSRM project itself), so this is
 * treated as best effort throughout: a failure here just means the map
 * falls back to drawing a straight line between pickup and drop instead of
 * a real route.
 */
class RoutingRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getDrivingRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        try {
            // OSRM takes coordinates as lon,lat (not lat,lon).
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                "$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    return@withContext Result.failure(Exception("Route lookup failed (${response.code})"))
                }

                val root = json.parseToJsonElement(body).jsonObject
                val route = root["routes"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?: return@withContext Result.failure(Exception("No route found"))
                val coordinates = route["geometry"]?.jsonObject?.get("coordinates")?.jsonArray
                    ?: return@withContext Result.failure(Exception("No route geometry"))

                val points = coordinates.map { point ->
                    val pair = point.jsonArray
                    RoutePoint(latitude = pair[1].jsonPrimitive.double, longitude = pair[0].jsonPrimitive.double)
                }
                val distance = route["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val duration = route["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                Result.success(RouteResult(points, distance, duration))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
