package com.uj.planner.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.room.execSQL
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DB 파일을 통째로 내보내고 가져온다. 서버도 자동 백업도 없는 앱이라, 폰을 바꾸거나 앱을 지울 때 데이터를 옮길 유일한 길이다.
 * 별도 형식을 만들지 않고 SQLite 파일 그대로 다룬다.
 */
class Backup(private val context: Context, private val database: PlannerDatabase) {
    private val dbFile get() = context.getDatabasePath(PlannerDatabase.FILE_NAME)

    suspend fun exportTo(target: Uri) = withContext(Dispatchers.IO) {
        val snapshot = File(context.cacheDir, "export.db")
        snapshot.delete()
        try {
            // 파일을 그냥 복사하면 WAL 에 남은 쓰기가 빠진다. VACUUM INTO 는 그 순간의 일관된 사본을 한 파일로 만든다.
            database.useWriterConnection { it.execSQL("VACUUM INTO '${snapshot.absolutePath.replace("'", "''")}'") }
            val out = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("파일을 열지 못했어요")
            out.use { snapshot.inputStream().use { input -> input.copyTo(it) } }
        } catch (e: Exception) {
            // 저장 위치를 고르는 순간 빈 파일이 이미 만들어져 있다. 실패했으면 반쯤 쓰인 파일을 남기지 않는다.
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) }
            throw e
        } finally {
            snapshot.delete()
        }
    }

    /**
     * 지금 데이터를 [source] 의 내용으로 바꾼다. 검증을 통과하지 못하면 아무것도 바꾸지 않고 [BackupException] 을 던진다.
     * 성공하면 DB 가 닫힌 상태다 — 부른 쪽이 앱을 다시 시작해야 한다. [BackupException] 이 아닌 예외로 실패했을 때도
     * DB 가 이미 닫혔을 수 있으므로 다시 시작해야 한다(그 경우 옛 파일이 그대로 남아 있다).
     */
    suspend fun importFrom(source: Uri) = withContext(Dispatchers.IO) {
        // rename 으로 한 번에 갈아 끼우려면 같은 폴더에 받아야 한다.
        val incoming = File(dbFile.parentFile, "import.db")
        incoming.delete()
        try {
            // 여기까지는 DB 를 건드리지 않는다. 실패는 모두 BackupException 으로 바꿔 "아무것도 안 바뀐 실패" 임을 알린다.
            // 파일을 고른 뒤 프로세스가 다시 만들어지면 읽기 권한이 사라져 SecurityException 이 난다.
            try {
                val input = context.contentResolver.openInputStream(source) ?: throw BackupException("파일을 열지 못했어요")
                input.use { copyLimited(it, incoming) }
            } catch (e: IOException) {
                throw BackupException("파일을 읽지 못했어요")
            } catch (e: SecurityException) {
                throw BackupException("파일을 읽을 권한이 없어요. 파일을 다시 골라 주세요")
            }
            // DB 를 닫기 전에 현재 스키마를 읽어 둔다.
            val (currentVersion, currentHash) = currentSchema()
            verify(incoming, currentVersion, currentHash)
            database.close()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            if (!incoming.renameTo(dbFile)) throw IOException("파일을 바꾸지 못했어요")
        } finally {
            incoming.delete()
        }
    }

    private fun copyLimited(input: InputStream, to: File) {
        to.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) throw BackupException("파일이 너무 커요. 이 앱에서 내보낸 파일이 맞는지 확인해 주세요")
                out.write(buffer, 0, n)
            }
        }
    }

    /** 지금 DB 의 (버전, 구조 지문). Room 의 드라이버 연결로 읽는다. */
    private suspend fun currentSchema(): Pair<Int, String?> = database.useReaderConnection { connection ->
        val version = connection.usePrepared("PRAGMA user_version") { if (it.step()) it.getLong(0).toInt() else 0 }
        val hash = connection.usePrepared(IDENTITY_HASH) { if (it.step()) it.getText(0) else null }
        version to hash
    }

    // 구조만 본다. 앱의 내보내기가 아니라 파일 관리자로 planner.db 본체만 복사한 파일도 통과하는데, 그런 파일은 최근 쓰기가 빠져 있을 수 있다.
    // ponytail: 지금은 DB 버전이 하나뿐이라 버전과 구조가 지금과 똑같은 파일만 받는다.
    // 버전 2 와 마이그레이션이 생기면 옛 버전 파일도 받아서 Room 이 다음 실행 때 올리도록 푼다.
    private fun verify(file: File, currentVersion: Int, currentHash: String?) {
        val header = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { it.read(header) }
        if (!header.contentEquals(SQLITE_HEADER)) throw BackupException(NOT_OURS)
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                if (db.single("PRAGMA quick_check") != "ok") throw BackupException("파일이 손상됐어요")
                if (db.version > currentVersion) throw BackupException("더 새 버전의 앱에서 내보낸 파일이에요. 앱을 먼저 업데이트해 주세요")
                if (db.version != currentVersion || currentHash == null || db.single(IDENTITY_HASH) != currentHash) throw BackupException(NOT_OURS)
            }
        } catch (e: SQLiteException) {
            throw BackupException(NOT_OURS)
        }
    }

    private fun SQLiteDatabase.single(sql: String): String? =
        rawQuery(sql, null).use { if (it.moveToFirst()) it.getString(0) else null }

    private companion object {
        const val MAX_BYTES = 50L * 1024 * 1024
        const val NOT_OURS = "이 앱에서 내보낸 파일이 아니에요"

        /** Room 이 테이블 구조로 만든 지문. 같은 구조의 DB 끼리만 같다. */
        const val IDENTITY_HASH = "SELECT identity_hash FROM room_master_table WHERE id = 42"

        /** 모든 SQLite 파일의 첫 16바이트: "SQLite format 3" + 0. */
        val SQLITE_HEADER = "SQLite format 3".toByteArray(Charsets.US_ASCII) + 0
    }
}
