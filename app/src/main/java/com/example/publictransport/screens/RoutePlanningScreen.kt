// RoutePlanningScreen.kt
package com.example.publictransport.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.publictransport.easyway.EasyWayService
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.publictransport.easyway.EasyWayClient
import com.example.publictransport.easyway.CompileResponse
import kotlinx.coroutines.launch

/**
 * Модель «остановки на пути» после парсинга wayDetails.
 */
data class StopOnWay(
    val id: Long,
    val name: String
)

/**
 * TripResponse содержит:
 * - номера маршрутов (routeNumbers)
 * - время пеших отрезков до и после
 * - общее расстояние поездки (в метрах)
 * - общее время в пути (в минутах)
 * - список реальных остановок (stopsOnWay)
 */
data class WayForMap(
    val routeIds: List<Long>,           // List of route.id (например [132, 83])
    val startPositions: List<String>,   // List of startPosition (например ["162","804"])
    val stopPositions: List<String>,    // List of stopPosition (например ["274","845"])
    val originLatLng: String,           // "a" — строка вида "43.226869,76.923396"
    val destLatLng: String             // "b" — строка вида "43.220059,76.896944"
)

data class TripResponse(
    // всю оставшуюся информацию мы по-прежнему храним,
    // но добавляем поле wayForMap, которое пойдёт в MainActivity
    val routeNumbers: List<String>,
    val walkBefore: Int,
    val walkAfter: Int,
    val totalRideDistance: Int,
    val wayTimeAuto: Int,
    val stopsOnWay: List<StopOnWay>,

    // **новое**: «параметры для getCompileRoute»
    val wayForMap: WayForMap
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    fusedLocationClient: FusedLocationProviderClient,
    onVariantClick: (TripResponse, Stop, Stop) -> Unit
) {
    val context = LocalContext.current

    // Состояние локальных остановок (для автозаполнения)
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var loadingStops by remember { mutableStateOf(true) }

    // Выбранные «Откуда» и «Куда»
    var fromStop by remember { mutableStateOf<Stop?>(null) }
    var toStop by remember { mutableStateOf<Stop?>(null) }

    // Подсказки и поля ввода
    var fromQuery by remember { mutableStateOf("") }
    var toQuery by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    // Найденные варианты
    var variants by remember { mutableStateOf<List<TripResponse>>(emptyList()) }
    var loadingTrips by remember { mutableStateOf(false) }

    // Сообщение об ошибке
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Анимации
    val loadingRotation by animateFloatAsState(
        targetValue = if (loadingStops) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 1) Загрузка локальных остановок для автозаполнения
    LaunchedEffect(Unit) {
        loadingStops = true
        try {
            withContext(Dispatchers.IO) {
                stops = JsonLoader.loadStops(context)
            }
            if (stops.isEmpty()) {
                errorMsg = "Не удалось загрузить список остановок"
            }
        } catch (e: Exception) {
            errorMsg = "Ошибка при загрузке остановок: ${e.message}"
        } finally {
            loadingStops = false
        }
    }

    // 2) Фильтрация подсказок по полям ввода
    val filteredFromStops = remember(stops, fromQuery) {
        if (fromQuery.isEmpty()) stops.take(8)
        else stops.filter { it.name.ru.contains(fromQuery, ignoreCase = true) }.take(8)
    }
    val filteredToStops = remember(stops, toQuery) {
        if (toQuery.isEmpty()) stops.take(8)
        else stops.filter { it.name.ru.contains(toQuery, ignoreCase = true) }.take(8)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        if (loadingStops) {
            // Современный прелоадер
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(loadingRotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Загрузка остановок...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.width(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Современный заголовок
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Планировщик маршрутов",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            "Найдите оптимальный путь по городу",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Улучшенное поле «Откуда»
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 100)) + fadeIn()
                    ) {
                        ModernSearchableStopField(
                            label = "Откуда",
                            icon = Icons.Default.MyLocation,
                            query = fromQuery,
                            onQueryChange = {
                                fromQuery = it
                                fromExpanded = it.isNotEmpty()
                            },
                            selectedStop = fromStop,
                            expanded = fromExpanded,
                            onExpandedChange = { fromExpanded = it },
                            filteredStops = filteredFromStops,
                            onStopSelected = { stop ->
                                fromStop = stop
                                fromQuery = stop.name.ru
                                fromExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            placeholder = "Выберите начальную точку"
                        )
                    }
                }

                item {
                    // Стильная кнопка «Поменять местами»
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(animationSpec = tween(300, delayMillis = 200))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh)
                            )

                            FloatingActionButton(
                                onClick = {
                                    val tmpStop = fromStop
                                    val tmpQuery = fromQuery
                                    fromStop = toStop
                                    fromQuery = toQuery
                                    toStop = tmpStop
                                    toQuery = tmpQuery
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(scale)
                                    .shadow(8.dp, CircleShape),
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 12.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Поменять местами",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    // Улучшенное поле «Куда»
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 300)) + fadeIn()
                    ) {
                        ModernSearchableStopField(
                            label = "Куда",
                            icon = Icons.Default.Place,
                            query = toQuery,
                            onQueryChange = {
                                toQuery = it
                                toExpanded = it.isNotEmpty()
                            },
                            selectedStop = toStop,
                            expanded = toExpanded,
                            onExpandedChange = { toExpanded = it },
                            filteredStops = filteredToStops,
                            onStopSelected = { stop ->
                                toStop = stop
                                toQuery = stop.name.ru
                                toExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            placeholder = "Выберите конечную точку"
                        )
                    }
                }

                item {
                    // Современное сообщение об ошибке
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        errorMsg?.let { message ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Ошибка",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Произошла ошибка",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = message,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Стильная кнопка «Найти маршрут»
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(animationSpec = tween(300, delayMillis = 400)) + fadeIn()
                    ) {
                        Button(
                            onClick = {
                                errorMsg = null
                                variants = emptyList()
                                if (fromStop != null && toStop != null) {
                                    loadingTrips = true
                                    scope.launch {
                                        try {
                                            // 1) Собираем координаты
                                            val sLat = fromStop!!.point[0]
                                            val sLng = fromStop!!.point[1]
                                            val tLat = toStop!!.point[0]
                                            val tLng = toStop!!.point[1]

                                            Log.d("RoutePlanningScreen", "EasyWay: $sLat,$sLng → $tLat,$tLng")

                                            // 2) Вызываем OkHttp-сервис EasyWayService.compile(...)
                                            val compileResponse = EasyWayService.compile(
                                                startLat = sLat,
                                                startLng = sLng,
                                                stopLat = tLat,
                                                stopLng = tLng,
                                                direct = false,
                                                wayType = "optimal",
                                                transports = "metro,trol,bus",
                                                enableWalkWays = 0
                                            )

                                            if (compileResponse.ways.isEmpty()) {
                                                errorMsg = "Не найдено ни одного варианта маршрута"
                                            } else {
                                                // Для каждого way из compileResponse.ways собираем TripResponse
                                                val allTrips: List<TripResponse> = compileResponse.ways.map { way ->
                                                    // 2.1. Номера маршрутов (например ["205", "99"])
                                                    val routeNumbers = way.routes.map { it.routeNumber }

                                                    // 2.2. Время пешком до первой остановки (type="first")
                                                    val walkBefore = way.wayDetails
                                                        .firstOrNull { it.type == "first" }
                                                        ?.time
                                                        ?: 0

                                                    // 2.3. Время пешком после последней остановки (type="last")
                                                    val walkAfter = way.wayDetails
                                                        .firstOrNull { it.type == "last" }
                                                        ?.time
                                                        ?: 0

                                                    // 2.4. Общее расстояние по всем «route»-деталям (length в метрах)
                                                    val totalRideDistance = way.wayDetails
                                                        .filter { it.type == "route" }
                                                        .sumOf { it.length }

                                                    // 2.5. Общее время в пути (wayTimeAuto)
                                                    val wayTimeAuto = way.wayTimeAuto

                                                    // 2.6. Список реальных остановок из wayDetails
                                                    val allStopsOnWay: List<StopOnWay> = way.wayDetails.flatMap { detail ->
                                                        when (detail.type) {
                                                            "first" -> {
                                                                val id = detail.stopId?.toLongOrNull() ?: return@flatMap emptyList()
                                                                val name = detail.stop ?: return@flatMap emptyList()
                                                                listOf(StopOnWay(id, name))
                                                            }
                                                            "route" -> {
                                                                val beginId = detail.stopBeginId?.toLongOrNull() ?: return@flatMap emptyList()
                                                                val beginName = detail.stopBegin ?: return@flatMap emptyList()
                                                                val endId = detail.stopEndId?.toLongOrNull() ?: return@flatMap emptyList()
                                                                val endName = detail.stopEnd ?: return@flatMap emptyList()
                                                                listOf(
                                                                    StopOnWay(beginId, beginName),
                                                                    StopOnWay(endId,   endName)
                                                                )
                                                            }
                                                            "last" -> {
                                                                val id = detail.stopId?.toLongOrNull() ?: return@flatMap emptyList()
                                                                val name = detail.stop ?: return@flatMap emptyList()
                                                                listOf(StopOnWay(id, name))
                                                            }
                                                            else -> emptyList()
                                                        }
                                                    }
                                                    val stopsOnWay = allStopsOnWay.distinctBy { it.id }

                                                    // 2.7. Подготовка параметров для вызова getCompileRoute()
                                                    val routeIds       = way.routes.map { it.id.toLongOrNull() ?: 0L }
                                                    val startPositions = way.routes.map { it.startPoint.position }
                                                    val stopPositions  = way.routes.map { it.stopPoint.position }

                                                    // 2.8. Строки «a» и «b» – координаты «откуда» и «куда»
                                                    val a = "${fromStop!!.point[0]},${fromStop!!.point[1]}"
                                                    val b = "${toStop!!.point[0]},${toStop!!.point[1]}"

                                                    val wayForMap = WayForMap(
                                                        routeIds = routeIds,
                                                        startPositions = startPositions,
                                                        stopPositions  = stopPositions,
                                                        originLatLng    = a,
                                                        destLatLng      = b
                                                    )

                                                    TripResponse(
                                                        routeNumbers       = routeNumbers,
                                                        walkBefore         = walkBefore,
                                                        walkAfter          = walkAfter,
                                                        totalRideDistance  = totalRideDistance,
                                                        wayTimeAuto        = wayTimeAuto,
                                                        stopsOnWay         = stopsOnWay,
                                                        wayForMap          = wayForMap
                                                    )
                                                }

                                                variants = allTrips
                                            }
                                        } catch (e: Exception) {
                                            errorMsg = "Ошибка при поиске маршрута: ${e.message}"
                                            Log.e("RoutePlanningScreen", errorMsg!!)
                                        } finally {
                                            loadingTrips = false
                                        }
                                    }
                                } else {
                                    errorMsg = "Пожалуйста, выберите точки отправления и назначения"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            ),
                            enabled = !loadingTrips && fromStop != null && toStop != null
                        ) {
                            if (loadingTrips) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Поиск маршрутов...",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Найти маршрут",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Найти маршрут",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 3) Отображаем все варианты с анимацией
                items(variants.size) { index ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            animationSpec = tween(300, delayMillis = index * 100)
                        ) + fadeIn()
                    ) {
                        ModernRouteVariantCard(
                            trip = variants[index],
                            onClick = {
                                Log.d("RoutePlanningScreen", "Клик по варианту EasyWay")
                                onVariantClick(variants[index], fromStop!!, toStop!!)
                            }
                        )
                    }
                }

                // Показываем пустое состояние, если нет вариантов
                if (variants.isEmpty() && !loadingTrips && fromStop != null && toStop != null) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn()
                        ) {
                            EmptyStateCard()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSearchableStopField(
    label: String,
    icon: ImageVector,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStop: Stop?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    filteredStops: List<Stop>,
    onStopSelected: (Stop) -> Unit,
    containerColor: Color,
    placeholder: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true,
                placeholder = {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )
        }

        AnimatedVisibility(
            visible = expanded && filteredStops.isNotEmpty(),
            enter = slideInVertically() + fadeIn() + expandVertically(),
            exit = slideOutVertically() + fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredStops) { stop ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStopSelected(stop) }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = stop.name.ru,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (stop.name.kk.isNotEmpty() && stop.name.kk != stop.name.ru) {
                                        Text(
                                            text = stop.name.kk,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ModernRouteVariantCard(
    trip: TripResponse,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Время и расстояние
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Время",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = "${trip.wayTimeAuto} мин",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "В пути",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Расстояние",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    val dist = trip.totalRideDistance
                    val km = dist / 1000
                    val metres = dist % 1000
                    Text(
                        text = if (km > 0) {
                            "$km.${"$metres".padStart(3, '0').take(1)} км"
                        } else {
                            "$metres м"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // 2. Маршруты
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Маршруты:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                trip.routeNumbers.forEachIndexed { index, routeNum ->
                    if (index > 0) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = routeNum,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 3. Пешие участки
            if (trip.walkBefore > 0 || trip.walkAfter > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (trip.walkBefore > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${trip.walkBefore} мин до первой остановки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (trip.walkAfter > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${trip.walkAfter} мин от последней остановки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 4. Список остановок (если есть)
            if (trip.stopsOnWay.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Остановки:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    trip.stopsOnWay.forEach { s ->
                        Text(
                            text = "• ${s.name} (ID=${s.id})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Подсказка «Нажмите для просмотра на карте»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Нажмите для просмотра на карте",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Варианты не найдены",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Попробуйте выбрать другие остановки",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
