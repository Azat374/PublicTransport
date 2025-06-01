// AuthViewModel.kt
package com.example.publictransport.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String, val code: String? = null) : AuthState()
}

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

class AuthViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        checkAuthState()
    }

    /**
     * Проверяет текущее состояние аутентификации при инициализации
     */
    private fun checkAuthState() {
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _authState.value = if (user != null) {
            AuthState.Authenticated
        } else {
            AuthState.Unauthenticated
        }
    }

    /**
     * Вход пользователя
     */
    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val result = firebaseAuth.signInWithEmailAndPassword(
                    email.trim(),
                    password.trim()
                ).await()

                _currentUser.value = result.user
                _authState.value = AuthState.Authenticated

            } catch (exception: FirebaseAuthException) {
                handleAuthException(exception)
            } catch (exception: Exception) {
                _authState.value = AuthState.Error(
                    message = "Произошла неожиданная ошибка при входе",
                    code = "UNKNOWN_ERROR"
                )
            }
        }
    }

    /**
     * Регистрация пользователя
     */
    fun register(email: String, password: String) {
        if (!validateInput(email, password)) return

        if (!validatePassword(password)) {
            _authState.value = AuthState.Error(
                message = "Пароль должен содержать минимум 6 символов",
                code = "WEAK_PASSWORD"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(
                    email.trim(),
                    password.trim()
                ).await()

                _currentUser.value = result.user
                _authState.value = AuthState.Authenticated

                // Отправляем письмо с подтверждением email
                sendEmailVerification()

            } catch (exception: FirebaseAuthException) {
                handleAuthException(exception)
            } catch (exception: Exception) {
                _authState.value = AuthState.Error(
                    message = "Произошла неожиданная ошибка при регистрации",
                    code = "UNKNOWN_ERROR"
                )
            }
        }
    }

    /**
     * Выход из системы
     */
    fun logout() {
        firebaseAuth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Сброс пароля
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error(
                message = "Введите email для сброса пароля",
                code = "EMPTY_EMAIL"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                firebaseAuth.sendPasswordResetEmail(email.trim()).await()
                _authState.value = AuthState.Unauthenticated
                // Здесь можно добавить отдельное состояние для успешной отправки письма

            } catch (exception: FirebaseAuthException) {
                handleAuthException(exception)
            } catch (exception: Exception) {
                _authState.value = AuthState.Error(
                    message = "Не удалось отправить письмо для сброса пароля",
                    code = "RESET_PASSWORD_FAILED"
                )
            }
        }
    }

    /**
     * Отправка письма с подтверждением email
     */
    fun sendEmailVerification() {
        viewModelScope.launch {
            try {
                _currentUser.value?.sendEmailVerification()?.await()
            } catch (exception: Exception) {
                // Не меняем состояние аутентификации, просто логируем ошибку
                // В реальном приложении здесь можно показать уведомление
            }
        }
    }

    /**
     * Очистка ошибок
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            val currentUser = _currentUser.value
            _authState.value = if (currentUser != null) {
                AuthState.Authenticated
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    /**
     * Проверка email и пароля на пустоту
     */
    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _authState.value = AuthState.Error(
                    message = "Email не может быть пустым",
                    code = "EMPTY_EMAIL"
                )
                false
            }
            password.isBlank() -> {
                _authState.value = AuthState.Error(
                    message = "Пароль не может быть пустым",
                    code = "EMPTY_PASSWORD"
                )
                false
            }
            else -> true
        }
    }

    /**
     * Проверка сложности пароля
     */
    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Обработка ошибок Firebase Authentication
     */
    private fun handleAuthException(exception: FirebaseAuthException) {
        val errorMessage = when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Некорректный формат email"
            "ERROR_WRONG_PASSWORD" -> "Неверный пароль"
            "ERROR_USER_NOT_FOUND" -> "Пользователь с таким email не найден"
            "ERROR_USER_DISABLED" -> "Аккаунт заблокирован"
            "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток входа. Попробуйте позже"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Аккаунт с таким email уже существует"
            "ERROR_WEAK_PASSWORD" -> "Пароль слишком простой. Минимум 6 символов"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Проблемы с подключением к интернету"
            "ERROR_INVALID_CREDENTIAL" -> "Неверные данные для входа"
            else -> exception.localizedMessage ?: "Произошла ошибка аутентификации"
        }

        _authState.value = AuthState.Error(
            message = errorMessage,
            code = exception.errorCode
        )
    }

    /**
     * Проверка, подтвержден ли email пользователя
     */
    fun isEmailVerified(): Boolean {
        return _currentUser.value?.isEmailVerified == true
    }

    /**
     * Обновление профиля пользователя
     */
    fun reloadUser() {
        viewModelScope.launch {
            try {
                _currentUser.value?.reload()?.await()
                _currentUser.value = firebaseAuth.currentUser
            } catch (exception: Exception) {
                // Обработка ошибки обновления профиля
            }
        }
    }
}