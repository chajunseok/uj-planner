package com.uj.planner

import android.app.Application
import com.uj.planner.data.PlannerDatabase
import com.uj.planner.data.PlannerRepository

/** DI 프레임워크 없이 여기서 직접 조립한다. 화면은 이 객체에서 의존성을 꺼내 생성자로 넘긴다. */
class PlannerApp : Application() {
    val repository: PlannerRepository by lazy { PlannerRepository(PlannerDatabase.create(this)) }
}
