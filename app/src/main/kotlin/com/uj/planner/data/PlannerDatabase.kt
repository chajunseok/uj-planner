package com.uj.planner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uj.planner.data.dao.AvailabilityDao
import com.uj.planner.data.dao.FixedEventDao
import com.uj.planner.data.dao.FlexTaskDao
import com.uj.planner.data.dao.PlacementDao
import com.uj.planner.data.entity.DayAvailabilityEntity
import com.uj.planner.data.entity.FixedEventEntity
import com.uj.planner.data.entity.FlexTaskEntity
import com.uj.planner.data.entity.PlacementEntity

// ponytail: 출시 전이라 스키마를 내보내지 않고 버전 1 에서 자유롭게 바꾼다.
// 첫 릴리스 때 exportSchema 를 켜고 그 뒤로는 마이그레이션을 작성한다.
@Database(
    entities = [FixedEventEntity::class, FlexTaskEntity::class, PlacementEntity::class, DayAvailabilityEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun fixedEvents(): FixedEventDao
    abstract fun flexTasks(): FlexTaskDao
    abstract fun placements(): PlacementDao
    abstract fun availability(): AvailabilityDao

    companion object {
        fun create(context: Context): PlannerDatabase =
            Room.databaseBuilder(context, PlannerDatabase::class.java, "planner.db")
                .addCallback(SeedDefaultAvailability)
                .build()
    }
}

/** DB 를 처음 만들 때 기본 가용 시간을 넣는다. 평일 08:00~24:00, 주말 10:00~25:00. */
private object SeedDefaultAvailability : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        for (day in 1..7) {
            val (start, end) = if (day <= 5) 8 * 60 to 24 * 60 else 10 * 60 to 25 * 60
            db.execSQL("INSERT INTO day_availability (dayOfWeek, startMin, endMin) VALUES ($day, $start, $end)")
        }
    }
}
