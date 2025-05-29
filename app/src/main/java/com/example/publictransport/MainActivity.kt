package com.example.publictransport

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.publictransport.dgis.Point
import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader
import com.example.publictransport.ui.MapScreenWithRoutes
import com.example.publictransport.ui.RoutePlanningScreen
import com.example.publictransport.ui.TransportSelectionScreen
import com.example.publictransport.ui.TripResponse
import com.example.publictransport.ui.theme.PublicTransportTheme
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val routeListState = mutableStateOf<List<Route>>(emptyList())
    private val stopsState     = mutableStateOf<List<Stop>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация Яндекс.Карт
        MapKitFactory.setApiKey("9fa8f9ff-c36a-4ebd-ad86-57e1d39d6e21")
        MapKitFactory.initialize(this)

        super.onCreate(savedInstanceState)
        checkLocationPermission()

        // FusedLocationProvider для карты
        val fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)

        // Загрузка локальных данных
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val routes = JsonLoader.loadRoutes(this@MainActivity)
                    val stops  = JsonLoader.loadStops(this@MainActivity)
                    withContext(Dispatchers.Main) {
                        routeListState.value = routes
                        stopsState.value     = stops
                        Log.d("MainActivity", "Загружено ${routes.size} маршрутов и ${stops.size} остановок")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка загрузки данных", e)
                routeListState.value = emptyList()
                stopsState.value     = emptyList()
            }
        }

        setContent {
            PublicTransportTheme {
                val navController = rememberNavController()
                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    navController.navigate("transport") {
                                        popUpTo("transport") { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null) },
                                label = { Text("Транспорт") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    navController.navigate("planning") {
                                        popUpTo("planning") { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Default.Schema, contentDescription = null) },
                                label = { Text("Проезд") }
                            )
                        }
                    }
                ) { inner ->
                    Box(Modifier.padding(inner)) {
                        NavHost(
                            navController = navController,
                            startDestination = "transport"
                        ) {
                            // Список статических маршрутов
                            composable("transport") {
                                LaunchedEffect(Unit) { selectedTab = 0 }
                                TransportSelectionScreen(
                                    routeList = routeListState.value,
                                    onNavigateToMap = { selected ->
                                        val ids = selected.joinToString(",") { it.id.toString() }
                                        navController.navigate("mapWithRoutes/$ids")
                                    }
                                )
                            }

                            // Планировщик маршрутов
                            composable("planning") {
                                LaunchedEffect(Unit) { selectedTab = 1 }
                                RoutePlanningScreen(
                                    fusedLocationClient = fusedLocationClient,
                                    onVariantClick = { trip ->
                                        handleTripVariantClick(trip, navController)
                                    }
                                )
                            }

                            // Карта со статическими маршрутами
                            composable(
                                "mapWithRoutes/{routeIds}",
                                arguments = listOf(navArgument("routeIds") { type = NavType.StringType })
                            ) { back ->
                                val arg = back.arguments!!.getString("routeIds") ?: ""
                                val ids = arg.split(",").mapNotNull { it.toLongOrNull() }
                                val sel = routeListState.value.filter { it.id in ids }
                                MapScreenWithRoutes(
                                    fusedLocationClient = fusedLocationClient,
                                    routes = sel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // Карта с общественным транспортом
                            composable(
                                "mapTrip/{coords}",
                                arguments = listOf(navArgument("coords") { type = NavType.StringType })
                            ) { back ->
                                val enc = back.arguments!!.getString("coords") ?: ""
                                val pts = parseRoutePoints(Uri.decode(enc))
                                MapScreen(
                                    fusedLocationClient = fusedLocationClient,
                                    routePoints = pts
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Обрабатываем клик — сразу берём все точки из TripResponse.paths.stops */
    private fun handleTripVariantClick(
        trip: TripResponse,
        navController: androidx.navigation.NavController
    ) {
        try {
            Log.d("MainActivity", "Обработка клика по варианту маршрута")
            // 1) Собираем все Point, удаляем дубли
            val pts = trip.paths
                .flatMap { it.stops }
                .distinctBy { it.lat to it.lon }

            if (pts.isEmpty()) {
                Log.e("MainActivity", "Нет точек для отображения")
                return
            }

            // 2) Ограничиваем до 50, сериализуем
            val limited = pts.take(50)
            val serialized = limited.joinToString("|") { "${it.lat};${it.lon}" }

            // 3) URL-энкод и переход
            val encoded = Uri.encode(serialized)
            navController.navigate("mapTrip/$encoded")

            Log.d("MainActivity", "Навигация к карте с ${limited.size} точками")
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при обработке клика", e)
        }
    }

    /** Парсим строку "lat;lon|lat;lon…" обратно в List<Point> */
    private fun parseRoutePoints(s: String): List<Point> = runCatching {
        s.split("|").mapNotNull { part ->
            part.split(";").takeIf { it.size == 2 }?.let {
                val lat = it[0].toDoubleOrNull()
                val lon = it[1].toDoubleOrNull()
                if (lat != null && lon != null) Point(lat, lon) else null
            }
        }
    }.getOrDefault(emptyList())

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }
    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }
}
