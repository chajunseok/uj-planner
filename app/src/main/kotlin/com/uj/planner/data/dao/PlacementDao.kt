package com.uj.planner.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    /** 아직 시작하지 않은 예정 배치만 지운다. 진행 중이거나 지나간 것, 완료·못함·버림은 남긴다. */
    @Query(
        "DELETE FROM placement WHERE status = 'PLANNED' AND date BETWEEN :from AND :to " +
            "AND (date > :today OR (date = :today AND startMin >= :nowMin))",
    )
    suspend fun deleteUpcomingPlanned(from: LocalDate, to: LocalDate, today: LocalDate, nowMin: Int)
}
