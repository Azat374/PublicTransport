// MapScreenRoute.kt
package com.example.publictransport.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.createBitmap
import com.example.publictransport.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationTokenSource
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

/**
 * @param fusedLocationClient – для кнопки «центрирования на пользователе».
 * @param polyString – строка формата
 *     "lat,lon;lat,lon;…|lat,lon;lat,lon;…|…"
 *   где каждая секция между '|' описывает одну полилинию (список точек через ';').
 * @param stopsString – строка формата
 *     "lat,lon;lat,lon;…;lat,lon"
 *   содержащая координаты **только** тех точек, которые являются автобусными остановками.
 */
@Composable
fun MapScreenRoute(
    fusedLocationClient: FusedLocationProviderClient,
    polyString: String,
    stopsString: String
) {
    val context = LocalContext.current

    // 1) Разбираем polyString в List<List<Point>> – те самые «линии маршрута»
    val allPolylines: List<List<Point>> = polyString
        .split("|")
        .mapNotNull { segmentStr ->
            val pts = segmentStr.split(";").mapNotNull { coordStr ->
                val parts = coordStr.split(",")
                if (parts.size != 2) return@mapNotNull null
                try {
                    val lat = parts[0].toDouble()
                    val lon = parts[1].toDouble()
                    Point(lat, lon)
                } catch (e: Exception) {
                    Log.w("MapScreenRoute", "Не удалось распарсить '$coordStr': $e")
                    null
                }
            }
            if (pts.size >= 2) pts else null
        }

    // 2) Разбираем stopsString в List<Point> – реальные остановки
    val stopMarkers: List<Point> = stopsString
        .split(";")
        .mapNotNull { coordStr ->
            val parts = coordStr.split(",")
            if (parts.size != 2) return@mapNotNull null
            try {
                val lat = parts[0].toDouble()
                val lon = parts[1].toDouble()
                Point(lat, lon)
            } catch (e: Exception) {
                Log.w("MapScreenRoute", "Не удалось распарсить остановку '$coordStr': $e")
                null
            }
        }

    Log.d("MapScreenRoute", "Получено ${allPolylines.size} полилиний для отрисовки")
    Log.d("MapScreenRoute", "Получено ${stopMarkers.size} остановок для отрисовки")

    // Сохраняем ссылку на MapView, чтобы потом перемещать камеру и добавлять placemark
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // --- Сюда добавляем состояние для пользовательского маркера ---
    var userPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mapViewRef = mv
                    try {
                        mv.map.mapObjects.clear()

                        // 3) Собираем все точки из полилиний, чтобы вычислить границы и начальный зум
                        val flatPoints = allPolylines.flatten()

                        // Эта переменная будет содержать значение зума после установки камеры
                        var actualZoom = 0f

                        if (flatPoints.isNotEmpty()) {
                            if (flatPoints.size == 1) {
                                // Если ровно одна точка → показываем её поближе
                                val cp = CameraPosition(flatPoints.first(), 14f, 0f, 0f)
                                mv.map.move(cp, Animation(Animation.Type.SMOOTH, 1f), null)
                                actualZoom = 14f
                            } else {
                                // Несколько точек → вычисляем BoundingBox и лавируем так, чтобы всё поместилось
                                val minLat = flatPoints.minOf { it.latitude }
                                val maxLat = flatPoints.maxOf { it.latitude }
                                val minLon = flatPoints.minOf { it.longitude }
                                val maxLon = flatPoints.maxOf { it.longitude }

                                val boundingBox = BoundingBox(
                                    Point(minLat, minLon),
                                    Point(maxLat, maxLon)
                                )
                                // cameraPosition(bound) возвращает оптимальный CameraPosition,
                                // где .zoom — тот, который «вписывает» boundingBox в экран
                                val camPos = mv.map.cameraPosition(boundingBox)
                                // Немного уменьшим зум, чтобы вокруг была небольшая «поляна»
                                val adjustedZoom = (camPos.zoom - 1f).coerceAtLeast(8f)
                                val finalPos = CameraPosition(camPos.target, adjustedZoom, 0f, 0f)
                                mv.map.move(finalPos, Animation(Animation.Type.SMOOTH, 1f), null)
                                actualZoom = adjustedZoom
                            }
                        } else {
                            // Если вдруг нет ни одной точки (аномалия) → центрируем на Алматы по умолчанию
                            mv.map.move(
                                CameraPosition(Point(43.2375, 76.9457), 10f, 0f, 0f),
                                Animation(Animation.Type.SMOOTH, 1f),
                                null
                            )
                            actualZoom = 10f
                        }

                        // 4) Рисуем полилинии разными цветами
                        val colors = listOf(
                            0xFF0066CC.toInt(), // синий
                            0xFFFF8800.toInt(), // оранжевый
                            0xFF33AA33.toInt(), // зеленый
                            0xFFCC0066.toInt(), // розовый
                            0xFF8800FF.toInt()  // фиолетовый
                        )
                        allPolylines.forEachIndexed { idx, polyPoints ->
                            try {
                                val polyline = mv.map.mapObjects.addPolyline(Polyline(polyPoints))
                                polyline.setStrokeWidth(6f)
                                polyline.setStrokeColor(colors[idx % colors.size])
                                Log.d(
                                    "MapScreenRoute",
                                    "Добавлена полилиния $idx: ${polyPoints.size} точек, цвет ${colors[idx % colors.size]}"
                                )
                            } catch (e: Exception) {
                                Log.e("MapScreenRoute", "Ошибка при добавлении полилинии $idx", e)
                            }
                        }

                        // 5) Ставим маркеры на первой и последней точке (как раньше)
                        if (flatPoints.isNotEmpty()) {
                            mv.map.mapObjects.addPlacemark(flatPoints.first())
                            if (flatPoints.size > 1) {
                                mv.map.mapObjects.addPlacemark(flatPoints.last())
                            }
                        }

                        // 6) Рисуем остановки (иконка R.drawable.bus_stop)
                        if (stopMarkers.isNotEmpty()) {
                            val bitmap = getBitmapFromVectorDrawable(context, R.drawable.bus_stop)
                            val icon = ImageProvider.fromBitmap(bitmap)
                            stopMarkers.forEach { p ->
                                val placemark = mv.map.mapObjects.addPlacemark(p)
                                placemark.setIcon(icon)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MapScreenRoute", "Ошибка инициализации карты", e)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 7) Кнопка «Центрировать на пользователе» + ставим маркер с иконкой пользователя
        FloatingActionButton(
            onClick = {
                val mv = mapViewRef
                if (mv == null) {
                    Log.w("MapScreenRoute", "MapView ещё не инициализирован.")
                    return@FloatingActionButton
                }

                // Проверяем разрешение ACCESS_FINE_LOCATION
                val hasFineLocation = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PermissionChecker.PERMISSION_GRANTED

                if (!hasFineLocation) {
                    Log.w("MapScreenRoute", "Нет разрешения на ACCESS_FINE_LOCATION")
                    return@FloatingActionButton
                }

                try {
                    val cts = CancellationTokenSource()
                    fusedLocationClient
                        .getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                            cts.token
                        )
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                val userPoint = Point(location.latitude, location.longitude)

                                // Перемещаем камеру к пользователю
                                val currentZoom = mv.map.cameraPosition.zoom
                                val targetZoom = (currentZoom.coerceAtLeast(10f))
                                    .coerceAtMost(18f)
                                mv.map.move(
                                    CameraPosition(userPoint, targetZoom, 0f, 0f),
                                    Animation(Animation.Type.SMOOTH, 1f),
                                    null
                                )

                                // Иконка пользователя
                                val userBitmap = getBitmapFromVectorDrawable(
                                    context,
                                    R.drawable.ic_user_location
                                )
                                val userIcon = ImageProvider.fromBitmap(userBitmap)

                                if (userPlacemark == null) {
                                    // если это первый раз — создаём новый Placemark и сохраняем ссылку
                                    val placemark = mv.map.mapObjects.addPlacemark(userPoint)
                                    placemark.setIcon(userIcon)
                                    userPlacemark = placemark
                                } else {
                                    // если маркер уже есть, просто обновляем его геометрию
                                    userPlacemark?.geometry = userPoint
                                }
                            } else {
                                Log.w(
                                    "MapScreenRoute",
                                    "Не удалось получить местоположение пользователя."
                                )
                            }
                        }
                        .addOnFailureListener { exc ->
                            Log.e(
                                "MapScreenRoute",
                                "Ошибка при получении местоположения: $exc"
                            )
                        }
                } catch (se: SecurityException) {
                    Log.e(
                        "MapScreenRoute",
                        "SecurityException при попытке getCurrentLocation: $se"
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Центрировать на мне"
            )
        }
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
