package com.uj.planner.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uj.planner.data.entity.DayAvailabilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM day_availability ORDER BY dayOfWeek")
    fun observeAll(): Flow<List<DayAvailabilityEntity>>

    @Query("SELECT * FROM day_availability")
    suspend fun getAll(): List<DayAvailabilityEntity>

    @Upsert
    suspend fun upsertAll(days: List<DayAvailabilityEntity>)
}
