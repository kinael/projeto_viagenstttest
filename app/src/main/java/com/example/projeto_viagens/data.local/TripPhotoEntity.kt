package com.example.projeto_viagens.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Foto vinculada a uma viagem. O campo [uri] guarda o caminho/URI do arquivo
 * de imagem (copiado para o armazenamento interno do app).
 */
@Entity(
    tableName = "trip_photos",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TripPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)
