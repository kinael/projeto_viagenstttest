package com.example.projeto_viagens.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projeto_viagens.data.local.TripEntity
import com.example.projeto_viagens.ui.viewmodel.LocationViewModel
import com.example.projeto_viagens.ui.viewmodel.TripViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

sealed class MenuDestination {
    object Home : MenuDestination()
    object NewTrip : MenuDestination()
    object MyTrips : MenuDestination()
    object Location : MenuDestination()
    object About : MenuDestination()
    data class EditTrip(val trip: TripEntity) : MenuDestination()
    data class Photos(val trip: TripEntity) : MenuDestination()
    data class Roteiro(val trip: TripEntity) : MenuDestination()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MenuScreen(
    email: String = "",
    tripViewModel: TripViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination: MenuDestination by remember { mutableStateOf(MenuDestination.Home) }

    // --- Localização (elevada para o nível do menu) ---
    val locationViewModel: LocationViewModel = viewModel()
    val locationState by locationViewModel.uiState.collectAsStateWithLifecycle()

    val locationPermission = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    ) { granted ->
        if (granted) locationViewModel.onPermissionGranted()
        else locationViewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            locationViewModel.onPermissionGranted()
        }
    }

    LaunchedEffect(locationState.city, tripViewModel.loggedUserId) {
        val city = locationState.city
        if (!city.isNullOrBlank() && tripViewModel.loggedUserId != 0) {
            tripViewModel.searchTripByCity(city)
        }
    }

    val currentTrip = tripViewModel.currentTripByLocation

    // Diálogo de confirmação de logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
            title = { Text("Sair da conta") },
            text = { Text("Deseja realmente sair da conta $email?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) { Text("Sair", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    LaunchedEffect(email) {
        tripViewModel.initUser(email)
    }

    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentDestination is MenuDestination.Photos -> currentDestination = MenuDestination.Home
            currentDestination is MenuDestination.Roteiro -> currentDestination = MenuDestination.Home
            currentDestination !is MenuDestination.Home -> currentDestination = MenuDestination.Home
            else -> (context as? Activity)?.finish()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Gerenciamento de Viagens",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Início") },
                    selected = currentDestination is MenuDestination.Home,
                    onClick = {
                        currentDestination = MenuDestination.Home
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = null) },
                    label = { Text("Nova Viagem") },
                    selected = currentDestination is MenuDestination.NewTrip,
                    onClick = {
                        currentDestination = MenuDestination.NewTrip
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Luggage, contentDescription = null) },
                    label = { Text("Minhas Viagens") },
                    selected = currentDestination is MenuDestination.MyTrips,
                    onClick = {
                        currentDestination = MenuDestination.MyTrips
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    label = { Text("Localização") },
                    selected = currentDestination is MenuDestination.Location,
                    onClick = {
                        currentDestination = MenuDestination.Location
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Sobre") },
                    selected = currentDestination is MenuDestination.About,
                    onClick = {
                        currentDestination = MenuDestination.About
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Botão de Sair — separado no fundo do drawer
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    label = {
                        Text(
                            "Sair",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showLogoutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentDestination) {
                                is MenuDestination.NewTrip -> "Nova Viagem"
                                is MenuDestination.MyTrips -> "Minhas Viagens"
                                is MenuDestination.Location -> "Localização"
                                is MenuDestination.About -> "Sobre"
                                is MenuDestination.EditTrip -> "Editar Viagem"
                                is MenuDestination.Photos -> "Fotos"
                                is MenuDestination.Roteiro -> "Roteiro"
                                else -> "Menu Principal"
                            }
                        )
                    },
                    navigationIcon = {
                        if (currentDestination is MenuDestination.Photos ||
                            currentDestination is MenuDestination.Roteiro
                        ) {
                            IconButton(onClick = { currentDestination = MenuDestination.Home }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Abrir menu")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // Bottom Bar exibida apenas na tela inicial quando há viagem corrente
                if (currentDestination is MenuDestination.Home && currentTrip != null) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = false,
                            onClick = { currentDestination = MenuDestination.Roteiro(currentTrip) },
                            icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                            label = { Text("Roteiro") }
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { currentDestination = MenuDestination.Photos(currentTrip) },
                            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            label = { Text("Fotos") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (val dest = currentDestination) {
                    is MenuDestination.Home -> HomeContent(
                        email = email,
                        locationState = locationState,
                        permissionGranted = locationPermission.status.isGranted,
                        showRationale = locationPermission.status.shouldShowRationale,
                        isSearchingByLocation = tripViewModel.isSearchingByLocation,
                        currentTrip = currentTrip,
                        onRequestPermission = { locationPermission.launchPermissionRequest() }
                    )
                    is MenuDestination.NewTrip -> NewTripScreen(
                        viewModel = tripViewModel,
                        onSuccess = { currentDestination = MenuDestination.MyTrips }
                    )
                    is MenuDestination.MyTrips -> MyTripsScreen(
                        viewModel = tripViewModel,
                        onEditTrip = { trip ->
                            currentDestination = MenuDestination.EditTrip(trip)
                        }
                    )
                    is MenuDestination.EditTrip -> NewTripScreen(
                        viewModel = tripViewModel,
                        editingTrip = dest.trip,
                        onSuccess = { currentDestination = MenuDestination.MyTrips }
                    )
                    is MenuDestination.Location -> LocationScreen()
                    is MenuDestination.About -> AboutContent()
                    is MenuDestination.Photos -> PhotosScreen(
                        tripId = dest.trip.id,
                        tripDestination = dest.trip.destination
                    )
                    is MenuDestination.Roteiro -> RoteiroScreen(
                        trip = dest.trip
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HomeContent
// ---------------------------------------------------------------------------

@Composable
fun HomeContent(
    email: String,
    locationState: com.example.projeto_viagens.ui.viewmodel.LocationUiState,
    permissionGranted: Boolean,
    showRationale: Boolean,
    isSearchingByLocation: Boolean,
    currentTrip: TripEntity?,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Bem-vindo!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        LocationCard(
            permissionGranted = permissionGranted,
            showRationale = showRationale,
            isLoading = locationState.isLoading,
            city = locationState.city,
            state = locationState.state,
            country = locationState.country,
            errorMessage = locationState.errorMessage,
            onRequestPermission = onRequestPermission
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isSearchingByLocation -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Procurando viagem para esta cidade...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            currentTrip != null -> CurrentTripCard(
                trip = currentTrip,
                latitude = locationState.latitude,
                longitude = locationState.longitude
            )
            locationState.city != null -> {
                Text(
                    text = "Nenhuma viagem ativa encontrada para ${locationState.city}.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Use o menu lateral para navegar entre as funcionalidades.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LocationCard(
    permissionGranted: Boolean,
    showRationale: Boolean,
    isLoading: Boolean,
    city: String?,
    state: String?,
    country: String?,
    errorMessage: String?,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sua localização", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                !permissionGranted && showRationale -> {
                    Text("Precisamos da sua localização para encontrar a viagem atual.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequestPermission) { Text("Conceder permissão") }
                }
                !permissionGranted -> {
                    Text("Permissão de localização necessária.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequestPermission) { Text("Solicitar permissão") }
                }
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Obtendo localização...", fontSize = 13.sp)
                }
                errorMessage != null -> {
                    Text(text = errorMessage, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
                city != null -> {
                    val parts = listOfNotNull(city, state, country)
                    val fullAddress = parts.distinct().joinToString(", ")
                    Text(
                        text = fullAddress,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> Text(text = "Aguardando dados de localização...", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CurrentTripCard(
    trip: TripEntity,
    latitude: Double?,
    longitude: Double?
) {
    val isLeisure = trip.type == "Lazer"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLeisure) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLeisure) Icons.Filled.BeachAccess else Icons.Filled.BusinessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isLeisure) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Viagem em andamento",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLeisure) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TripInfoRow("Destino", trip.destination)
            TripInfoRow("Data início", trip.startDate)
            TripInfoRow("Data final", trip.endDate)
            TripInfoRow("Tipo", trip.type)
            TripInfoRow("Orçamento", "R$ ${"%.2f".format(trip.budget)}")
            TripInfoRow("Total de gastos", "R$ ${"%.2f".format(trip.totalSpent)}")

            // Mapa com a localização atual da viagem
            if (latitude != null && longitude != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Localização atual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLeisure) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                TripMapView(
                    latitude = latitude,
                    longitude = longitude,
                    markerTitle = trip.destination,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun TripInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, modifier = Modifier.weight(1.4f))
    }
}

// ---------------------------------------------------------------------------
// AboutContent
// ---------------------------------------------------------------------------

@Composable
fun AboutContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Gerenciamento de Viagens", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Versão 1.0.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aplicativo para gerenciar suas viagens de lazer e negócios de forma prática e organizada.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// LocationScreen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen(locationViewModel: LocationViewModel = viewModel()) {
    val locationState by locationViewModel.uiState.collectAsStateWithLifecycle()

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) locationViewModel.onPermissionGranted() else locationViewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) locationPermission.launchPermissionRequest()
        else locationViewModel.onPermissionGranted()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Sua Localização", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        when {
            locationPermission.status.shouldShowRationale -> {
                Text("A permissão de localização é necessária para esta funcionalidade.", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { locationPermission.launchPermissionRequest() }) { Text("Conceder permissão") }
            }
            !locationPermission.status.isGranted -> {
                Text("Permissão de localização não concedida.", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { locationPermission.launchPermissionRequest() }) { Text("Solicitar permissão") }
            }
            locationState.isLoading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Obtendo localização...", fontSize = 14.sp)
            }
            locationState.errorMessage != null -> {
                Text(text = locationState.errorMessage ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            }
            locationState.city != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LocationDetailRow(Icons.Filled.LocationCity, "Cidade", locationState.city ?: "-")
                        LocationDetailRow(Icons.Filled.Map, "Estado", locationState.state ?: "-")
                        LocationDetailRow(Icons.Filled.Public, "País", locationState.country ?: "-")
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        LocationDetailRow(Icons.Filled.MyLocation, "Latitude", "%.6f".format(locationState.latitude))
                        LocationDetailRow(Icons.Filled.MyLocation, "Longitude", "%.6f".format(locationState.longitude))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        LocationDetailRow(Icons.Filled.GpsFixed, "Precisão", "${locationState.accuracy?.toInt() ?: "-"} m")
                    }
                }
            }
            else -> Text(text = "Aguardando dados de localização...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LocationDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}