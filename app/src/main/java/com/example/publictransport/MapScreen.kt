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
    var routeLine by remember { mutableStateOf<PolylineMapObject?>(null) }

    // convert your DTOs into Yandex Points once
    val yandexPoints = remember(routePoints) {
        routePoints.map { dto ->
            com.yandex.mapkit.geometry.Point(dto.lat, dto.lon)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val mv = MapView(ctx).apply {
                // center on first point
                if (yandexPoints.isNotEmpty()) {
                    map.move(
                        CameraPosition(yandexPoints.first(), 14f, 0f, 0f),
                        Animation(Animation.Type.SMOOTH, 1f),
                        null
                    )
                }
            }
            mapView = mv

            // draw the polyline
            if (yandexPoints.size >= 2) {
                routeLine?.let { mv.map.mapObjects.remove(it) }
                val polyObj = mv.map.mapObjects.addPolyline(Polyline(yandexPoints))
                polyObj.setStrokeColor(0xFF0066CC.toInt())
                polyObj.setStrokeWidth(5f)
                routeLine = polyObj
            }

            // draw placemarks
            yandexPoints.forEach { pt ->
                try {
                    mv.map.mapObjects.addPlacemark(
                        pt,
                        ImageProvider.fromBitmap(getBitmapFromVectorDrawable(ctx, R.drawable.bus_stop))
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback: создаем простую метку без иконки
                    mv.map.mapObjects.addPlacemark(pt)
                }
            }

            mv
        }, modifier = Modifier.fillMaxSize())

        // "center on me" button
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
                                    userPlacemark?.let { mv.map.mapObjects.remove(it) }
                                    val bmp = getBitmapFromVectorDrawable(context, R.drawable.ic_user_location)
                                    val img = ImageProvider.fromBitmap(bmp)
                                    userPlacemark = mv.map.mapObjects.addPlacemark(pt, img)

                                    mv.map.move(
                                        CameraPosition(pt, 17f, 0f, 0f),
                                        Animation(Animation.Type.SMOOTH, 0.8f),
                                        null
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
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
        e.printStackTrace()
        // Fallback: создаем простой цветной битмап
        createBitmap(48, 48).apply {
            eraseColor(0xFF0066CC.toInt())
        }
    }
}