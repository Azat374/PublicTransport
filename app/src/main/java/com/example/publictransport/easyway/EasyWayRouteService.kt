//// EasyWayRouteService.kt
//package com.example.publictransport.easyway
//
//import android.util.Log
//import com.example.publictransport.dgis.Point
//import com.example.publictransport.ui.PathResponse
//import com.example.publictransport.ui.TripResponse
//
//object EasyWayRouteService {
//
//    /**
//     * Получить маршруты от точки A до точки B
//     */
//    suspend fun fetchRoutes(
//        fromLat: Double,
//        fromLon: Double,
//        toLat: Double,
//        toLon: Double,
//        modes: List<String> = listOf("bus", "tram", "trolleybus")
//    ): List<TripResponse> {
//        return try {
//            Log.d("EasyWayRouteService", "Запрос маршрутов: ($fromLat,$fromLon) -> ($toLat,$toLon)")
//
//            // Преобразуем режимы транспорта в формат EasyWay
//            val transports = modes.joinToString(",") { mode ->
//                when (mode) {
//                    "bus" -> "bus"
//                    "tram" -> "tram"
//                    "trolleybus" -> "trol"
//                    else -> "bus"
//                }
//            }
//
//            // 1. Получаем варианты маршрутов
//            val compileResponse = EasyWayClient.api.compile(
//                startLat = fromLat,
//                startLng = fromLon,
//                stopLat = toLat,
//                stopLng = toLon,
//                transports = transports
//            )
//
//            Log.d("EasyWayRouteService", "Получено ${compileResponse.ways.size} вариантов")
//
//            // 2. Преобразуем в наш формат TripResponse
//            val trips = compileResponse.ways.mapNotNull { way ->
//                convertWayToTrip(way, fromLat, fromLon, toLat, toLon)
//            }
//
//            Log.d("EasyWayRouteService", "Конвертировано ${trips.size} маршрутов")
//            trips
//
//        } catch (e: Exception) {
//            Log.e("EasyWayRouteService", "Ошибка при получении маршрутов", e)
//            emptyList()
//        }
//    }
//
//    /**
//     * Преобразует Way из EasyWay в наш TripResponse
//     */
//    private suspend fun convertWayToTrip(
//        way: Way,
//        fromLat: Double,
//        fromLon: Double,
//        toLat: Double,
//        toLon: Double
//    ): TripResponse? {
//        try {
//            // Получаем детали пешего перехода
//            val firstDetail = way.wayDetails.firstOrNull { it.type == "first" }
//            val lastDetail = way.wayDetails.lastOrNull { it.type == "last" }
//
//            val startWalkDistance = firstDetail?.length ?: 0
//            val startWalkDuration = formatDuration(firstDetail?.time ?: 0)
//            val endWalkDistance = lastDetail?.length ?: 0
//            val endWalkDuration = formatDuration(lastDetail?.time ?: 0)
//
//            // Получаем маршруты транспорта
//            val routeDetails = way.wayDetails.filter { it.type == "route" }
//
//            val paths = mutableListOf<PathResponse>()
//
//            for (routeDetail in routeDetails) {
//                val routeId = routeDetail.id?.toLongOrNull() ?: continue
//                val routeName = routeDetail.route ?: "Маршрут $routeId"
//
//                // Получаем геометрию маршрута
//                val routePoints = getRouteGeometry(
//                    routeId = routeId.toString(),
//                    startStopId = routeDetail.stopBeginId ?: "",
//                    endStopId = routeDetail.stopEndId ?: "",
//                    fromLat = fromLat,
//                    fromLon = fromLon,
//                    toLat = toLat,
//                    toLon = toLon
//                )
//
//                val pathResponse = PathResponse(
//                    routeId = routeId,
//                    name = routeName,
//                    directionIndex = 0, // EasyWay не предоставляет направление напрямую
//                    walkDistance = 0, // Расстояние пешком внутри маршрута (обычно 0)
//                    walkDuration = "00:00:00",
//                    rideDistance = routeDetail.length.toInt(),
//                    rideDuration = formatDuration(routeDetail.time),
//                    stops = routePoints
//                )
//
//                paths.add(pathResponse)
//            }
//
//            if (paths.isEmpty()) {
//                Log.w("EasyWayRouteService", "Нет транспортных маршрутов в варианте")
//                return null
//            }
//
//            return TripResponse(
//                startWalkDistance = startWalkDistance,
//                startWalkDuration = startWalkDuration,
//                endWalkDistance = endWalkDistance,
//                endWalkDuration = endWalkDuration,
//                paths = paths
//            )
//
//        } catch (e: Exception) {
//            Log.e("EasyWayRouteService", "Ошибка конвертации Way в TripResponse", e)
//            return null
//        }
//    }
//
//    /**
//     * Получает геометрию маршрута через getCompileRoute
//     */
//    private suspend fun getRouteGeometry(
//        routeId: String,
//        startStopId: String,
//        endStopId: String,
//        fromLat: Double,
//        fromLon: Double,
//        toLat: Double,
//        toLon: Double
//    ): List<Point> {
//        return try {
//            val response = EasyWayClient.api.getCompileRoute(
//                ids = routeId,
//                starts = startStopId,
//                stops = endStopId,
//                a = "$fromLat,$fromLon",
//                b = "$toLat,$toLon"
//            )
//
//            val routePoints = mutableListOf<Point>()
//
//            response.routesPoints.forEach { routePointsData ->
//                // Добавляем точки остановок из compile_points
//                routePointsData.compilePoints.forEach { compilePoint ->
//                    // Преобразуем координаты из формата EasyWay (умножены на 1000000)
//                    val lat = compilePoint.x.toDouble() / 1000000
//                    val lon = compilePoint.y.toDouble() / 1000000
//
//                    routePoints.add(Point(lat = lat, lon = lon))
//                }
//
//                // Также можно добавить промежуточные точки из route_points для более детальной геометрии
//                routePointsData.routePoints.forEach { pointGroup ->
//                    pointGroup.forEach { pointCoords ->
//                        if (pointCoords.size >= 2) {
//                            val lat = pointCoords[0].toDouble() / 1000000
//                            val lon = pointCoords[1].toDouble() / 1000000
//                            routePoints.add(Point(lat = lat, lon = lon))
//                        }
//                    }
//                }
//            }
//
//            Log.d("EasyWayRouteService", "Получено ${routePoints.size} точек геометрии для маршрута $routeId")
//            routePoints.distinctBy { "${it.lat},${it.lon}" } // Убираем дубликаты
//
//        } catch (e: Exception) {
//            Log.e("EasyWayRouteService", "Ошибка получения геометрии маршрута $routeId", e)
//            // Возвращаем базовые точки начала и конца
//            listOf(
//                Point(lat = fromLat, lon = fromLon),
//                Point(lat = toLat, lon = toLon)
//            )
//        }
//    }
//
//    /**
//     * Форматирует время в секундах в строку HH:MM:SS
//     */
//    private fun formatDuration(seconds: Int): String {
//        val hours = seconds / 3600
//        val minutes = (seconds % 3600) / 60
//        val secs = seconds % 60
//        return String.format("%02d:%02d:%02d", hours, minutes, secs)
//    }
//}