// RegisterScreen.kt
package com.example.publictransport.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    // Локальный стейт для валидации
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Регистрация",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // E-mail
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = localError != null && (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
            )
            if (localError != null && (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
                Text(
                    text = "Введите корректный e-mail",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Пароль
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = localError != null && password.isBlank()
            )
            if (localError != null && password.isBlank()) {
                Text(
                    text = "Введите пароль",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Подтвердите пароль
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                label = { Text("Повторите пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                isError = localError != null && (
                        confirmPassword.isBlank() ||
                                confirmPassword != password
                        )
            )
            if (localError != null && (confirmPassword.isBlank() || confirmPassword != password)) {
                Text(
                    text = if (confirmPassword.isBlank()) "Повторите пароль"
                    else "Пароли не совпадают",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Ошибка из Firebase
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Кнопка «Зарегистрироваться»
            Button(
                onClick = {
                    localError = null
                    when {
                        email.isBlank() -> localError = "Введите e-mail"
                        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> localError = "Некорректный e-mail"
                        password.isBlank() -> localError = "Введите пароль"
                        confirmPassword.isBlank() -> localError = "Повторите пароль"
                        password != confirmPassword -> localError = "Пароли не совпадают"
                        else -> {
                            authViewModel.register(email.trim(), password.trim())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp ),
                enabled = authState != AuthState.Loading
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Регистрируем…")
                } else {
                    Text("Зарегистрироваться")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ссылка на вход
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Уже есть аккаунт? Войти")
            }
        }
    }
}
