// WelcomeScreen.kt
package com.example.publictransport.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.publictransport.R
import kotlinx.coroutines.delay

/**
 *  экран-приветствие с анимациями и градиентным фоном
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    // Запуск анимации при появлении экрана
    LaunchedEffect(Unit) {
        isVisible = true
        delay(200)
        logoVisible = true
        delay(300)
        textVisible = true
        delay(400)
        buttonVisible = true
    }

    // Анимированное значение для масштаба кнопки при наведении
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(800))
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Анимированный логотип
                    AnimatedVisibility(
                        visible = logoVisible,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(animationSpec = tween(600))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            Color.Transparent
                                        ),
                                        radius = 100f
                                    ),
                                    shape = RoundedCornerShape(60.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Логотип приложения",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }

                    // Анимированный текст заголовка
                    AnimatedVisibility(
                        visible = textVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(600, delayMillis = 100)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 100))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Добро пожаловать",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "в мир удобного общественного транспорта",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Описание с анимацией
                    AnimatedVisibility(
                        visible = textVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(600, delayMillis = 200)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeatureItem(
                                text = "📍 Планирование маршрутов",
                                visible = textVisible
                            )
                            FeatureItem(
                                text = "🚌 Отслеживание транспорта",
                                visible = textVisible
                            )
                            FeatureItem(
                                text = "⏰ Актуальное расписание",
                                visible = textVisible
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Анимированная кнопка
                    AnimatedVisibility(
                        visible = buttonVisible,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(animationSpec = tween(400))
                    ) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(buttonScale),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Начать путешествие",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Декоративные элементы фона
        DecorativeElements()
    }
}

@Composable
private fun FeatureItem(
    text: String,
    visible: Boolean
) {
    var itemVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            itemVisible = true
        }
    }

    AnimatedVisibility(
        visible = itemVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(400))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DecorativeElements() {
    // Анимированные декоративные круги
    val infiniteTransition = rememberInfiniteTransition()

    val circle1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val circle2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .offset(x = (-100).dp, y = (-200).dp)
            .size(200.dp)
            .scale(circle2Scale)
            .alpha(circle1Alpha)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(100.dp)
            )
    )

    Box(
        modifier = Modifier
            .offset(x = 150.dp, y = 250.dp)
            .size(150.dp)
            .alpha(circle1Alpha * 0.7f)
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                RoundedCornerShape(75.dp)
            )
    )
}