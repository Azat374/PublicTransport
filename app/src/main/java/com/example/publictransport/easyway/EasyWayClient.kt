// EasyWayClient.kt
package com.example.publictransport.easyway

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

object EasyWayClient {
    // Настраиваем OkHttpClient с логгированием
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Унифицированный JSON-парсер (привязка kotlinx-serialization)
    private val jsonParser = Json {
        ignoreUnknownKeys = true   // чтобы не падало на незнакомых полях
    }

    /**
     * 1) Компиляция маршрутов (/ajax/ru/almaty/compile).
     * Возвращает сериализованный CompileResponse или кидает IOException.
     */
    @Throws(IOException::class)
    suspend fun compile(
        startLat: Double,
        startLng: Double,
        stopLat: Double,
        stopLng: Double,
        direct: Boolean = false,
        wayType: String = "optimal",
        transports: String,
        enableWalkWays: Int = 0
    ): CompileResponse {
        // Формируем тело x-www-form-urlencoded
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
            // Обязательные заголовки:
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("x-requested-with", "XMLHttpRequest")
            .addHeader("Cookie", "full_version=1")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("EasyWay compile failed: ${response.code}")
            }
            val bodyString = response.body?.string()
                ?: throw IOException("Empty response from EasyWay compile")

            // Парсим JSON вручную
            return jsonParser.decodeFromString(bodyString)
        }
    }

    /**
     * 2) Получить полные точки конкретного маршрута (/ajax/ru/almaty/getCompileRoute).
     */
    @Throws(IOException::class)
    suspend fun getCompileRoute(
        ids: String,
        starts: String,
        stops: String,
        a: String,  // "lat,lng"
        b: String   // "lat,lng"
    ): CompileRouteResponse {
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
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("x-requested-with", "XMLHttpRequest")
            .addHeader("Cookie", "full_version=1")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("EasyWay getCompileRoute failed: ${response.code}")
            }
            val bodyString = response.body?.string()
                ?: throw IOException("Empty response from EasyWay getCompileRoute")

            return jsonParser.decodeFromString(bodyString)
        }
    }
}
