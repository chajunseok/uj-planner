package com.uj.planner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.uj.planner.data.entity.FlexTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlexTaskDao {
    @Query("SELECT * FROM flex_task ORDER BY priority DESC, title")
    fun observeAll(): Flow<List<FlexTaskEntity>>

    @Query("SELECT * FROM flex_task")
    suspend fun getAll(): List<FlexTaskEntity>

    @Upsert
    suspend fun upsert(task: FlexTaskEntity): Long

    /** 이 일정의 배치도 외래키 CASCADE 로 함께 지워진다. */
    @Delete
    suspend fun delete(task: FlexTaskEntity)
}
