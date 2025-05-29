
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
import com.example.publictransport.model.Stop
import com.example.publictransport.network.JsonLoader

import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import com.example.publictransport.dgis.TwoGisRouteService

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

    val scope = rememberCoroutineScope()

    // Загрузка остановок
    LaunchedEffect(Unit) {
        loadingStops = true
        try {
            stops = JsonLoader.loadStops(context)
            if (stops.isEmpty()) {
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

                                        Log.d("RoutePlanningScreen", "Поиск маршрута: $sLat,$sLong -> $eLat,$eLong")

                                        variants = TwoGisRouteService.fetchRoutes(
                                            fromLat = sLat, fromLon = sLong,
                                            toLat = eLat, toLon = eLong,
                                            modes = listOf("bus", "tram", "trolleybus")
                                        )

                                        Log.d("RoutePlanningScreen", "Найдено ${variants.size} вариантов")
                                    } catch (e: Exception) {
                                        Log.e("RoutePlanningScreen", "Ошибка поиска", e)
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

                // Результаты
                items(variants) { trip ->
                    RouteVariantCard(
                        trip = trip,
                        onClick = {
                            Log.d("RoutePlanningScreen", "Клик по варианту маршрута")

                            onVariantClick(trip)
                        }
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
private fun RouteVariantCard(
    trip: TripResponse,
    onClick: () -> Unit = {} // Добавляем параметр onClick
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Добавляем обработчик клика
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
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
                        val (h, m, _) = trip.paths.first().rideDuration.split(":")
                        val totalMins = h.toInt() * 60 + m.toInt()
                        Text(
                            text = "$totalMins мин",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Время в пути",
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
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    val dist = trip.paths.first().rideDistance
                    val km = dist / 1000
                    val metres = dist % 1000
                    Text(
                        text = if (km > 0) "$km.${"$metres".padStart(3, '0').take(1)} км" else "$metres м",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
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
                                text = path.routeId.toString(),
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

            // Визуальная подсказка для клика
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
