package com.uj.planner.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uj.planner.data.entity.PlacementEntity
import com.uj.planner.data.entity.PlacementStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PlacementDao {
    @Query("SELECT * FROM placement WHERE date BETWEEN :from AND :to ORDER BY date, startMin")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<PlacementEntity>>

    @Query("SELECT * FROM placement WHERE date BETWEEN :from AND :to ORDER BY date, startMin")
    suspend fun getBetween(from: LocalDate, to: LocalDate): List<PlacementEntity>

    @Query("SELECT * FROM placement WHERE id IN (:ids)")
    suspend fun getByIds(ids: Collection<Long>): List<PlacementEntity>

    /**
     * 끝났는데 아직 확인받지 못한 배치.
     *
     * ponytail: 24:00 을 넘겨 끝나는 배치는 자정이 되는 순간 밀린 것으로 잡힌다(최대 1시간 이름).
     * 심야 일정을 실제로 쓰게 되면 date·endMin 을 절대 시각으로 환산해 비교한다.
     */
    @Query(
        "SELECT * FROM placement WHERE status = 'PLANNED' " +
            "AND (date < :today OR (date = :today AND endMin <= :nowMin)) ORDER BY date, startMin",
    )
    suspend fun getOverdue(today: LocalDate, nowMin: Int): List<PlacementEntity>

    @Insert
    suspend fun insertAll(placements: List<PlacementEntity>)

    @Query("UPDATE placement SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: PlacementStatus)

    @Update
    suspend fun update(placement: PlacementEntity)

    @Query("DELETE FROM placement WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Collection<Long>)

    /** [date] 뒤의 예정 배치를 전부 지운다. 아직 오지 않은 주는 볼 때 다시 짜므로 손으로 옮긴 자리도 남기지 않는다. */
    @Query("DELETE FROM placement WHERE status = 'PLANNED' AND date > :date")
    suspend fun deletePlannedAfter(date: LocalDate)

    @Query("UPDATE placement SET pinned = 0 WHERE flexTaskId = :flexTaskId")
    suspend fun unpinTask(flexTaskId: Long)
}
