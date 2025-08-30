package com.example.menza.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class GeocodingResult(
    val name: String,
    val address: String,
    val city: String,
    val postcode: String?,
    val fullAddress: String,
    val lat: Double,
    val lon: Double
)

class GeocodingRepository {
    private val client = OkHttpClient()

    suspend fun searchAddress(query: String): List<GeocodingResult> =
        withContext(Dispatchers.IO) {
            val url =
                "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=${query}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "menza-app")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONArray(body)
                (0 until json.length()).map { i ->
                    val obj = json.getJSONObject(i)

                    val fullAddress = obj.getString("display_name")
                    val name = fullAddress.split(",").firstOrNull()?.trim() ?: "Unknown"

                    val addressObj = obj.optJSONObject("address")
                    val road = addressObj?.optString("road") ?: ""
                    val houseNumber = addressObj?.optString("house_number") ?: ""
                    val city = addressObj?.optString("city")
                        ?: addressObj?.optString("town")
                        ?: addressObj?.optString("village")
                        ?: ""
                    val addr = buildString {
                        append(road)
                        if (houseNumber.isNotBlank()) append(" $houseNumber")
                    }.ifBlank { fullAddress }

                    val postcode = addressObj?.optString("postcode")

                    GeocodingResult(
                        name = name,
                        address = addr,
                        city = city,
                        postcode = postcode,
                        fullAddress = fullAddress,
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon")
                    )
                }
            }
        }
}
