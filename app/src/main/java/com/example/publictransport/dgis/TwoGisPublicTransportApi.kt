package com.example.publictransport.dgis

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

// TwoGisPublicTransportApi.kt
interface TwoGisPublicTransportApi {
    @POST("public_transport/2.0")
    @Headers("Content-Type: application/json")
    suspend fun getTrips(
        @Query("key") apiKey: String,
        @Body req: TwoGisTripRequest
    ): List<TwoGisTripResponse>
}


object TwoGisRetrofit {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://routing.api.2gis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: TwoGisPublicTransportApi = retrofit.create(TwoGisPublicTransportApi::class.java)
}