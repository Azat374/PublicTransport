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
                                            val polylineObj: PolylineMapObject =
                                                mapObjects.addPolyline(Polyline(points))
                                            polylineObj.setStrokeColor(routeColor)
                                            polylineObj.setStrokeWidth(5f)
                                        }

                                        // Добавляем маркеры остановок
                                        direction.stops.forEach { routeStop ->
                                            stopsMap[routeStop.stopId]?.let { stop ->
                                                val point = Point(stop.point[0], stop.point[1])
                                                val bitmap = getBitmapFromVectorDrawable(context, getMarkerResource(route.typeId))
                                                val placemark = mapObjects.addPlacemark(
                                                    point,
                                                    ImageProvider.fromBitmap(bitmap))

                                                // Можно добавить пользовательские данные для popup
                                                placemark.userData = stop.name.ru
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
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

private fun getMarkerResource(typeId: Int): Int {
    return when (typeId) {
        0 -> R.drawable.bus_stop
        1 -> R.drawable.bus_stop // или создайте отдельную иконку для троллейбуса
        else -> R.drawable.bus_stop // или создайте отдельную иконку для метро
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