package com.example.projeto_viagens.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_viagens.data.location.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val errorMessage: String? = null
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])
    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.locationWithCityFlow()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Erro ao obter localização: ${e.message}")
                    }
                }
                .collect { location ->
                    _uiState.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            city = location.city,
                            state = location.state,
                            country = location.country,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(hasPermission = false, isLoading = false, errorMessage = "Permissão de localização negada.")
        }
    }
}