package com.example.projeto_viagens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projeto_viagens.data.local.TripEntity
import com.example.projeto_viagens.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(
    viewModel: TripViewModel,
    editingTrip: TripEntity? = null,
    onSuccess: () -> Unit
) {
    val state = viewModel.formUiState

    // Carrega dados ao editar
    LaunchedEffect(editingTrip) {
        if (editingTrip != null) {
            viewModel.loadTripForEdit(editingTrip)
        } else {
            viewModel.resetForm()
        }
    }

    // DatePicker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Dialogs de data
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onStartDateChange(dateFormatter.format(Date(millis)))
                    }
                    showStartDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onEndDateChange(dateFormatter.format(Date(millis)))
                    }
                    showEndDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (editingTrip != null) "Editar Viagem" else "Nova Viagem",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (editingTrip != null) "Atualize os dados da viagem" else "Preencha os dados da viagem",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Destino
        OutlinedTextField(
            value = state.destination,
            onValueChange = { viewModel.onDestinationChange(it) },
            label = { Text("Destino") },
            placeholder = { Text("Ex: Paris, França") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tipo: Lazer ou Negócios
        Text(
            text = "Tipo de Viagem",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Lazer", "Negócios").forEach { tipo ->
                FilterChip(
                    selected = state.type == tipo,
                    onClick = { viewModel.onTypeChange(tipo) },
                    label = { Text(tipo) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Data Início
        OutlinedTextField(
            value = state.startDate,
            onValueChange = {},
            label = { Text("Data de Início") },
            placeholder = { Text("dd/MM/yyyy") },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showStartDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Selecionar data início")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Data Fim
        OutlinedTextField(
            value = state.endDate,
            onValueChange = {},
            label = { Text("Data de Fim") },
            placeholder = { Text("dd/MM/yyyy") },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showEndDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Selecionar data fim")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Orçamento
        OutlinedTextField(
            value = state.budget,
            onValueChange = { viewModel.onBudgetChange(it) },
            label = { Text("Orçamento (R$)") },
            placeholder = { Text("Ex: 5000.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Erro
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (editingTrip != null) {
                    viewModel.updateTrip(editingTrip, onSuccess)
                } else {
                    viewModel.saveTrip(onSuccess)
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (editingTrip != null) "Atualizar" else "Salvar Viagem")
            }
        }
    }
}