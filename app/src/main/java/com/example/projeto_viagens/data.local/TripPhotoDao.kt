package com.example.projeto_viagens.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPhotoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhoto(photo: TripPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: TripPhotoEntity)

    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getPhotosByTrip(tripId: Int): Flow<List<TripPhotoEntity>>
}
