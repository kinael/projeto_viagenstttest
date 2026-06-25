package com.example.projeto_viagens.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_viagens.data.local.AppDatabase
import com.example.projeto_viagens.data.local.TripPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val photoDao = AppDatabase.getDatabase(application).tripPhotoDao()

    // Id da viagem corrente cujas fotos estão sendo exibidas
    private val _tripId = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val photos: StateFlow<List<TripPhotoEntity>> = _tripId
        .flatMapLatest { id ->
            if (id == 0) flowOf(emptyList()) else photoDao.getPhotosByTrip(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTrip(tripId: Int) {
        _tripId.value = tripId
    }

    /**
     * Copia a imagem apontada por [sourceUri] para o armazenamento interno do
     * app e vincula o arquivo resultante à viagem corrente.
     */
    fun addPhotoFromUri(sourceUri: Uri) {
        val tripId = _tripId.value
        if (tripId == 0) return

        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                copyToInternalStorage(sourceUri, tripId)
            } ?: return@launch

            photoDao.insertPhoto(
                TripPhotoEntity(tripId = tripId, uri = savedPath)
            )
        }
    }

    /**
     * Vincula à viagem uma foto cujo arquivo já existe (ex.: capturada pela
     * câmera diretamente no destino interno).
     */
    fun addPhotoFromFile(file: File) {
        val tripId = _tripId.value
        if (tripId == 0 || !file.exists()) return

        viewModelScope.launch {
            photoDao.insertPhoto(
                TripPhotoEntity(tripId = tripId, uri = Uri.fromFile(file).toString())
            )
        }
    }

    fun deletePhoto(photo: TripPhotoEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(Uri.parse(photo.uri).path ?: "")
                    if (file.exists()) file.delete()
                } catch (_: Exception) {
                }
            }
            photoDao.deletePhoto(photo)
        }
    }

    /** Cria um arquivo de destino dentro de files/trip_photos para a câmera. */
    fun createImageFile(): File {
        val dir = File(getApplication<Application>().filesDir, "trip_photos")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    }

    private fun copyToInternalStorage(sourceUri: Uri, tripId: Int): String? {
        return try {
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "trip_photos")
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, "IMG_${tripId}_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Uri.fromFile(dest).toString()
        } catch (e: Exception) {
            null
        }
    }
}
