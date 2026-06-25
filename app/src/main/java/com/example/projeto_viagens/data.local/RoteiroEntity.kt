package com.example.projeto_viagens.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Roteiro turístico gerado pela IA, vinculado a uma viagem.
 * Mantemos apenas o roteiro mais recente por viagem (substituído a cada
 * nova geração), mas o esquema permite histórico se necessário.
 */
@Entity(
    tableName = "trip_roteiros",
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
data class RoteiroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val interesses: String,
    val conteudo: String,
    val createdAt: Long = System.currentTimeMillis()
)
