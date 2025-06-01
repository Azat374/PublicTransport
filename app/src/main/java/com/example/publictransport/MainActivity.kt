// MainActivity.kt
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
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.publictransport.auth.AuthState
import com.example.publictransport.auth.AuthViewModel
import com.example.publictransport.auth.LoginScreen
import com.example.publictransport.auth.RegisterScreen
import com.example.publictransport.easyway.EasyWayService
import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader
import com.example.publictransport.screens.MapScreenRoute
import com.example.publictransport.screens.MapScreenWithRoutes
import com.example.publictransport.screens.TransportSelectionScreen
import com.example.publictransport.screens.WelcomeScreen
import com.example.publictransport.ui.RoutePlanningScreen
import com.example.publictransport.ui.TripResponse
import com.example.publictransport.ui.theme.PublicTransportTheme
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Segment(
    val routeName: String,
    val directionIndex: Int,
    val fromStopId: Long,
    val toStopId: Long
)

class MainActivity : ComponentActivity() {
    private val routeListState = mutableStateOf<List<Route>>(emptyList())
    private val stopsState     = mutableStateOf<List<Stop>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация Яндекс.Карт
        MapKitFactory.setApiKey("9fa8f9ff-c36a-4ebd-ad86-57e1d39d6e21")
        MapKitFactory.initialize(this)

        super.onCreate(savedInstanceState)
        checkLocationPermission()

        // FusedLocationProviderClient для карт
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Загрузка локальных маршрутов и остановок (для автозаполнения)
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

                // Получаем ViewModel аутентификации
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()

                // Если пользователь аутентифицирован, показываем основной UI; иначе — вход/регистрация
                if (authState == AuthState.Authenticated) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = (selectedTab == 0),
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
                                    selected = (selectedTab == 1),
                                    onClick = {
                                        selectedTab = 1
                                        navController.navigate("planning") {
                                            popUpTo("planning") { inclusive = true }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Schema, contentDescription = null) },
                                    label = { Text("Проезд") }
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { authViewModel.logout() },
                                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Выйти") },
                                    label = { Text("Выйти") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = "welcome"
                            ) {
                                // Экран приветствия
                                composable("welcome") {
                                    WelcomeScreen(
                                        onContinue = {
                                            navController.navigate("transport") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        }
                                    )
                                }

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
                                        onVariantClick = { trip, from, to ->
                                            handleTripVariantClick(trip, from, to, navController)
                                        }
                                    )
                                }

                                // Карта со статическими маршрутами
                                composable(
                                    "mapWithRoutes/{routeIds}",
                                    arguments = listOf(navArgument("routeIds") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val arg = backStackEntry.arguments!!.getString("routeIds") ?: ""
                                    val ids = arg.split(",").mapNotNull { it.toLongOrNull() }
                                    val sel = routeListState.value.filter { it.id in ids }
                                    MapScreenWithRoutes(
                                        fusedLocationClient = fusedLocationClient,
                                        routes = sel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // Карта EasyWay (polyString и stopsString)
                                composable(
                                    "mapEasyWay/{poly}/{stops}",
                                    arguments = listOf(
                                        navArgument("poly") { type = NavType.StringType },
                                        navArgument("stops") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val polyParam = Uri.decode(backStackEntry.arguments!!.getString("poly")!!)
                                    val stopsParam = Uri.decode(backStackEntry.arguments!!.getString("stops")!!)
                                    MapScreenRoute(
                                        fusedLocationClient = fusedLocationClient,
                                        polyString = polyParam,
                                        stopsString = stopsParam
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Экран входа/регистрации
                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToRegister = {
                                    navController.navigate("register") {
                                        popUpTo("login") { inclusive = false }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = false }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Обработчик клика на вариант EasyWay: строим polyString и stopsString,
     * затем навигируем на экран "mapEasyWay/{poly}/{stops}"
     */
    private fun handleTripVariantClick(
        trip: TripResponse,
        fromStop: Stop,
        toStop: Stop,
        navController: NavController
    ) {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Клик по варианту EasyWay")
                Log.d("MainActivity", "From: ${fromStop.name.ru} (${fromStop.id})")
                Log.d("MainActivity", "To:   ${toStop.name.ru} (${toStop.id})")

                // 1) Запрашиваем compile
                val (sLat, sLng) = fromStop.point.let { it[0] to it[1] }
                val (tLat, tLng) = toStop.point.let { it[0] to it[1] }
                val compileResponse = EasyWayService.compile(
                    startLat       = sLat,
                    startLng       = sLng,
                    stopLat        = tLat,
                    stopLng        = tLng,
                    direct         = false,
                    wayType        = "optimal",
                    transports     = "metro,trol,bus",
                    enableWalkWays = 0
                )
                if (compileResponse.ways.isEmpty()) return@launch
                val firstWay = compileResponse.ways.first()

                // 2) Формируем параметры ids, starts, stops
                val routeBlocks = firstWay.wayDetails.filter { it.type == "route" }
                val idsParam    = routeBlocks.mapNotNull { it.id }.joinToString(",")
                val startsParam = routeBlocks.mapNotNull { it.startPosition }.joinToString(",")
                val stopsParam  = routeBlocks.mapNotNull { it.stopPosition }.joinToString(",")

                // 3) Запрашиваем getCompileRoute
                val compileRouteResponse = EasyWayService.getCompileRoute(
                    ids    = idsParam,
                    starts = startsParam,
                    stops  = stopsParam,
                    a      = "$sLat,$sLng",
                    b      = "$tLat,$tLng"
                )

                // 4) Собираем polyString из compilePoints
                val polyLinesStrings = compileRouteResponse.routesPoints.mapNotNull { rp ->
                    val pts = rp.compilePoints.mapNotNull { cp ->
                        try {
                            val lat = cp.x / 1_000_000.0
                            val lon = cp.y / 1_000_000.0
                            "$lat,$lon"
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (pts.size >= 2) pts.joinToString(";") else null
                }

                // 5) Собираем stopsString из «реальных» compilePoints (cp.i != null)
                val stopsStrings = compileRouteResponse.routesPoints.flatMap { rp ->
                    rp.compilePoints.mapNotNull { cp ->
                        if (cp.i != null) {
                            val lat = cp.x / 1_000_000.0
                            val lon = cp.y / 1_000_000.0
                            "$lat,$lon"
                        } else null
                    }
                }
                val uniqueStopsStrings = stopsStrings.distinct()
                val stopsParamString = uniqueStopsStrings.joinToString(";")

                // 6) Кодируем и навигируем
                val polyParamEncoded  = Uri.encode(polyLinesStrings.joinToString("|"))
                val stopsParamEncoded = Uri.encode(stopsParamString)
                navController.navigate("mapEasyWay/$polyParamEncoded/$stopsParamEncoded")
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при getCompileRoute", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }
}
