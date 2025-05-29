    package com.example.publictransport.network
    
    import com.example.publictransport.model.Route
    import com.example.publictransport.model.Stop
    import com.example.publictransport.ui.TripResponse
    import retrofit2.http.GET
    import retrofit2.http.Headers
    import retrofit2.http.Query

    interface TransportApi {
        @GET("route/list")
        @Headers(
            "X-Auth-Token: @string/x_auth\n",
            "X-Visitor-Id: 1333796405bf72754589e09938b54f64",
            "Referer: https://citybus.tha.kz/",
            "Accept: application/json"
        )
        suspend fun getRoutes(): List<Route>
    
        @GET("stop/list")
        @Headers(
            "X-Auth-Token: @string/x_auth\n",
            "X-Visitor-Id: 1333796405bf72754589e09938b54f64",
            "Referer: https://citybus.tha.kz/",
            "Accept: application/json"
        )
        suspend fun getStops(): List<Stop>

        @GET("trip/list")
        suspend fun getTrips(
            @Query("sLat") sLat: Double,
            @Query("sLong") sLong: Double,
            @Query("eLat") eLat: Double,
            @Query("eLong") eLong: Double
        ): List<TripResponse>

    }
