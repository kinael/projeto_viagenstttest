package com.example.projeto_viagens.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY id DESC")
    fun getTripsByUser(userId: Int): Flow<List<TripEntity>>

    /**
     * Busca todas as viagens do usuário cujo destino corresponde à cidade
     * informada (case-insensitive). O filtro adicional por intervalo de datas
     * é feito no ViewModel, já que as datas são armazenadas como string
     * no formato dd/MM/yyyy.
     */
    @Query("SELECT * FROM trips WHERE userId = :userId AND LOWER(destination) = LOWER(:city)")
    suspend fun findTripsByCity(userId: Int, city: String): List<TripEntity>
}