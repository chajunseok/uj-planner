package com.uj.planner.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
@ConstructedBy(PlannerDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun fixedEvents(): FixedEventDao
    abstract fun flexTasks(): FlexTaskDao
    abstract fun placements(): PlacementDao
    abstract fun availability(): AvailabilityDao

    companion object {
        const val FILE_NAME = "planner.db"
    }
}

/**
 * 인스턴스를 만드는 자리. **본문을 우리가 쓰지 않는다** — KSP 가 타깃마다 actual 을 생성한다.
 * 리플렉션(`::class.java`)이 사라진 덕에 R8 keep 규칙도 필요 없어진다.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object PlannerDatabaseConstructor : RoomDatabaseConstructor<PlannerDatabase> {
    override fun initialize(): PlannerDatabase
}

/** DB 를 처음 만들 때 기본 가용 시간을 넣는다. 평일 08:00~24:00, 주말 10:00~25:00. */
internal object SeedDefaultAvailability : RoomDatabase.Callback() {
    // KMP 의 Callback 은 바인드 인자를 받는 execSQL 오버로드가 없다. 값이 전부 우리가 만든
    // Int 라 문자열에 그대로 넣어도 주입될 여지가 없다.
    override fun onCreate(connection: SQLiteConnection) {
        for (day in 1..7) {
            val (start, end) = if (day <= 5) 8 * 60 to 24 * 60 else 10 * 60 to 25 * 60
            connection.execSQL("INSERT INTO day_availability (dayOfWeek, startMin, endMin) VALUES ($day, $start, $end)")
        }
    }
}
