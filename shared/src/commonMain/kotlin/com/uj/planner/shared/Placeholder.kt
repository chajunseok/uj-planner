package com.uj.planner.shared

/**
 * 빈 모듈이 아님을 알리는 표식. 다음 단계에서 data·ui 가 들어오면 지운다.
 *
 * 이 파일이 있는 이유는 하나. 이 커밋의 목적이 "코드 이식" 이 아니라 "플러그인 조합이
 * 성립하는가" 하나이기 때문이다. 둘을 한 커밋에 섞으면 실패했을 때 어느 쪽 문제인지 모른다.
 */
internal const val SHARED_MODULE_MARKER = "uj-planner-shared"
