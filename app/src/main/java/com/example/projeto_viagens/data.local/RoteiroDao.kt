package com.example.projeto_viagens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoteiroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoteiro(roteiro: RoteiroEntity)

    /** Roteiro mais recente da viagem (observável). */
    @Query("SELECT * FROM trip_roteiros WHERE tripId = :tripId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestRoteiroByTrip(tripId: Int): Flow<RoteiroEntity?>

    /** Remove roteiros antigos da viagem, mantendo só o mais recente. */
    @Query(
        """
        DELETE FROM trip_roteiros
        WHERE tripId = :tripId
          AND id NOT IN (
            SELECT id FROM trip_roteiros
            WHERE tripId = :tripId
            ORDER BY createdAt DESC
            LIMIT 1
          )
        """
    )
    suspend fun deleteOldRoteiros(tripId: Int)
}
