package com.uj.planner.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS 의 DB 파일 자리. 문서 디렉터리에 둔다 — 백업 대상이고 앱을 지우면 같이 사라진다. */
private fun plannerDatabaseBuilder(): RoomDatabase.Builder<PlannerDatabase> {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val path = requireNotNull(documents?.path) { "문서 디렉터리를 찾지 못함" }
    return Room.databaseBuilder(
        name = "$path/${PlannerDatabase.FILE_NAME}",
        factory = { PlannerDatabaseConstructor.initialize() },
    )
}

/**
 * iOS 에서 DB 를 연다.
 *
 * 안드로이드 쪽과 같은 모양으로 맞춘다 — 빌더 타입을 밖으로 내보내지 않아 Swift 호출부가
 * Room 타입을 마주칠 일이 없다.
 */
fun createPlannerDatabase(): PlannerDatabase = createPlannerDatabase(plannerDatabaseBuilder())
