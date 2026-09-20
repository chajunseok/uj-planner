package com.uj.planner.data

/**
 * 백업 파일을 받아들일 수 없는 이유. [message] 를 그대로 사용자에게 보여 준다.
 *
 * 파일을 실제로 읽고 쓰는 일은 플랫폼별이지만, "왜 거절했는지" 를 화면에 알리는 규칙은
 * 공용이라 이 타입만 여기 둔다.
 */
class BackupException(message: String) : Exception(message)
