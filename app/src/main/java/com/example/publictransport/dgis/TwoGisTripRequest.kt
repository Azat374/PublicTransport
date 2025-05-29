package com.example.publictransport.dgis

// TwoGisPublicTransport.kt

// Тело запроса
data class TwoGisTripRequest(
    val locale: String = "ru",
    val source: PointInfo,
    val target: PointInfo,
    val transport: List<String>,              // e.g. ["bus","tram","trolleybus"]
    val intermediate_points: List<PointInfo>? = null
)

data class PointInfo(
    val name: String,
    val point: Point
)

data class Point(
    val lat: Double,
    val lon: Double
)

// Ответ 2GIS — упрощённый, но с координатами waypoints
data class TwoGisTripResponse(
    val id: String,
    val total_distance: Int,                  // в метрах
    val total_duration: Int,                  // в секундах
    val transfer_count: Int,
    val crossing_count: Int,
    val pedestrian: Boolean,
    val total_walkway_distance: String,       // "пешком 9 мин"
    val transport: List<String>,              // ["bus","tram",...]
    val waypoints: List<WaypointInfo>,
    val movements: List<Movement>
)

/**
 * В 2GIS JSON у каждого waypoint теперь есть объект с координатами.
 * Мы десериализуем его в поле `location`.
 */
data class WaypointInfo(
    val combined: Boolean,
    val routes_names: List<String>,           // номера маршрутов
    val subtype: String,                      // bus, tram, trolleybus…
    val location: Point                       // добавлено: координаты этой точки
)

data class Movement(
    val id: String,
    val type: String,                         // "walkway", "passage", "transfer"
    val distance: Int,
    val moving_duration: Int,
    val waiting_duration: Int,
    val waypoint: MovementWaypoint
)

/**
 * Добавили координаты и здесь, если они есть в JSON движения.
 */
data class MovementWaypoint(
    val subtype: String,                      // "bus", "pedestrian"…
    val name: String,
    val comment: String?,
    val location: Point?                      // иногда движения могут привязываться к точке
)
