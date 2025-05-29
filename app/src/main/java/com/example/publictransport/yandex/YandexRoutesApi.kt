package com.example.publictransport.yandex

import retrofit2.http.GET
import retrofit2.http.Query

// API интерфейс
interface YandexRoutesApi {
    // Правильно
    @GET("v2/route")
    suspend fun getRoute(
        @Query("apikey") apiKey: String,
        @Query("format") format: String = "geojson",
        @Query("mode")   mode: String = "masstransit",
        @Query("waypoints") waypoints: String
    ): YandexRouteResponse

}