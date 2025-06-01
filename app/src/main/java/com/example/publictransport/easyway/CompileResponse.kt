// CompileResponse.kt
package com.example.publictransport.easyway

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompileResponse(
    val time: Double,
    val ways: List<Way> = emptyList()
)

@Serializable
data class Way(
    val routes: List<WayRoute> = emptyList(),

    @SerialName("wayTimeAuto")
    val wayTimeAuto: Int,

    @SerialName("wayTimeTravel")
    val wayTimeTravel: Int,

    @SerialName("wayPrice")
    val wayPrice: Int,

    @SerialName("wayDetails")
    val wayDetails: List<WayDetail> = emptyList(),

    @SerialName("wayTime")
    val wayTime: Int,

    @SerialName("travelLength")
    val travelLength: Double,

    @SerialName("wayTimeComparer")
    val wayTimeComparer: Double,

    @SerialName("autoLength")
    val autoLength: Double,

    @SerialName("wayLength")
    val wayLength: Double
)

@Serializable
data class WayRoute(
    val id: String,
    val startPoint: WayRoutePoint,
    val stopPoint: WayRoutePoint,
    val speed: String,
    val length: Double,
    val price: String,
    val transportType: String,
    val transportTypeId: Int,
    val transportIsSuburban: Int,
    val interval: String,         // здесь оставляем String, поскольку в "routes" приходят строки
    val workTime: String,
    val routeNumber: String,
    val color: String,
    val icon: Int,
    val showTransportType: Boolean,
    val isBasic: String,
    val transportKey: String,
    val transportName: String,
    val isSingleTrip: Int,
    val routeTime: Double,
    val gps: Int
)

@Serializable
data class WayRoutePoint(
    val position: String
)

@Serializable
data class WayDetail(
    val type: String,      // "first", "route" или "last"
    val stop: String? = null,

    @SerialName("stop_id")
    val stopId: String? = null,

    val length: Int,
    val time: Int,

    // Далее — поля, которые есть только если type == "route"
    val id: String? = null,
    val startPosition: String? = null,
    val stopPosition: String? = null,
    val index: String? = null,
    val route: String? = null,

    @SerialName("route_color")
    val routeColor: String? = null,

    @SerialName("route_icon")
    val routeIcon: Int? = null,

    @SerialName("route_is_single_trip")
    val routeIsSingleTrip: Int? = null,

    @SerialName("route_type")
    val routeType: String? = null,

    @SerialName("route_show_transport_type")
    val routeShowTransportType: Boolean? = null,

    @SerialName("route_transport_key")
    val routeTransportKey: String? = null,

    @SerialName("route_transport_name")
    val routeTransportName: String? = null,

    val price: String? = null,

    // <-- поменяли тип с Int? на Double?
    val interval: Double? = null,

    @SerialName("stop_begin")
    val stopBegin: String? = null,

    @SerialName("stop_begin_id")
    val stopBeginId: String? = null,

    @SerialName("stop_end")
    val stopEnd: String? = null,

    @SerialName("stop_end_id")
    val stopEndId: String? = null,

    val features: String? = null,

    @SerialName("route_gps")
    val routeGps: Int? = null,

    @SerialName("route_schedules")
    val routeSchedules: Int? = null,

    val direction: String? = null
)
