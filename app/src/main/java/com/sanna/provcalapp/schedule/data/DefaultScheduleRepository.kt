package com.sanna.provcalapp.feature.schedule.data

import com.sanna.provcalapp.feature.schedule.domain.ScheduleRepository
import com.sanna.provcalapp.feature.schedule.domain.model.MySchedule

class DefaultScheduleRepository(
    private val remote: ScheduleRemoteDataSource = ScheduleRemoteDataSource()
) : ScheduleRepository {
    override suspend fun getMySchedule(): MySchedule = remote.fetchMySchedule()
}
