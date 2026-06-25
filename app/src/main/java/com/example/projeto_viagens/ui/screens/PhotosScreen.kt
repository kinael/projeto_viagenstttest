package com.example.projeto_viagens.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.projeto_viagens.data.local.TripPhotoEntity
import com.example.projeto_viagens.ui.viewmodel.PhotoViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    tripId: Int,
    tripDestination: String,
    photoViewModel: PhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    val photos by photoViewModel.photos.collectAsStateWithLifecycle()

    LaunchedEffect(tripId) {
        photoViewModel.setTrip(tripId)
    }

    // Controla o menu de escolha (galeria x câmera)
    var showSourceDialog by remember { mutableStateOf(false) }

    // Foto ampliada em tela cheia
    var previewPhoto by remember { mutableStateOf<TripPhotoEntity?>(null) }

    // Arquivo de destino para a câmera (precisa ser lembrado entre o launch e o resultado)
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // Seleção da galeria do dispositivo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoViewModel.addPhotoFromUri(it) }
    }

    // Captura pela câmera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val file = pendingCameraFile
        if (success && file != null) {
            photoViewModel.addPhotoFromFile(file)
        }
        pendingCameraFile = null
    }

    fun launchCamera() {
        val file = photoViewModel.createImageFile()
        pendingCameraFile = file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraLauncher.launch(uri)
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
            title = { Text("Adicionar foto") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Galeria do dispositivo") },
                        leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickableRow {
                                showSourceDialog = false
                                galleryLauncher.launch("image/*")
                            }
                    )
                    ListItem(
                        headlineContent = { Text("Tirar foto (câmera)") },
                        leadingContent = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickableRow {
                                showSourceDialog = false
                                launchCamera()
                            }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Preview em tela cheia
    previewPhoto?.let { photo ->
        Dialog(onDismissRequest = { previewPhoto = null }) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            photoViewModel.deletePhoto(photo)
                            previewPhoto = null
                        }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                        IconButton(onClick = { previewPhoto = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    )
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSourceDialog = true },
                icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                text = { Text("Adicionar") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Text(
                text = "Fotos de $tripDestination",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            if (photos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma foto vinculada a esta viagem.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use o botão \"Adicionar\" para incluir fotos.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickableRow { previewPhoto = photo }
                        )
                    }
                }
            }
        }
    }
}

/** Helper de clique enxuto reutilizado nas células e itens de lista. */
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
