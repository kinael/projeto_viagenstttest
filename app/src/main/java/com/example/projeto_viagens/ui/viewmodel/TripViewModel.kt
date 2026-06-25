package com.example.projeto_viagens.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_viagens.data.local.AppDatabase
import com.example.projeto_viagens.data.local.TripEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TripFormUiState(
    val destination: String = "",
    val type: String = "Lazer", // "Lazer" ou "Negócios"
    val startDate: String = "",
    val endDate: String = "",
    val budget: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val tripDao = AppDatabase.getDatabase(application).tripDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    var formUiState by mutableStateOf(TripFormUiState())
        private set

    // userId do usuário logado (definido após login)
    var loggedUserId by mutableStateOf(0)
        private set

    // trips do usuário logado como StateFlow
    var trips: StateFlow<List<TripEntity>> = tripDao
        .getTripsByUser(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        private set

    // Viagem encontrada pela busca de localização (cidade + intervalo de datas)
    var currentTripByLocation by mutableStateOf<TripEntity?>(null)
        private set

    var isSearchingByLocation by mutableStateOf(false)
        private set

    fun initUser(email: String) {
        viewModelScope.launch {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                loggedUserId = user.id
                // recarrega o flow com o userId correto
                trips = tripDao
                    .getTripsByUser(user.id)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            }
        }
    }

    // --- Form ---
    fun onDestinationChange(value: String) {
        formUiState = formUiState.copy(destination = value, errorMessage = null)
    }

    fun onTypeChange(value: String) {
        formUiState = formUiState.copy(type = value, errorMessage = null)
    }

    fun onStartDateChange(value: String) {
        formUiState = formUiState.copy(startDate = value, errorMessage = null)
    }

    fun onEndDateChange(value: String) {
        formUiState = formUiState.copy(endDate = value, errorMessage = null)
    }

    fun onBudgetChange(value: String) {
        formUiState = formUiState.copy(budget = value, errorMessage = null)
    }

    fun resetForm() {
        formUiState = TripFormUiState()
    }

    fun loadTripForEdit(trip: TripEntity) {
        formUiState = TripFormUiState(
            destination = trip.destination,
            type = trip.type,
            startDate = trip.startDate,
            endDate = trip.endDate,
            budget = trip.budget.toString()
        )
    }

    fun saveTrip(onSuccess: () -> Unit) {
        val state = formUiState

        if (state.destination.isBlank() || state.startDate.isBlank() ||
            state.endDate.isBlank() || state.budget.isBlank()
        ) {
            formUiState = state.copy(errorMessage = "Todos os campos são obrigatórios.")
            return
        }

        val budgetValue = state.budget.replace(",", ".").toDoubleOrNull()
        if (budgetValue == null || budgetValue <= 0) {
            formUiState = state.copy(errorMessage = "Informe um orçamento válido.")
            return
        }

        formUiState = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                tripDao.insertTrip(
                    TripEntity(
                        destination = state.destination,
                        type = state.type,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        budget = budgetValue,
                        totalSpent = 0.0,
                        userId = loggedUserId
                    )
                )
                formUiState = TripFormUiState()
                onSuccess()
            } catch (e: Exception) {
                formUiState = state.copy(
                    isLoading = false,
                    errorMessage = "Erro ao salvar viagem. Tente novamente."
                )
            }
        }
    }

    fun updateTrip(trip: TripEntity, onSuccess: () -> Unit) {
        val state = formUiState

        if (state.destination.isBlank() || state.startDate.isBlank() ||
            state.endDate.isBlank() || state.budget.isBlank()
        ) {
            formUiState = state.copy(errorMessage = "Todos os campos são obrigatórios.")
            return
        }

        val budgetValue = state.budget.replace(",", ".").toDoubleOrNull()
        if (budgetValue == null || budgetValue <= 0) {
            formUiState = state.copy(errorMessage = "Informe um orçamento válido.")
            return
        }

        formUiState = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                tripDao.updateTrip(
                    trip.copy(
                        destination = state.destination,
                        type = state.type,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        budget = budgetValue
                    )
                )
                formUiState = TripFormUiState()
                onSuccess()
            } catch (e: Exception) {
                formUiState = state.copy(
                    isLoading = false,
                    errorMessage = "Erro ao atualizar viagem."
                )
            }
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            tripDao.deleteTrip(trip)
        }
    }

    /**
     * Procura no banco uma viagem do usuário cujo destino corresponda à cidade
     * informada (case-insensitive) e cuja data atual esteja dentro do intervalo
     * [startDate, endDate] da viagem.
     *
     * Atualiza [currentTripByLocation] com o resultado (ou null se não encontrar).
     */
    fun searchTripByCity(city: String) {
        if (city.isBlank() || loggedUserId == 0) {
            currentTripByLocation = null
            return
        }

        isSearchingByLocation = true
        viewModelScope.launch {
            try {
                val candidates = tripDao.findTripsByCity(loggedUserId, city.trim())
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateFormat.isLenient = false

                // Normaliza "hoje" para 00:00:00 — comparações de Date com horas
                // poderiam excluir uma viagem cujo endDate seja a data de hoje.
                val today = startOfDay(Date())

                val match = candidates.firstOrNull { trip ->
                    try {
                        val start = startOfDay(dateFormat.parse(trip.startDate) ?: return@firstOrNull false)
                        val end = startOfDay(dateFormat.parse(trip.endDate) ?: return@firstOrNull false)
                        // data atual >= startDate E data atual <= endDate
                        !today.before(start) && !today.after(end)
                    } catch (e: Exception) {
                        false
                    }
                }

                currentTripByLocation = match
            } catch (e: Exception) {
                currentTripByLocation = null
            } finally {
                isSearchingByLocation = false
            }
        }
    }

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}