package com.example.publictransport.dgis

import android.util.Log
import com.example.publictransport.ui.PathResponse
import com.example.publictransport.ui.TripResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TwoGisRouteService.kt
object TwoGisRouteService {
    private const val API_KEY = "6ce85e72-0bb5-4e3c-a8f8-a1a386f48a11"

    suspend fun fetchRoutes(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double,
        modes: List<String> = listOf("bus", "tram", "trolleybus", "metro")
    ): List<TripResponse> = withContext(Dispatchers.IO) {
        try {
            val req = TwoGisTripRequest(
                source = PointInfo("Начало", Point(fromLat, fromLon)),
                target = PointInfo("Конец", Point(toLat, toLon)),
                transport = modes
            )

            Log.d("TwoGisRouteService", "Запрос маршрутов: $fromLat,$fromLon -> $toLat,$toLon")

            val response = TwoGisRetrofit.api.getTrips(API_KEY, req)

            Log.d("TwoGisRouteService", "Получено ${response.size} вариантов маршрута")

            response.mapIndexed { index, variant ->
                Log.d("TwoGisRouteService", "Обработка варианта $index")
                Log.d("TwoGisRouteService", "waypoints: ${variant.waypoints.size}")
                Log.d("TwoGisRouteService", "movements: ${variant.movements.size}")

                // Собираем все точки маршрута в правильном порядке
                val routePoints = mutableListOf<Point>()

                // 1. Начальная точка (откуда идем)
                routePoints.add(Point(fromLat, fromLon))

                // 2. Точки из movements (остановки и переходы)
                variant.movements
                    .filter { it.type in listOf("passage", "transfer") }
                    .forEach { movement ->
                        movement.waypoint.location?.let { location ->
                            // Проверяем, что точка не дублируется
                            val isDuplicate = routePoints.any { existing ->
                                kotlin.math.abs(existing.lat - location.lat) < 0.0001 &&
                                        kotlin.math.abs(existing.lon - location.lon) < 0.0001
                            }
                            if (!isDuplicate) {
                                routePoints.add(location)
                                Log.d("TwoGisRouteService", "Добавлена точка: ${location.lat}, ${location.lon}")
                            }
                        }
                    }

                // 3. Точки из waypoints (основные остановки)
                variant.waypoints.forEach { waypoint ->
                    val location = waypoint.location
                    if (location != null) {
                        val isDuplicate = routePoints.any { existing ->
                            kotlin.math.abs(existing.lat - location.lat) < 0.0001 &&
                                    kotlin.math.abs(existing.lon - location.lon) < 0.0001
                        }
                        if (!isDuplicate) {
                            routePoints.add(location)
                            Log.d("TwoGisRouteService", "Добавлена waypoint: ${location.lat}, ${location.lon}")
                        }
                    } else {
                        Log.w("TwoGisRouteService", "Пропущен waypoint с null location")
                    }
                }


                // 4. Конечная точка (куда идем)
                val isEndDuplicate = routePoints.any { existing ->
                    kotlin.math.abs(existing.lat - toLat) < 0.0001 &&
                            kotlin.math.abs(existing.lon - toLon) < 0.0001
                }
                if (!isEndDuplicate) {
                    routePoints.add(Point(toLat, toLon))
                }

                // Получаем информацию о пешеходных участках
                val walkMovements = variant.movements.filter { it.type == "walkway" }
                val startWalk = walkMovements.firstOrNull()
                val endWalk = walkMovements.lastOrNull()

                Log.d("TwoGisRouteService", "Маршрут $index: ${routePoints.size} точек")
                routePoints.forEachIndexed { i, point ->
                    Log.d("TwoGisRouteService", "  Точка $i: ${point.lat}, ${point.lon}")
                }

                // Создаем несколько PathResponse для каждого транспортного сегмента
                val paths = mutableListOf<PathResponse>()

                // Группируем waypoints по маршрутам
                val routeGroups = variant.waypoints.groupBy { it.routes_names.firstOrNull() ?: "unknown" }

                routeGroups.forEach { (routeName, waypoints) ->
                    val segmentPoints = waypoints.mapNotNull { it.location }

                    paths.add(
                        PathResponse(
                            routeId = routeName.toLongOrNull() ?: System.currentTimeMillis(),
                            name = routeName,
                            directionIndex = 0,
                            walkDistance = startWalk?.distance ?: 0,
                            walkDuration = formatSec(startWalk?.moving_duration ?: 0),
                            rideDistance = variant.total_distance / routeGroups.size, // Делим на количество сегментов
                            rideDuration = formatSec(variant.total_duration),
                            stops = segmentPoints
                        )
                    )
                }

                // Если нет отдельных сегментов, создаем один общий
                if (paths.isEmpty()) {
                    paths.add(
                        PathResponse(
                            routeId = System.currentTimeMillis(),
                            name = variant.waypoints.firstOrNull()?.routes_names?.joinToString(", ")
                                ?: "Маршрут ${index + 1}",
                            directionIndex = 0,
                            walkDistance = startWalk?.distance ?: 0,
                            walkDuration = formatSec(startWalk?.moving_duration ?: 0),
                            rideDistance = variant.total_distance,
                            rideDuration = formatSec(variant.total_duration),
                            stops = routePoints
                        )
                    )
                }

                TripResponse(
                    startWalkDistance = startWalk?.distance ?: 0,
                    startWalkDuration = formatSec(startWalk?.moving_duration ?: 0),
                    endWalkDistance = endWalk?.distance ?: 0,
                    endWalkDuration = formatSec(endWalk?.moving_duration ?: 0),
                    paths = paths
                )
            }
        } catch (e: Exception) {
            Log.e("TwoGisRouteService", "Ошибка при получении маршрутов", e)
            throw e
        }
    }

    private fun formatSec(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}