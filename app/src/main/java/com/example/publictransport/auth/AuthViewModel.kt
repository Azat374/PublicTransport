// AuthViewModel.kt
package com.example.publictransport.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading       : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email или пароль не могут быть пустыми")
            return
        }

        _authState.value = AuthState.Loading

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    val err = task.exception?.localizedMessage ?: "Неизвестная ошибка при входе"
                    _authState.value = AuthState.Error(err)
                }
            }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email или пароль не могут быть пустыми")
            return
        }

        _authState.value = AuthState.Loading

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    val err = task.exception?.localizedMessage ?: "Неизвестная ошибка при регистрации"
                    _authState.value = AuthState.Error(err)
                }
            }
    }

    fun logout() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
