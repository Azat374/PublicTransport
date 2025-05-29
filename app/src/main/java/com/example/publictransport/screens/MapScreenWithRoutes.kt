// MapScreenWithRoutes.kt
package com.example.publictransport.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.example.publictransport.R
import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader
import com.google.android.gms.location.FusedLocationProviderClient
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

// Класс для хранения информации о движущемся автобусе
data class MovingBus(
    val placemark: PlacemarkMapObject,
    val route: List<Point>,
    val stopPoints: List<Point>, // координаты остановок
    var currentSegment: Int = 0, // текущий сегмент пути (между двумя точками)
    var progress: Float = 0f, // прогресс по текущему сегменту (0.0 - 1.0)
    val speed: Float = 0.004f, // скорость движения по сегменту за кадр
    var isWaiting: Boolean = false, // находится ли автобус в ожидании на остановке
    var waitStartTime: Long = 0L, // время начала ожидания
    val waitDuration: Long = 5000L, // длительность ожидания в миллисекундах (5 секунд)
    var lastStopIndex: Int = -1, // индекс последней остановки, где останавливался автобус
    var hasLeftStop: Boolean = true // покинул ли автобус зону остановки
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenWithRoutes(
    fusedLocationClient: FusedLocationProviderClient,
    routes: List<Route>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Список остановок, грузим один раз при запуске
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Список движущихся автобусов
    var movingBuses by remember { mutableStateOf<List<MovingBus>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            stops = JsonLoader.loadStops(context)
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }

    // Быстрый доступ по id
    val stopsMap = remember(stops) { stops.associateBy { it.id } }

    // Корутина для плавного движения автобусов
    LaunchedEffect(movingBuses) {
        if (movingBuses.isNotEmpty()) {
            while (true) {
                delay(50) // обновление каждые 50 мс для плавности
                val currentTime = System.currentTimeMillis()

                movingBuses.forEach { bus ->
                    if (bus.route.size >= 2) {
                        // Проверяем, находится ли автобус в ожидании на остановке
                        if (bus.isWaiting) {
                            // Проверяем, прошло ли время ожидания
                            if (currentTime - bus.waitStartTime >= bus.waitDuration) {
                                bus.isWaiting = false
                                bus.hasLeftStop = false // помечаем, что нужно покинуть зону остановки
                            }
                            // Если автобус ждет, не двигаем его
                            return@forEach
                        }

                        // Увеличиваем прогресс
                        bus.progress += bus.speed

                        // Если дошли до конца сегмента, переходим к следующему
                        if (bus.progress >= 1f) {
                            bus.progress = 0f
                            bus.currentSegment = (bus.currentSegment + 1) % (bus.route.size - 1)
                        }

                        // Вычисляем текущую позицию с интерполяцией
                        val currentPoint = bus.route[bus.currentSegment]
                        val nextPoint = bus.route[(bus.currentSegment + 1) % bus.route.size]

                        val interpolatedPosition = interpolatePoints(
                            currentPoint,
                            nextPoint,
                            bus.progress
                        )

                        // Проверяем, находится ли автобус рядом с остановкой
                        val nearestStopInfo = findNearestStopWithIndex(interpolatedPosition, bus.stopPoints)
                        val nearestStop = nearestStopInfo?.first
                        val nearestStopIndex = nearestStopInfo?.second

                        if (nearestStop != null && nearestStopIndex != null) {
                            val isNearCurrentStop = isNearStop(interpolatedPosition, nearestStop, 50.0)

                            if (isNearCurrentStop) {
                                // Проверяем, покинул ли автобус зону предыдущей остановки
                                if (!bus.hasLeftStop) {
                                    bus.hasLeftStop = true
                                }

                                // Проверяем, не та ли это остановка, где мы уже останавливались
                                if (bus.hasLeftStop && nearestStopIndex != bus.lastStopIndex) {
                                    // Автобус подошел к новой остановке, начинаем ожидание
                                    bus.isWaiting = true
                                    bus.waitStartTime = currentTime
                                    bus.lastStopIndex = nearestStopIndex
                                    bus.hasLeftStop = false
                                    // Устанавливаем точную позицию на остановке
                                    bus.placemark.geometry = nearestStop
                                } else {
                                    // Обновляем позицию на карте
                                    bus.placemark.geometry = interpolatedPosition
                                }
                            } else {
                                // Автобус не рядом с остановкой, помечаем что покинул зону
                                if (!bus.hasLeftStop) {
                                    bus.hasLeftStop = true
                                }
                                // Обновляем позицию на карте
                                bus.placemark.geometry = interpolatedPosition
                            }
                        } else {
                            // Обновляем позицию на карте
                            bus.placemark.geometry = interpolatedPosition
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Маршруты на карте (${routes.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Показываем выбранные маршруты
            if (routes.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routes) { route ->
                        RouteChip(route = route)
                    }
                }
            }

            // Карта
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                // Находим все точки маршрутов для установки камеры
                                val allPoints = routes
                                    .flatMap { route ->
                                        route.directions.flatMap { dir -> dir.stops }
                                    }
                                    .mapNotNull { routeStop ->
                                        stopsMap[routeStop.stopId]?.point
                                    }
                                    .map { coords -> Point(coords[0], coords[1]) }

                                if (allPoints.isNotEmpty()) {
                                    // Устанавливаем камеру на центр всех точек
                                    val centerLat = allPoints.map { it.latitude }.average()
                                    val centerLon = allPoints.map { it.longitude }.average()
                                    val centerPoint = Point(centerLat, centerLon)

                                    map.move(
                                        CameraPosition(centerPoint, 12f, 0f, 0f),
                                        Animation(Animation.Type.SMOOTH, 1f),
                                        null
                                    )
                                } else {
                                    // Алматы по умолчанию
                                    map.move(
                                        CameraPosition(Point(43.238949, 76.889709), 11f, 0f, 0f),
                                        Animation(Animation.Type.SMOOTH, 1f),
                                        null
                                    )
                                }

                                val mapObjects = map.mapObjects
                                val busesForRoutes = mutableListOf<MovingBus>()

                                // Рисуем каждый маршрут
                                routes.forEachIndexed { routeIndex, route ->
                                    val routeColor = getRouteColor(route.typeId, routeIndex)

                                    route.directions.forEach { direction ->
                                        val points = direction.stops
                                            .mapNotNull { routeStop ->
                                                stopsMap[routeStop.stopId]?.point
                                            }
                                            .map { coords -> Point(coords[0], coords[1]) }

                                        if (points.size >= 2) {
                                            // Рисуем полилинию маршрута
                                            val polylineObj: PolylineMapObject =
                                                mapObjects.addPolyline(Polyline(points))
                                            polylineObj.setStrokeColor(routeColor)
                                            polylineObj.setStrokeWidth(5f)

                                            // Создаем сглаженный маршрут для более плавного движения
                                            val smoothedRoute = createSmoothRoute(points)

                                            // Создаем движущиеся автобусы для этого направления
                                            val buses = createMovingBusesForRoute(
                                                context,
                                                mapObjects,
                                                smoothedRoute,
                                                points, // передаем оригинальные точки остановок
                                                route.typeId,
                                                10 // количество автобусов на маршрут
                                            )
                                            busesForRoutes.addAll(buses)
                                        }

                                        // Добавляем маркеры остановок
                                        direction.stops.forEach { routeStop ->
                                            stopsMap[routeStop.stopId]?.let { stop ->
                                                val point = Point(stop.point[0], stop.point[1])
                                                val bitmap = getBitmapFromVectorDrawable(
                                                    context,
                                                    getStopMarkerResource(route.typeId)
                                                )
                                                val placemark = mapObjects.addPlacemark(
                                                    point,
                                                    ImageProvider.fromBitmap(bitmap)
                                                )

                                                // Можно добавить пользовательские данные для popup
                                                placemark.userData = stop.name.ru
                                            }
                                        }
                                    }
                                }

                                // Обновляем список движущихся автобусов
                                movingBuses = busesForRoutes
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Находит ближайшую остановку к текущей позиции и возвращает её с индексом
 */
private fun findNearestStopWithIndex(currentPosition: Point, stopPoints: List<Point>): Pair<Point, Int>? {
    return stopPoints.mapIndexed { index, stop ->
        Pair(stop, index) to calculateDistance(currentPosition, stop)
    }.minByOrNull { it.second }?.first
}

/**
 * Проверяет, находится ли автобус рядом с остановкой
 */
private fun isNearStop(busPosition: Point, stopPosition: Point, thresholdMeters: Double): Boolean {
    val distance = calculateDistance(busPosition, stopPosition)
    return distance <= thresholdMeters
}

/**
 * Вычисляет расстояние между двумя точками в метрах (приблизительно)
 */
private fun calculateDistance(point1: Point, point2: Point): Double {
    val earthRadius = 6371000.0 // радиус Земли в метрах
    val dLat = Math.toRadians(point2.latitude - point1.latitude)
    val dLon = Math.toRadians(point2.longitude - point1.longitude)
    val lat1 = Math.toRadians(point1.latitude)
    val lat2 = Math.toRadians(point2.latitude)

    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2) * kotlin.math.cos(lat1) * kotlin.math.cos(lat2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

    return earthRadius * c
}

/**
 * Создает сглаженный маршрут с дополнительными промежуточными точками
 */
private fun createSmoothRoute(originalPoints: List<Point>): List<Point> {
    if (originalPoints.size < 2) return originalPoints

    val smoothedPoints = mutableListOf<Point>()

    for (i in 0 until originalPoints.size - 1) {
        val currentPoint = originalPoints[i]
        val nextPoint = originalPoints[i + 1]

        // Добавляем текущую точку
        smoothedPoints.add(currentPoint)

        // Добавляем промежуточные точки для более плавного движения
        val intermediateSteps = 5 // количество промежуточных точек
        for (j in 1 until intermediateSteps) {
            val progress = j.toFloat() / intermediateSteps
            val interpolatedPoint = interpolatePoints(currentPoint, nextPoint, progress)
            smoothedPoints.add(interpolatedPoint)
        }
    }

    // Добавляем последнюю точку
    smoothedPoints.add(originalPoints.last())

    return smoothedPoints
}

/**
 * Интерполяция между двумя точками
 */
private fun interpolatePoints(point1: Point, point2: Point, progress: Float): Point {
    val lat = point1.latitude + (point2.latitude - point1.latitude) * progress
    val lon = point1.longitude + (point2.longitude - point1.longitude) * progress
    return Point(lat, lon)
}

/**
 * Создает движущиеся автобусы для одного маршрута
 */
private fun createMovingBusesForRoute(
    context: Context,
    mapObjects: com.yandex.mapkit.map.MapObjectCollection,
    routePoints: List<Point>,
    stopPoints: List<Point>, // оригинальные точки остановок
    typeId: Int,
    busCount: Int
): List<MovingBus> {
    val buses = mutableListOf<MovingBus>()

    if (routePoints.size < 2) return buses

    // Создаем иконку автобуса
    val busBitmap = getBitmapFromVectorDrawable(context, getBusMarkerResource(typeId))
    val busImageProvider = ImageProvider.fromBitmap(busBitmap)

    // Размещаем автобусы на равных интервалах вдоль маршрута
    for (i in 0 until busCount) {
        // Вычисляем начальную позицию автобуса
        val totalSegments = routePoints.size - 1
        val segmentPerBus = totalSegments.toFloat() / busCount
        val startSegmentFloat = segmentPerBus * i
        val startSegment = startSegmentFloat.toInt() % totalSegments
        val startProgress = startSegmentFloat - startSegment

        // Вычисляем начальную позицию
        val currentPoint = routePoints[startSegment]
        val nextPoint = routePoints[(startSegment + 1) % routePoints.size]
        val initialPosition = interpolatePoints(currentPoint, nextPoint, startProgress)

        // Создаем плейсмарк для автобуса
        val busPlacemark = mapObjects.addPlacemark(initialPosition, busImageProvider)

        // Добавляем небольшую случайность в скорость для более реалистичного движения
        val speed = 0.0015f + Random.nextFloat() * 0.001f

        val movingBus = MovingBus(
            placemark = busPlacemark,
            route = routePoints,
            stopPoints = stopPoints,
            currentSegment = startSegment,
            progress = startProgress,
            speed = speed
        )

        buses.add(movingBus)
    }

    return buses
}

@Composable
private fun RouteChip(route: Route) {
    Card(
        modifier = Modifier.wrapContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = when (route.typeId) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (route.typeId) {
                    0 -> Icons.Default.DirectionsBus
                    1 -> Icons.Default.Tram
                    else -> Icons.Default.DirectionsSubway
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (route.typeId) {
                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
            Text(
                text = route.name.ru,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = when (route.typeId) {
                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }
}

private fun getRouteColor(typeId: Int, routeIndex: Int): Int {
    return when (typeId) {
        0 -> { // Автобусы - оттенки синего
            val blueColors = arrayOf(
                Color.BLUE, Color.rgb(0, 100, 200), Color.rgb(30, 144, 255),
                Color.rgb(0, 191, 255), Color.rgb(65, 105, 225)
            )
            blueColors[routeIndex % blueColors.size]
        }
        1 -> { // Троллейбусы - оттенки красного
            val redColors = arrayOf(
                Color.RED, Color.rgb(220, 20, 60), Color.rgb(255, 69, 0),
                Color.rgb(255, 99, 71), Color.rgb(178, 34, 34)
            )
            redColors[routeIndex % redColors.size]
        }
        else -> { // Метро - оттенки зеленого
            val greenColors = arrayOf(
                Color.GREEN, Color.rgb(34, 139, 34), Color.rgb(0, 128, 0),
                Color.rgb(50, 205, 50), Color.rgb(46, 125, 50)
            )
            greenColors[routeIndex % greenColors.size]
        }
    }
}

/**
 * Возвращает ресурс иконки для остановки
 */
private fun getStopMarkerResource(typeId: Int): Int {
    return when (typeId) {
        0 -> R.drawable.bus_stop
        1 -> R.drawable.bus_stop // или создайте отдельную иконку для троллейбуса
        else -> R.drawable.bus_stop // или создайте отдельную иконку для метро
    }
}

/**
 * Возвращает ресурс иконки для автобуса
 */
private fun getBusMarkerResource(typeId: Int): Int {
    return when (typeId) {
        0 -> R.drawable.ic_bus // иконка автобуса
        1 -> R.drawable.ic_trolleybus // иконка троллейбуса
        else -> R.drawable.ic_metro // иконка метро
    }
}

/** Вспомогательная функция для конвертации в Bitmap из VectorDrawable */
private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(context, drawableId)
        ?: error("Drawable not found")
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap
}