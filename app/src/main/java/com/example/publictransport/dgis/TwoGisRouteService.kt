package com.example.publictransport.dgis

import com.example.publictransport.ui.PathResponse
import com.example.publictransport.ui.TripResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TwoGisRouteService.kt
object TwoGisRouteService {
    private const val API_KEY = "6ce85e72-0bb5-4e3c-a8f8-a1a386f48a11"

    // TwoGisRouteService.kt
    suspend fun fetchRoutes(
        fromLat: Double, fromLon: Double,
        toLat:   Double, toLon:   Double,
        modes:   List<String> = listOf("bus","tram","trolleybus","metro")
    ): List<TripResponse> = withContext(Dispatchers.IO) {
        val req = TwoGisTripRequest(
            source    = PointInfo("A", Point(fromLat, fromLon)),
            target    = PointInfo("B", Point(toLat,   toLon)),
            transport = modes
        )
        val resp = TwoGisRetrofit.api.getTrips(API_KEY, req)
        resp.map { variant ->
            // найдём первую и последнюю «пешие» отрезки
            val startWalk = variant.movements.firstOrNull { it.type=="walkway" && it.waypoint.subtype=="start" }
            val endWalk   = variant.movements.lastOrNull  { it.type=="walkway" && it.waypoint.subtype=="finish" }
            // все «проходы» (passage) — это ваши остановки
            val stopPoints = variant.movements
                .filter { it.type=="passage" }
                .mapNotNull { mv ->
                    // предполагаем, что MovementWaypoint содержит поле `location` с координатами
                    mv.waypoint.location?.let { loc ->
                        com.example.publictransport.dgis.Point(loc.lat, loc.lon)
                    }
                }

            TripResponse(
                startWalkDistance = startWalk?.distance ?: 0,
                startWalkDuration = formatSec(startWalk?.moving_duration ?: 0),
                endWalkDistance   = endWalk?.distance   ?: 0,
                endWalkDuration   = formatSec(endWalk?.moving_duration   ?: 0),
                paths = listOf(
                    PathResponse(
                        routeId       = variant.waypoints.firstOrNull()?.routes_names
                            ?.firstOrNull()?.toLongOrNull() ?: 0L,
                        directionIndex= 0,
                        walkDistance  = startWalk?.distance ?: 0,
                        walkDuration  = formatSec(startWalk?.moving_duration ?: 0),
                        rideDistance  = variant.total_distance,
                        rideDuration  = formatSec(variant.total_duration),
                        stops         = stopPoints,
                        name          = variant.waypoints.first().routes_names.first()
                    )
                )
            )
        }
    }

    private fun parseWalkDistance(m: Int) = m
    private fun formatSec(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%02d:%02d:%02d".format(h,m,s)
    }
}
