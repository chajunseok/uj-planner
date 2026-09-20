package com.uj.planner.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 안드로이드의 DB 파일 자리. 경로가 지금까지와 같아 기존 파일을 그대로 연다.
 *
 * 팩토리를 명시해서 넘긴다. 빼면 Room 이 리플렉션으로 PlannerDatabase_Impl 을 찾으려 하는데,
 * @ConstructedBy 를 쓰는 순간 그 경로가 아니라 KSP 가 만든 생성자 객체를 써야 한다.
 */
private fun plannerDatabaseBuilder(context: Context): RoomDatabase.Builder<PlannerDatabase> =
    Room.databaseBuilder(
        context = context,
        name = context.getDatabasePath(PlannerDatabase.FILE_NAME).absolutePath,
        factory = { PlannerDatabaseConstructor.initialize() },
    )

/**
 * 안드로이드에서 DB 를 연다.
 *
 * 빌더 타입을 밖으로 내보내지 않는다. 그래야 :app 이 Room 을 직접 의존하지 않아도 된다.
 */
fun createPlannerDatabase(context: Context): PlannerDatabase = createPlannerDatabase(plannerDatabaseBuilder(context))
