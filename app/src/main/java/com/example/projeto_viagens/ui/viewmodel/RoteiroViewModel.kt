package com.example.projeto_viagens.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_viagens.data.local.AppDatabase
import com.example.projeto_viagens.data.local.TripEntity
import com.example.projeto_viagens.data.remote.GeminiRetrofitClient
import com.example.projeto_viagens.data.repository.RoteiroRepository
import com.example.projeto_viagens.data.repository.RoteiroResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoteiroUiState(
    val interesses: String = "",
    val isLoading: Boolean = false,
    val roteiro: String? = null,   // roteiro atualmente exibido (persistido ou recém-gerado)
    val errorMessage: String? = null
)

/**
 * ViewModel da funcionalidade de Roteiro. Gerencia o estado da tela e delega
 * a geração/persistência ao [RoteiroRepository].
 */
class RoteiroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoteiroRepository(
        api = GeminiRetrofitClient.service,
        roteiroDao = AppDatabase.getDatabase(application).roteiroDao()
    )

    private val _uiState = MutableStateFlow(RoteiroUiState())
    val uiState: StateFlow<RoteiroUiState> = _uiState.asStateFlow()

    private var currentTrip: TripEntity? = null

    /**
     * Inicializa a tela para uma viagem: carrega o roteiro já persistido
     * (se houver) para exibição imediata.
     */
    fun init(trip: TripEntity) {
        if (currentTrip?.id == trip.id) return
        currentTrip = trip

        viewModelScope.launch {
            repository.observeRoteiro(trip.id).collect { saved ->
                _uiState.update {
                    it.copy(
                        roteiro = saved?.conteudo ?: it.roteiro,
                        interesses = if (it.interesses.isBlank() && saved != null)
                            saved.interesses else it.interesses
                    )
                }
            }
        }
    }

    fun onInteressesChange(value: String) {
        _uiState.update { it.copy(interesses = value, errorMessage = null) }
    }

    /** Dispara a geração do roteiro via IA. */
    fun gerarRoteiro() {
        val trip = currentTrip ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.gerarRoteiro(trip, _uiState.value.interesses)) {
                is RoteiroResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, roteiro = result.conteudo, errorMessage = null)
                    }
                }
                is RoteiroResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }
}
