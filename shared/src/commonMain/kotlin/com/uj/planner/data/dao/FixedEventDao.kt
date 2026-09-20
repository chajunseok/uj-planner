package com.uj.planner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.uj.planner.data.entity.FixedEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedEventDao {
    @Query("SELECT * FROM fixed_event ORDER BY dayOfWeek, startMin")
    fun observeAll(): Flow<List<FixedEventEntity>>

    @Query("SELECT * FROM fixed_event")
    suspend fun getAll(): List<FixedEventEntity>

    @Upsert
    suspend fun upsert(event: FixedEventEntity): Long

    @Delete
    suspend fun delete(event: FixedEventEntity)
}
