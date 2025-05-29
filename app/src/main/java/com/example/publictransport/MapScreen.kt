//package com.example.publictransport
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import androidx.appcompat.content.res.AppCompatResources
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.app.ActivityCompat
//import androidx.core.graphics.createBitmap
//import com.google.android.gms.location.*
//import com.yandex.mapkit.Animation
//import com.example.publictransport.dgis.Point
//import com.example.publictransport.R
//import com.yandex.mapkit.map.CameraPosition
//import com.yandex.mapkit.map.PolylineMapObject
//import com.yandex.mapkit.map.PlacemarkMapObject
//import com.yandex.mapkit.mapview.MapView
//import com.yandex.runtime.image.ImageProvider
//import com.yandex.mapkit.geometry.Polyline
//import com.yandex.mapkit.geometry.BoundingBox
//import android.util.Log
//
///**
// * @param fusedLocationClient для центрирования на пользователя
// * @param routePoints Список точек (Point(lat, lon)), образующих ваш маршрут
// */
//@Composable
//fun MapScreen(
//    fusedLocationClient: FusedLocationProviderClient,
//    routePoints: List<Point>
//) {
//    val context = LocalContext.current
//    var mapView by remember { mutableStateOf<MapView?>(null) }
//    var userPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }
//    var routeLine by remember { mutableStateOf<PolylineMapObject?>(null) }
//    var stopPlacemarks by remember { mutableStateOf<List<PlacemarkMapObject>>(emptyList()) }
//
//    // convert your DTOs into Yandex Points once
//    val yandexPoints = remember(routePoints) {
//        routePoints.map { dto ->
//            com.yandex.mapkit.geometry.Point(dto.lat, dto.lon)
//        }
//    }
//
//    Log.d("MapScreen", "Отображение маршрута с ${yandexPoints.size} точками")
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(factory = { ctx ->
//            val mv = MapView(ctx).apply {
//                // Центрируем карту на маршруте
//                if (yandexPoints.isNotEmpty()) {
//                    if (yandexPoints.size == 1) {
//                        // Если только одна точка
//                        map.move(
//                            CameraPosition(yandexPoints.first(), 14f, 0f, 0f),
//                            Animation(Animation.Type.SMOOTH, 1f),
//                            null
//                        )
//                    } else {
//                        // Если несколько точек - вычисляем bounding box
//                        val minLat = yandexPoints.minOf { it.latitude }
//                        val maxLat = yandexPoints.maxOf { it.latitude }
//                        val minLon = yandexPoints.minOf { it.longitude }
//                        val maxLon = yandexPoints.maxOf { it.longitude }
//
//                        val boundingBox = BoundingBox(
//                            com.yandex.mapkit.geometry.Point(minLat, minLon),
//                            com.yandex.mapkit.geometry.Point(maxLat, maxLon)
//                        )
//
//                        // Устанавливаем видимую область с отступами
//                        val cameraPosition = map.cameraPosition(boundingBox)
//                        map.move(
//                            CameraPosition(
//                                cameraPosition.target,
//                                cameraPosition.zoom - 1f, // немного отдаляем для отступов
//                                0f,
//                                0f
//                            ),
//                            Animation(Animation.Type.SMOOTH, 1f),
//                            null
//                        )
//                    }
//                }
//            }
//            mapView = mv
//
//            // Очищаем предыдущие объекты
//            mv.map.mapObjects.clear()
//
//            // Рисуем маршрут как отдельные сегменты между остановками
//            if (yandexPoints.size >= 2) {
//                // Вместо одной длинной линии рисуем короткие сегменты
//                for (i in 0 until yandexPoints.size - 1) {
//                    val segment = listOf(yandexPoints[i], yandexPoints[i + 1])
//                    val polyObj = mv.map.mapObjects.addPolyline(Polyline(segment))
//                    polyObj.setStrokeColor(0xFF0066CC.toInt())
//                    polyObj.setStrokeWidth(4f)
//                    // Делаем линию пунктирной для большей ясности
//                    polyObj.setDashLength(10f)
//                    polyObj.setGapLength(5f)
//                }
//            }
//
//            // Рисуем остановки
//            yandexPoints.forEachIndexed { index, pt ->
//                try {
//                    val placemark = when (index) {
//                        0 -> {
//                            // Начальная точка - зеленая
//                            mv.map.mapObjects.addPlacemark(
//                                pt,
//                                ImageProvider.fromBitmap(
//                                    createColoredBitmap(context, 0xFF00AA00.toInt(), "A")
//                                )
//                            )
//                        }
//                        yandexPoints.size - 1 -> {
//                            // Конечная точка - красная
//                            mv.map.mapObjects.addPlacemark(
//                                pt,
//                                ImageProvider.fromBitmap(
//                                    createColoredBitmap(context, 0xFFAA0000.toInt(), "B")
//                                )
//                            )
//                        }
//                        else -> {
//                            // Промежуточные остановки - синие
//                            mv.map.mapObjects.addPlacemark(
//                                pt,
//                                ImageProvider.fromBitmap(
//                                    getBitmapFromVectorDrawable(context, R.drawable.bus_stop)
//                                )
//                            )
//                        }
//                    }
//                    stopPlacemarks = stopPlacemarks + placemark
//                } catch (e: Exception) {
//                    Log.e("MapScreen", "Ошибка создания метки остановки", e)
//                    // Fallback: создаем простую метку без иконки
//                    val placemark = mv.map.mapObjects.addPlacemark(pt)
//                    stopPlacemarks = stopPlacemarks + placemark
//                }
//            }
//
//            Log.d("MapScreen", "Добавлено ${stopPlacemarks.size} меток остановок")
//
//            mv
//        }, modifier = Modifier.fillMaxSize())
//
//        // "center on me" button
//        IconButton(
//            onClick = {
//                if (ActivityCompat.checkSelfPermission(
//                        context, Manifest.permission.ACCESS_FINE_LOCATION
//                    ) == PackageManager.PERMISSION_GRANTED
//                ) {
//                    val req = LocationRequest.Builder(5000)
//                        .setMinUpdateIntervalMillis(3000)
//                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//                        .build()
//
//                    fusedLocationClient.requestLocationUpdates(
//                        req,
//                        object : LocationCallback() {
//                            override fun onLocationResult(result: LocationResult) {
//                                val loc = result.lastLocation ?: return
//                                val pt = com.yandex.mapkit.geometry.Point(loc.latitude, loc.longitude)
//                                val mv = mapView ?: return
//
//                                try {
//                                    userPlacemark?.let { mv.map.mapObjects.remove(it) }
//                                    val bmp = getBitmapFromVectorDrawable(context, R.drawable.ic_user_location)
//                                    val img = ImageProvider.fromBitmap(bmp)
//                                    userPlacemark = mv.map.mapObjects.addPlacemark(pt, img)
//
//                                    mv.map.move(
//                                        CameraPosition(pt, 17f, 0f, 0f),
//                                        Animation(Animation.Type.SMOOTH, 0.8f),
//                                        null
//                                    )
//                                } catch (e: Exception) {
//                                    Log.e("MapScreen", "Ошибка центрирования на пользователе", e)
//                                }
//                            }
//                        },
//                        context.mainLooper
//                    )
//                }
//            },
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp)
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.focus_location),
//                contentDescription = "Центрировать на мне"
//            )
//        }
//    }
//}
//
///** Создает цветной битмап с текстом */
//private fun createColoredBitmap(context: Context, color: Int, text: String): Bitmap {
//    return try {
//        val size = 64
//        val bitmap = createBitmap(size, size)
//        val canvas = Canvas(bitmap)
//
//        // Рисуем цветной круг
//        val paint = android.graphics.Paint().apply {
//            this.color = color
//            isAntiAlias = true
//        }
//        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
//
//        // Рисуем белую обводку
//        paint.apply {
//            this.color = 0xFFFFFFFF.toInt()
//            style = android.graphics.Paint.Style.STROKE
//            strokeWidth = 4f
//        }
//        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
//
//        // Рисуем текст
//        paint.apply {
//            this.color = 0xFFFFFFFF.toInt()
//            style = android.graphics.Paint.Style.FILL
//            textSize = 24f
//            textAlign = android.graphics.Paint.Align.CENTER
//            typeface = android.graphics.Typeface.DEFAULT_BOLD
//        }
//        canvas.drawText(text, size / 2f, size / 2f + 8f, paint)
//
//        bitmap
//    } catch (e: Exception) {
//        Log.e("MapScreen", "Ошибка создания цветного битмапа", e)
//        // Fallback
//        createBitmap(48, 48).apply {
//            eraseColor(color)
//        }
//    }
//}
//
///** Вспомогательная функция для конвертации в Bitmap из VectorDrawable */
//private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
//    return try {
//        val drawable = AppCompatResources.getDrawable(context, drawableId)
//            ?: error("Drawable not found")
//        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
//        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
//        val bitmap = createBitmap(width, height)
//        val canvas = Canvas(bitmap)
//        drawable.setBounds(0, 0, width, height)
//        drawable.draw(canvas)
//        bitmap
//    } catch (e: Exception) {
//        Log.e("MapScreen", "Ошибка создания битмапа из векторного drawable", e)
//        // Fallback: создаем простой цветной битмап
//        createBitmap(48, 48).apply {
//            eraseColor(0xFF0066CC.toInt())
//        }
//    }
//}


package com.example.publictransport

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import com.google.android.gms.location.*
import com.yandex.mapkit.Animation
import com.example.publictransport.dgis.Point
import com.example.publictransport.R
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.BoundingBox
import android.util.Log

/**
 * @param fusedLocationClient для центрирования на пользователя
 * @param routePoints Список точек (Point(lat, lon)), образующих ваш маршрут
 */
@Composable
fun MapScreen(
    fusedLocationClient: FusedLocationProviderClient,
    routePoints: List<Point>
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var userPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }
    var routeLines by remember { mutableStateOf<List<PolylineMapObject>>(emptyList()) }
    var placemarks by remember { mutableStateOf<List<PlacemarkMapObject>>(emptyList()) }

    Log.d("MapScreen", "Получено ${routePoints.size} точек для отображения")
    routePoints.forEachIndexed { index, point ->
        Log.d("MapScreen", "Точка $index: ${point.lat}, ${point.lon}")
    }

    // Конвертируем точки в формат Yandex
    val yandexPoints = remember(routePoints) {
        routePoints.map { dto ->
            com.yandex.mapkit.geometry.Point(dto.lat, dto.lon)
        }.also {
            Log.d("MapScreen", "Сконвертировано ${it.size} Yandex точек")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val mv = MapView(ctx).apply {
                if (yandexPoints.isNotEmpty()) {
                    // Центрируем камеру на первой точке
                    map.move(
                        CameraPosition(yandexPoints.first(), 14f, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                }
            }
            mapView = mv

            // Очищаем предыдущие объекты
            mv.map.mapObjects.clear()

            if (yandexPoints.isNotEmpty()) {
                try {
                    // 1. Добавляем маркеры для всех точек
                    yandexPoints.forEachIndexed { index, point ->
                        val iconRes = when (index) {
                            0 -> R.drawable.ic_start_point // Начальная точка
                            yandexPoints.size - 1 -> R.drawable.ic_end_point // Конечная точка
                            else -> R.drawable.bus_stop // Промежуточные остановки
                        }

                        try {
                            val bitmap = getBitmapFromVectorDrawable(ctx, iconRes)
                            val placemark = mv.map.mapObjects.addPlacemark(point, ImageProvider.fromBitmap(bitmap))
                            placemarks = placemarks + placemark
                            Log.d("MapScreen", "Добавлен маркер $index в точке ${point.latitude}, ${point.longitude}")
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Ошибка добавления маркера $index", e)
                            // Fallback: простой маркер
                            val placemark = mv.map.mapObjects.addPlacemark(point)
                            placemarks = placemarks + placemark
                        }
                    }

                    // 2. Добавляем линии между соседними точками (сегментированный маршрут)
                    if (yandexPoints.size >= 2) {
                        // Создаем сегменты маршрута
                        for (i in 0 until yandexPoints.size - 1) {
                            val segmentPoints = listOf(yandexPoints[i], yandexPoints[i + 1])
                            val polyline = mv.map.mapObjects.addPolyline(Polyline(segmentPoints))

                            // Разные цвета для разных типов сегментов
                            when (i) {
                                0 -> {
                                    // Первый сегмент (подход к остановке)
                                    polyline.setStrokeColor(0xFF00AA00.toInt()) // Зеленый
                                    polyline.setStrokeWidth(4f)
                                }
                                yandexPoints.size - 2 -> {
                                    // Последний сегмент (от остановки к цели)
                                    polyline.setStrokeColor(0xFFAA0000.toInt()) // Красный
                                    polyline.setStrokeWidth(4f)
                                }
                                else -> {
                                    // Транспортные сегменты
                                    polyline.setStrokeColor(0xFF0066CC.toInt()) // Синий
                                    polyline.setStrokeWidth(6f)
                                }
                            }

                            routeLines = routeLines + polyline
                            Log.d("MapScreen", "Добавлен сегмент $i -> ${i+1}")
                        }
                    }

                    // 3. Подгоняем камеру под все точки
                    if (yandexPoints.size > 1) {
                        val minLat = yandexPoints.minOf { it.latitude }
                        val maxLat = yandexPoints.maxOf { it.latitude }
                        val minLon = yandexPoints.minOf { it.longitude }
                        val maxLon = yandexPoints.maxOf { it.longitude }

                        val boundingBox = BoundingBox(
                            com.yandex.mapkit.geometry.Point(minLat, minLon),
                            com.yandex.mapkit.geometry.Point(maxLat, maxLon)
                        )

                        val cameraPosition = mv.map.cameraPosition(boundingBox)
                        mv.map.move(
                            cameraPosition,
                            Animation(Animation.Type.SMOOTH, 1.5f),
                            null
                        )

                        Log.d("MapScreen", "Камера подогнана под маршрут: $minLat,$minLon - $maxLat,$maxLon")
                    }

                } catch (e: Exception) {
                    Log.e("MapScreen", "Ошибка при добавлении маршрута на карту", e)
                }
            }

            mv
        }, modifier = Modifier.fillMaxSize())

        // Кнопка "центрировать на мне"
        IconButton(
            onClick = {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val req = LocationRequest.Builder(5000)
                        .setMinUpdateIntervalMillis(3000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()

                    fusedLocationClient.requestLocationUpdates(
                        req,
                        object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                val loc = result.lastLocation ?: return
                                val pt = com.yandex.mapkit.geometry.Point(loc.latitude, loc.longitude)
                                val mv = mapView ?: return

                                try {
                                    // Удаляем предыдущий маркер пользователя
                                    userPlacemark?.let { mv.map.mapObjects.remove(it) }

                                    // Добавляем новый маркер
                                    val bmp = getBitmapFromVectorDrawable(context, R.drawable.ic_user_location)
                                    val img = ImageProvider.fromBitmap(bmp)
                                    userPlacemark = mv.map.mapObjects.addPlacemark(pt, img)

                                    // Центрируем камеру на пользователе
                                    mv.map.move(
                                        CameraPosition(pt, 17f, 0f, 0f),
                                        Animation(Animation.Type.SMOOTH, 0.8f),
                                        null
                                    )

                                    Log.d("MapScreen", "Позиция пользователя: ${loc.latitude}, ${loc.longitude}")
                                } catch (e: Exception) {
                                    Log.e("MapScreen", "Ошибка центрирования на пользователе", e)
                                }

                                // Останавливаем обновления после получения позиции
                                fusedLocationClient.removeLocationUpdates(this)
                            }
                        },
                        context.mainLooper
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.focus_location),
                contentDescription = "Центрировать на мне"
            )
        }
    }
}

/** Вспомогательная функция для конвертации в Bitmap из VectorDrawable */
private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
    return try {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
            ?: error("Drawable not found")
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        Log.e("MapScreen", "Ошибка создания bitmap для drawable $drawableId", e)
        // Fallback: создаем простой цветной битмап
        createBitmap(48, 48).apply {
            eraseColor(0xFF0066CC.toInt())
        }
    }
}