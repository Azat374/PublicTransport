package com.example.publictransport.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.example.publictransport.repository.TransportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TransportViewModel : ViewModel() {

    private val repository = TransportRepository()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes

    private val _stops = MutableStateFlow<List<Stop>>(emptyList())
    val stops: StateFlow<List<Stop>> = _stops

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _routes.value = repository.getRoutes()
                _stops.value = repository.getStops()
            } catch (e: Exception) {
                // можно добавить лог
            } finally {
                _isLoading.value = false
            }
        }
    }
}
