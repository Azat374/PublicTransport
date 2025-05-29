package com.example.publictransport.model

data class Route(
    val id: Long,
    val typeId: Int,
    val name: LocalizedName,
    val directions: List<RouteDirection>
)

data class LocalizedName(
    val ru: String,
    val kk: String,
    val en: String
)

data class RouteDirection(
    val index: Int,
    val distance: Double,
    val stops: List<RouteStop>
)

data class RouteStop(
    val stopId: Long,
    val lineIndex: Int,
    val offsetDistance: Double,
    val distance: Int
)
