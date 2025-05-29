package com.example.publictransport.repository

import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.example.publictransport.network.RetrofitInstance
import com.example.publictransport.ui.TripResponse
import retrofit2.http.Query

class TransportRepository {
    suspend fun getRoutes(): List<Route> {
        return RetrofitInstance.api.getRoutes()
    }

    suspend fun getStops(): List<Stop> {
        return RetrofitInstance.api.getStops()
    }
    suspend fun getTrips( sLat: Double,
                          sLong: Double,
                          eLat: Double,
                          eLong: Double): List<TripResponse> {
        return RetrofitInstance.api.getTrips(sLat, sLong, eLat, eLong)
    }




}
