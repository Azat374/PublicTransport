// TransportSelectionScreen.kt
package com.example.publictransport.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.publictransport.model.Route
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransportSelectionScreen(
    routeList: List<Route>,
    onNavigateToMap: (List<Route>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(0) }
    var selectedRoutes by remember { mutableStateOf(listOf<Route>()) }
    var showScheme by remember { mutableStateOf(true) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current

    // Filtered routes with improved sorting
    val filteredRoutes = remember(routeList, selectedType, searchText) {
        routeList
            .filter { it.typeId == selectedType }
            .filter {
                if (searchText.isBlank()) true
                else it.name.ru.contains(searchText, ignoreCase = true)
            }
            .sortedWith(compareBy<Route> { route ->
                route.name.ru.toIntOrNull() ?: Int.MAX_VALUE
            }.thenBy { it.name.ru })
    }

    // Transport type data
    val transportTypes = remember {
        listOf(
            TransportType("Автобусы", Icons.Default.DirectionsBus, Icons.Outlined.DirectionsBus, Color(0xFF2196F3)),
            TransportType("Троллейбусы", Icons.Default.Tram, Icons.Outlined.Tram, Color(0xFF4CAF50)),
            TransportType("Метро", Icons.Default.DirectionsSubway, Icons.Outlined.DirectionsSubway, Color(0xFFFF9800))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (isSearchFocused) 8.dp else 4.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and selected routes section
                HeaderSection(
                    selectedRoutes = selectedRoutes,
                    onRemoveRoute = { route ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedRoutes = selectedRoutes - route
                    }
                )

                // Enhanced search field
                EnhancedSearchField(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onFocusChange = { isSearchFocused = it },
                    isFocused = isSearchFocused
                )

                // Transport type filter chips
                TransportTypeSelector(
                    transportTypes = transportTypes,
                    selectedType = selectedType,
                    onTypeSelected = { newType ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedType = newType
                        searchText = "" // Clear search when changing type
                    }
                )
            }
        }

        // Routes grid with enhanced animations
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = filteredRoutes,
                key = { it.id }
            ) { route ->
                val isSelected = route in selectedRoutes
                val transportColor = transportTypes[selectedType].color

                EnhancedRouteCard(
                    route = route,
                    isSelected = isSelected,
                    transportColor = transportColor,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedRoutes = if (isSelected) {
                            selectedRoutes - route
                        } else {
                            selectedRoutes + route
                        }
                    }
                )
            }
        }

        // Bottom action section
        AnimatedVisibility(
            visible = selectedRoutes.isNotEmpty(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut()
        ) {
            BottomActionSection(
                selectedRoutes = selectedRoutes,
                showScheme = showScheme,
                onSchemeToggle = { showScheme = it },
                onNavigateToMap = { onNavigateToMap(selectedRoutes) }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    selectedRoutes: List<Route>,
    onRemoveRoute: (Route) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Выберите маршруты",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (selectedRoutes.isNotEmpty()) {
            Text(
                text = "Выбрано: ${selectedRoutes.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(selectedRoutes.take(5)) { route ->
                    SelectedRouteChip(
                        route = route,
                        onRemove = { onRemoveRoute(route) }
                    )
                }
                if (selectedRoutes.size > 5) {
                    item {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "+${selectedRoutes.size - 5}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedSearchField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    isFocused: Boolean
) {
    val borderColor by animateColorAsState(
        targetValue = if (isFocused)
            MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200)
    )

    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        placeholder = {
            Text(
                text = "Поиск по номеру маршрута...",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchTextChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Очистить",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun TransportTypeSelector(
    transportTypes: List<TransportType>,
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        transportTypes.forEachIndexed { index, type ->
            val isSelected = selectedType == index

            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(index) },
                label = {
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected) type.selectedIcon else type.unselectedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = type.color.copy(alpha = 0.2f),
                    selectedLabelColor = type.color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = type.color,
                    selectedBorderWidth = 1.5.dp,
                    enabled = true,
                    selected = isSelected,
                )
            )
        }
    }
}

@Composable
private fun SelectedRouteChip(
    route: Route,
    onRemove: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    InputChip(
        selected = true,
        onClick = { },
        label = {
            Text(
                text = route.name.ru,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = when (route.typeId) {
                    0 -> Icons.Default.DirectionsBus
                    1 -> Icons.Default.Tram
                    else -> Icons.Default.DirectionsSubway
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить",
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = true
                        onRemove()
                    }
            )
        },
        modifier = Modifier.scale(scale),
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun EnhancedRouteCard(
    route: Route,
    isSelected: Boolean,
    transportColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                transportColor.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, transportColor)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Subtle background pattern for selected state
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    transportColor.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                radius = 100f
                            )
                        )
                )
            }

            Text(
                text = route.name.ru,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = if (isSelected) {
                    transportColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun BottomActionSection(
    selectedRoutes: List<Route>,
    showScheme: Boolean,
    onSchemeToggle: (Boolean) -> Unit,
    onNavigateToMap: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Выбрано маршрутов: ${selectedRoutes.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scheme button
                ElevatedButton(
                    onClick = { onSchemeToggle(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (showScheme) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (showScheme) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = if (showScheme) 8.dp else 4.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (showScheme) Icons.Default.Schema else Icons.Outlined.Schema,
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

                // Map button
                Button(
                    onClick = {
                        onSchemeToggle(false)
                        onNavigateToMap()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showScheme) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (!showScheme) 8.dp else 4.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (!showScheme) Icons.Default.Map else Icons.Outlined.Map,
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
        }
    }
}

// Data class for transport types
private data class TransportType(
    val name: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val color: Color
)