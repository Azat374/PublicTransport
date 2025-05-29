// RoutePlanningScreen.kt
package com.example.publictransport.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.publictransport.dgis.Point
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader

import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import com.example.publictransport.dgis.TwoGisRouteService
import com.example.publictransport.model.Route
import kotlin.math.absoluteValue
import java.lang.Math.toRadians
import kotlin.math.*
// Модели ответа
data class TripResponse(
    val startWalkDistance: Int,
    val startWalkDuration: String,
    val endWalkDistance: Int,
    val endWalkDuration: String,
    val paths: List<PathResponse>
)
data class PathResponse(
    val routeId: Long,
    val name: String,
    val directionIndex: Int,
    val walkDistance: Int,
    val walkDuration: String,
    val rideDistance: Int,
    val rideDuration: String,
    val stops: List<com.example.publictransport.dgis.Point>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    fusedLocationClient: FusedLocationProviderClient,
    onVariantClick: (TripResponse) -> Unit
) {
    val context = LocalContext.current

    // Состояние
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var loadingStops by remember { mutableStateOf(true) }
    var fromStop by remember { mutableStateOf<Stop?>(null) }
    var toStop by remember { mutableStateOf<Stop?>(null) }
    var fromQuery by remember { mutableStateOf("") }
    var toQuery by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var variants by remember { mutableStateOf<List<TripResponse>>(emptyList()) }
    var loadingTrips by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Загрузка остановок
    LaunchedEffect(Unit) {
        loadingStops = true
        try {
            stops = JsonLoader.loadStops(context)
            routes = JsonLoader.loadRoutes(context)
            if (stops.isEmpty() || routes.isEmpty()) {
                errorMsg = "Не удалось загрузить остановки"
            }
        } catch (e: Exception) {
            errorMsg = "Ошибка при загрузке остановок: ${e.message}"
        } finally {
            loadingStops = false
        }
    }

    // Фильтрация остановок для поиска
    val filteredFromStops = remember(stops, fromQuery) {
        if (fromQuery.isEmpty()) stops.take(10)
        else stops.filter { it.name.ru.contains(fromQuery, ignoreCase = true) }.take(10)
    }

    val filteredToStops = remember(stops, toQuery) {
        if (toQuery.isEmpty()) stops.take(10)
        else stops.filter { it.name.ru.contains(toQuery, ignoreCase = true) }.take(10)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        if (loadingStops) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Загрузка остановок...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    // Заголовок
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Планировщик маршрутов",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Найдите оптимальный путь",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                item {
                    // Выбор начальной точки
                    SearchableStopField(
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
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                item {
                    // Кнопка смены местами
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val tempStop = fromStop
                                val tempQuery = fromQuery
                                fromStop = toStop
                                fromQuery = toQuery
                                toStop = tempStop
                                toQuery = tempQuery
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondary,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Поменять местами",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }

                item {
                    // Выбор конечной точки
                    SearchableStopField(
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
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }

                item {
                    // Сообщение об ошибке
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Кнопка поиска
                    Button(
                        onClick = {
                            errorMsg = null
                            variants = emptyList()
                            if (fromStop != null && toStop != null) {
                                loadingTrips = true
                                scope.launch {
                                    try {
                                        val sLat = fromStop!!.point[0]
                                        val sLong = fromStop!!.point[1]
                                        val eLat = toStop!!.point[0]
                                        val eLong = toStop!!.point[1]

                                        variants = if (fromStop != null && toStop != null) {
                                            planLocalTripsByName(
                                                fromQuery    = fromStop!!.name.ru,
                                                toQuery      = toStop!!.name.ru,
                                                allRoutes = JsonLoader.loadRoutes(context), // из JsonLoader.loadRoutes(...)
                                                allStops  = JsonLoader.loadStops(context)
                                            )
                                        } else emptyList()
                                    } catch (e: Exception) {
                                        errorMsg = "Ошибка при поиске маршрутов: ${e.message}"
                                    } finally {
                                        loadingTrips = false
                                    }
                                }
                            } else {
                                errorMsg = "Выберите обе точки"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(8.dp),
                        enabled = !loadingTrips
                    ) {
                        if (loadingTrips) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Поиск...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Найти маршрут",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Результаты: делаем карточки кликабельными
                items(variants) { trip ->
                    RouteVariantCard(trip, Modifier
                        .fillMaxWidth()
                        .clickable { onVariantClick(trip) }    // <- передаём только trip
                        .padding(vertical = 8.dp)
                    )
                }

                if (variants.isEmpty() && !loadingTrips && fromStop != null && toStop != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schema,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Маршруты не найдены",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Попробуйте выбрать другие остановки",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableStopField(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStop: Stop?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    filteredStops: List<Stop>,
    onStopSelected: (Stop) -> Unit,
    containerColor: Color
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true,
                placeholder = { Text("Введите название остановки") }
            )
        }

        // Выпадающий список
        AnimatedVisibility(
            visible = expanded && filteredStops.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(filteredStops) { stop ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = stop.name.ru,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (stop.name.kk.isNotEmpty() && stop.name.kk != stop.name.ru) {
                                        Text(
                                            text = stop.name.kk,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            },
                            onClick = { onStopSelected(stop) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RouteSegmentChip(
    icon: ImageVector,
    text: String,
    description: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
private fun RouteVariantCard(trip: TripResponse, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок с временем и расстоянием
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Время
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        // Парсим hh:mm:ss
                        val parts = trip.paths.first().rideDuration.split(":").map { it.toInt() }
                        val hours = parts.getOrNull(0) ?: 0
                        val mins  = parts.getOrNull(1) ?: 0
                        val durationText = if (hours > 0) {
                            "${hours} ч ${mins} мин"
                        } else {
                            "${mins} мин"
                        }
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Время в пути",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Расстояние
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    val dist = trip.paths.first().rideDistance
                    val distText = if (dist >= 1000) {
                        // 1 знак после запятой
                        String.format("%.1f км", dist / 1000f)
                    } else {
                        "$dist м"
                    }
                    Text(
                        text = distText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Маршруты
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Маршруты:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                trip.paths.forEachIndexed { index, path ->
                    if (index > 0) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = path.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Информация о ходьбе
            if (trip.startWalkDistance > 0 || trip.endWalkDistance > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (trip.startWalkDistance > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${trip.startWalkDistance}м до остановки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (trip.endWalkDistance > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${trip.endWalkDistance}м от остановки",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1. Добавляем локальную функцию-планировщик
private const val CLOSE_THRESHOLD_METERS = 200.0

/**
 * Ищет по названиям, возвращает список возможных маршрутов (включая до 1 пересадки).
 * Если точки ближе CLOSE_THRESHOLD_METERS — возвращает единственный пеший вариант.
 *
 * @return List<TripResponse>
 */
fun planLocalTripsByName(
    fromQuery: String,
    toQuery: String,
    allRoutes: List<com.example.publictransport.model.Route>,
    allStops: List<com.example.publictransport.model.Stop>
): List<TripResponse> {
    // 1) Найдём ВСЕ остановки, чьё имя содержит подстроку
    val fromCandidates = allStops.filter {
        it.name.ru.contains(fromQuery, ignoreCase = true) ||
                it.name.en.contains(fromQuery, ignoreCase = true)
    }
    val toCandidates = allStops.filter {
        it.name.ru.contains(toQuery, ignoreCase = true) ||
                it.name.en.contains(toQuery, ignoreCase = true)
    }

    // 2) Построим индексы по ID -> Stop, а также по маршруту->список направлений
    val stopById = allStops.associateBy { it.id }

    val results = mutableListOf<TripResponse>()

    // для каждой пары кандидатов от/до
    fromCandidates.forEach { fromStop ->
        toCandidates.forEach { toStop ->
            // 3) если очень близко — пешком
            val d = haversine(
                fromStop.point[0], fromStop.point[1],
                toStop.point[0],   toStop.point[1]
            )
            if (d <= CLOSE_THRESHOLD_METERS) {
                val sec = (d / 1.4).roundToInt()
                results += TripResponse(
                    startWalkDistance = d.roundToInt(),
                    startWalkDuration = formatSec(sec),
                    endWalkDistance   = 0,
                    endWalkDuration   = "00:00:00",
                    paths = listOf(PathResponse(
                        routeId        = 0L,
                        name           = "пешком",
                        directionIndex = 0,
                        walkDistance   = d.roundToInt(),
                        walkDuration   = formatSec(sec),
                        rideDistance   = 0,
                        rideDuration   = "00:00:00",
                        stops          = listOf(
                            com.example.publictransport.dgis.Point(fromStop.point[0], fromStop.point[1]),
                            com.example.publictransport.dgis.Point(toStop.point[0],   toStop.point[1])
                        )
                    ))
                )
                return@forEach
            }

            // 4) сначала пробуем без пересадок
            allRoutes.forEach { route ->
                route.directions.forEach { dir ->
                    val stopIds = dir.stops.map { it.stopId }
                    val iFrom = stopIds.indexOf(fromStop.id)
                    val iTo   = stopIds.indexOf(toStop.id)
                    if (iFrom>=0 && iTo>=0 && iFrom!=iTo) {
                        val range = if (iFrom < iTo) iFrom..iTo else iFrom downTo iTo
                        val pts = range.mapNotNull { idx ->
                            stopById[ stopIds[idx] ]?.point?.let { coords->
                                com.example.publictransport.dgis.Point(coords[0], coords[1])
                            }
                        }
                        if (pts.size>=2) {
                            val dist = abs(
                                dir.stops[iTo].offsetDistance - dir.stops[iFrom].offsetDistance
                            ).roundToInt()
                            results += TripResponse(
                                startWalkDistance = 0,
                                startWalkDuration = "00:00:00",
                                endWalkDistance   = 0,
                                endWalkDuration   = "00:00:00",
                                paths = listOf(PathResponse(
                                    routeId        = route.id,
                                    name           = route.name.ru,
                                    directionIndex = dir.index,
                                    walkDistance   = 0,
                                    walkDuration   = "00:00:00",
                                    rideDistance   = dist,
                                    rideDuration   = "00:00:00",
                                    stops          = pts
                                ))
                            )
                        }
                    }
                }
            }

            // 5) если прямых нет — один transfer
            //    находим все маршруты, где есть fromStop, и где есть toStop
            val fromRoutes = allRoutes.flatMap { r-> r.directions.map{ r to it } }
                .filter { (_, dir) -> dir.stops.any{ it.stopId==fromStop.id } }
            val toRoutes   = allRoutes.flatMap { r-> r.directions.map{ r to it } }
                .filter { (_, dir) -> dir.stops.any{ it.stopId==toStop.id   } }

            fromRoutes.forEach { (rA, dA) ->
                toRoutes.forEach { (rB, dB) ->
                    // ищем точки пересадки: общие stopId
                    val sA = dA.stops.map{ it.stopId }.toSet()
                    val sB = dB.stops.map{ it.stopId }.toSet()
                    val common = sA.intersect(sB)
                    common.forEach { transferId ->
                        val iA = dA.stops.indexOfFirst{ it.stopId==transferId }
                        val iB = dB.stops.indexOfFirst{ it.stopId==transferId }
                        val iFrom = dA.stops.indexOfFirst{ it.stopId==fromStop.id }
                        val iTo   = dB.stops.indexOfFirst{ it.stopId==toStop.id   }
                        if (iA>=0 && iFrom>=0 && iB>=0 && iTo>=0) {
                            // сегмент A: от fromStop до transfer
                            val segA = (if(iFrom< iA) iFrom..iA else iFrom downTo iA)
                            val ptsA = segA.mapNotNull { idx ->
                                stopById[dA.stops[idx].stopId]?.point?.let{ p->
                                    com.example.publictransport.dgis.Point(p[0],p[1])
                                }
                            }
                            // сегмент B: от transfer до toStop
                            val segB = (if(iB< iTo) iB..iTo else iB downTo iTo)
                            val ptsB = segB.mapNotNull { idx ->
                                stopById[dB.stops[idx].stopId]?.point?.let{ p->
                                    com.example.publictransport.dgis.Point(p[0],p[1])
                                }
                            }
                            if (ptsA.size>=2 && ptsB.size>=2) {
                                val distA = abs(dA.stops[iA].offsetDistance - dA.stops[iFrom].offsetDistance).roundToInt()
                                val distB = abs(dB.stops[iTo].offsetDistance - dB.stops[iB].offsetDistance).roundToInt()
                                results += TripResponse(
                                    startWalkDistance = 0,
                                    startWalkDuration = "00:00:00",
                                    endWalkDistance   = 0,
                                    endWalkDuration   = "00:00:00",
                                    paths = listOf(
                                        PathResponse(
                                            routeId        = rA.id,
                                            name           = rA.name.ru,
                                            directionIndex = dA.index,
                                            walkDistance   = 0, walkDuration="00:00:00",
                                            rideDistance   = distA, rideDuration="00:00:00",
                                            stops          = ptsA
                                        ),
                                        PathResponse(
                                            routeId        = rB.id,
                                            name           = rB.name.ru,
                                            directionIndex = dB.index,
                                            walkDistance   = 0, walkDuration="00:00:00",
                                            rideDistance   = distB, rideDuration="00:00:00",
                                            stops          = ptsB
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    return results
}


// Haversine
private fun haversine(lat1:Double, lon1:Double, lat2:Double, lon2:Double):Double {
    val R=6371000.0
    val dLat=toRadians(lat2-lat1)
    val dLon=toRadians(lon2-lon1)
    val a=sin(dLat/2).pow(2.0)+cos(toRadians(lat1))*cos(toRadians(lat2))*sin(dLon/2).pow(2.0)
    return R*2*atan2(sqrt(a), sqrt(1-a))
}

// seconds -> HH:MM:SS
private fun formatSec(sec:Int):String {
    val h= sec/3600
    val m=(sec%3600)/60
    val s= sec%60
    return "%02d:%02d:%02d".format(h,m,s)}
