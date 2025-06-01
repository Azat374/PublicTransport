// CompileRouteResponse.kt
package com.example.publictransport.easyway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompileRouteResponse(
    @SerialName("routes_points")
    val routesPoints: List<RoutePoints> = emptyList()
)

@Serializable
data class RoutePoints(
    @SerialName("r")
    val routeId: Long,

    // Сделали nullable, потому что в JSON иногда "rn": null
    @SerialName("rn")
    val routeNumber: String?,

    @SerialName("tt")
    val transportType: String?,

    @SerialName("compile_points")
    val compilePoints: List<CompilePoint> = emptyList(),

    @SerialName("route_points")
    val routePoints: List<List<List<Long>>> = emptyList(),

    @SerialName("walk_points")
    val walkPoints: List<List<List<Long>>> = emptyList()
)

@Serializable
data class CompilePoint(
    val x: Long,
    val y: Long,
    val p: Long,
    // Эти поля тоже могут быть null
    val i: Long? = null,
    val n: String? = null
)
