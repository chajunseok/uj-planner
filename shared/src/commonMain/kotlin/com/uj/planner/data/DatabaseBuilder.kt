package com.uj.planner.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * 빌더 마감. 드라이버·콜백·디스패처는 플랫폼이 달라도 같다.
 *
 * 빌더를 **만드는** 일만 플랫폼별이다 — 안드로이드는 Context 가 있어야 파일 경로를 알고,
 * iOS 는 문서 디렉터리를 시스템에 물어본다. expect/actual 을 두지 않고 부르는 쪽에서 넘긴다.
 */
fun createPlannerDatabase(builder: RoomDatabase.Builder<PlannerDatabase>): PlannerDatabase =
    builder
        .addCallback(SeedDefaultAvailability)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
