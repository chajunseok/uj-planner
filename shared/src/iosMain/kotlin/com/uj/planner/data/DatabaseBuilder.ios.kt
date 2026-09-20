package com.uj.planner.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS 의 DB 파일 자리. 문서 디렉터리에 둔다 — 백업 대상이고 앱을 지우면 같이 사라진다. */
fun plannerDatabaseBuilder(): RoomDatabase.Builder<PlannerDatabase> {
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
