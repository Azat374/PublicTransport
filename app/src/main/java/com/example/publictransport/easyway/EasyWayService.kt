// EasyWayService.kt
package com.example.publictransport.easyway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object EasyWayService {
    private val client = OkHttpClient()

    // Настройка Json: ignoreUnknownKeys + coerceInputValues
    // coerceInputValues = true позволит автоматически подставить null в поля типа String? или Long?
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues  = true
    }

    /**
     * 1) Вызов /ajax/ru/almaty/compile
     */
    suspend fun compile(
        startLat: Double,
        startLng: Double,
        stopLat: Double,
        stopLng: Double,
        direct: Boolean = false,
        wayType: String = "optimal",
        transports: String = "metro,trol,bus",
        enableWalkWays: Int = 0
    ): CompileResponse = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("start_lat", startLat.toString())
            .add("start_lng", startLng.toString())
            .add("stop_lat", stopLat.toString())
            .add("stop_lng", stopLng.toString())
            .add("direct", direct.toString())
            .add("way_type", wayType)
            .add("transports", transports)
            .add("enable_walk_ways", enableWalkWays.toString())
            .build()

        val request = Request.Builder()
            .url("https://kz.easyway.info/ajax/ru/almaty/compile")
            .post(formBody)
            .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("x-requested-with", "XMLHttpRequest")
            .addHeader("Cookie", "full_version=1")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code ${response.code}")
            }
            val bodyString = response.body?.string().orEmpty()
            json.decodeFromString(bodyString)
        }
    }

    /**
     * 2) Вызов /ajax/ru/almaty/getCompileRoute
     */
    suspend fun getCompileRoute(
        ids: String,       // пример: "60,83"
        starts: String,    // пример: "1201,804"
        stops: String,     // пример: "1259,845"
        a: String,         // пример: "43.226869,76.923396"
        b: String          // пример: "43.220059,76.896944"
    ): CompileRouteResponse = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("ids", ids)
            .add("starts", starts)
            .add("stops", stops)
            .add("a", a)
            .add("b", b)
            .build()

        val request = Request.Builder()
            .url("https://kz.easyway.info/ajax/ru/almaty/getCompileRoute")
            .post(formBody)
            .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("x-requested-with", "XMLHttpRequest")
            .addHeader("Cookie", "full_version=1")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code ${response.code}")
            }
            val bodyString = response.body?.string().orEmpty()
            json.decodeFromString(bodyString)
        }
    }
}
