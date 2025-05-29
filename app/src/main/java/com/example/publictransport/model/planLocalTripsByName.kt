package com.example.publictransport.model
import com.example.publictransport.dgis.Point
import com.example.publictransport.ui.PathResponse
import com.example.publictransport.ui.TripResponse
import kotlin.math.*

private const val CLOSE_THRESHOLD_METERS = 100.0

/**
 * Планы маршрутов по названиям остановок, используя только локальные данные.
 * Если остановки ближе пары сотен метров — возвращает единственный пеший маршрут.
 */
fun planLocalTripsByName(
    fromNameQuery: String,
    toNameQuery: String,
    allRoutes: List<Route>,
    allStops: List<Stop>
): List<TripResponse> {
    // 1) Находим сами остановки по подстроке
    val fromStop = allStops.firstOrNull {
        it.name.ru.contains(fromNameQuery, ignoreCase = true) ||
                it.name.en.contains(fromNameQuery, ignoreCase = true)
    } ?: return emptyList()
    val toStop = allStops.firstOrNull {
        it.name.ru.contains(toNameQuery, ignoreCase = true) ||
                it.name.en.contains(toNameQuery, ignoreCase = true)
    } ?: return emptyList()

    // 2) Если остановки очень близко — пеший маршрут
    val distBetween = haversine(
        fromStop.point[0], fromStop.point[1],
        toStop.point[0],   toStop.point[1]
    )
    if (distBetween <= CLOSE_THRESHOLD_METERS) {
        val durationSec = (distBetween / 1.4).roundToInt()  // средняя скорость 1.4 м/с
        return listOf(
            TripResponse(
                startWalkDistance = distBetween.roundToInt(),
                startWalkDuration = formatSec(durationSec),
                endWalkDistance   = 0,
                endWalkDuration   = "00:00:00",
                paths = listOf(
                    PathResponse(
                        routeId        = 0L,
                        name = "",
                        directionIndex = 0,
                        walkDistance   = distBetween.roundToInt(),
                        walkDuration   = formatSec(durationSec),
                        rideDistance   = 0,
                        rideDuration   = "00:00:00",
                        stops          = listOf(
                            Point(fromStop.point[0], fromStop.point[1]),
                            Point(toStop.point[0],   toStop.point[1])
                        )
                    )
                )
            )
        )
    }

    // 3) Строим индекс остановок
    val stopsMap = allStops.associateBy { it.id }

    val result = mutableListOf<TripResponse>()
    allRoutes.forEach { route ->
        route.directions.forEach { dir ->
            val stopIds = dir.stops.map { it.stopId }
            val idxFrom = stopIds.indexOf(fromStop.id)
            val idxTo   = stopIds.indexOf(toStop.id)
            if (idxFrom == -1 || idxTo == -1 || idxFrom == idxTo) return@forEach

            // 4) Выбираем диапазон (прямо или назад)
            val indices = if (idxFrom < idxTo) idxFrom..idxTo else idxFrom downTo idxTo

            // 5) Собираем точки
            val segmentPoints = indices.mapNotNull { i ->
                stopsMap[ stopIds[i] ]?.point?.let { coords ->
                    Point(lat = coords[0], lon = coords[1])
                }
            }
            if (segmentPoints.size < 2) return@forEach

            // 6) Считаем расстояние по offsetDistance
            val offsetFrom = dir.stops[idxFrom].offsetDistance
            val offsetTo   = dir.stops[idxTo].offsetDistance
            val rideDist   = abs((offsetTo - offsetFrom)).roundToInt()

            result += TripResponse(
                startWalkDistance = 0,
                startWalkDuration = "00:00:00",
                endWalkDistance   = 0,
                endWalkDuration   = "00:00:00",
                paths = listOf(
                    PathResponse(
                        routeId        = route.id,
                        name = route.name.ru,
                        directionIndex = dir.index,
                        walkDistance   = 0,
                        walkDuration   = "00:00:00",
                        rideDistance   = rideDist,
                        rideDuration   = "00:00:00",
                        stops          = segmentPoints
                    )
                )
            )
        }
    }
    return result
}

/** Haversine для метрик */
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    return R * c
}

/** Форматирует секунды в "HH:MM:SS" */
private fun formatSec(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
