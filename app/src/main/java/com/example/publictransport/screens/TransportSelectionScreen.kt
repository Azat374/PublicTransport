// TransportSelectionScreen.kt
package com.example.publictransport.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Tram
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.publictransport.model.Route

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransportSelectionScreen(
    routeList: List<Route>,
    onNavigateToMap: (List<Route>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(0) }       // 0 = bus, 1 = trolleybus, 2 = metro
    var selectedRoutes by remember { mutableStateOf(listOf<Route>()) }
    var showScheme by remember { mutableStateOf(true) }

    // Отфильтрованный список по типу и вводу
    val filteredRoutes = routeList
        .filter { it.typeId == selectedType }
        .filter { it.name.ru.contains(searchText, ignoreCase = true) }
        .sortedBy { it.name.ru.toIntOrNull() ?: Int.MAX_VALUE }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1) Поиск + чипы выбранных (до 5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
                .padding(4.dp)
        ) {
            selectedRoutes.take(5).forEach { route ->
                InputChip(
                    selected = true,
                    onClick = { /* детали */ },
                    label = { Text(route.name.ru) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (route.typeId) {
                                0 -> Icons.Default.DirectionsBus
                                1 -> Icons.Default.Tram
                                else -> Icons.Default.DirectionsSubway
                            },
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Удалить",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    selectedRoutes = selectedRoutes - route
                                }
                        )
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            if (selectedRoutes.size < 5) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Найти номер") },
                    singleLine = true,
                    modifier = Modifier
                        .height(40.dp)
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2) Фильтр-чипы по виду транспорта
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TransportFilterChip("Автобусы", Icons.Default.DirectionsBus, selectedType == 0) { selectedType = 0 }
            TransportFilterChip("Троллейбусы", Icons.Default.Tram, selectedType == 1) { selectedType = 1 }
            TransportFilterChip("Метро", Icons.Default.DirectionsSubway, selectedType == 2) { selectedType = 2 }
        }

        Spacer(Modifier.height(12.dp))

        // 3) Сетка всех маршрутов по фильтру
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(filteredRoutes) { route ->
                val isSelected = route in selectedRoutes

                // Красивые кнопки маршрутов с градиентом и улучшенным дизайном
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
                            selectedRoutes = if (isSelected) selectedRoutes - route
                            else selectedRoutes + route
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 8.dp else 2.dp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected)
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = route.name.ru,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4) Красивые кнопки Схема/Карта
        if (selectedRoutes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопка Схема
                ElevatedButton(
                    onClick = { showScheme = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (showScheme)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showScheme)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = if (showScheme) 8.dp else 4.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schema,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Схема",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Кнопка Карта
                ElevatedButton(
                    onClick = {
                        showScheme = false
                        onNavigateToMap(selectedRoutes)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (!showScheme)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!showScheme)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = if (!showScheme) 8.dp else 4.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Карта",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TransportFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        elevation = FilterChipDefaults.elevatedFilterChipElevation(
            elevation = if (selected) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    )
}