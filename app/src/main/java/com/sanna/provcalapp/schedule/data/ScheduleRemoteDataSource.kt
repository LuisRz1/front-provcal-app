package com.sanna.provcalapp.feature.schedule.data

import com.sanna.provcalapp.core.network.GraphqlClient
import com.sanna.provcalapp.feature.schedule.domain.model.MySchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRemoteDataSource(
    private val gql: GraphqlClient = GraphqlClient()
) {
    suspend fun fetchMySchedule(): MySchedule = withContext(Dispatchers.IO) {
        val dto = gql.mySchedule()
            ?: error("mySchedule() devolvió null (revisa token/servidor)")
        MySchedule(
            shiftType = dto.shiftType,
            startTime = dto.startTime,
            endTime   = dto.endTime,
            workingDaysNames = dto.workingDaysNames,
            lateToleranceMinutes = dto.lateToleranceMinutes,
            totalHoursPerDay = dto.totalHoursPerDay
        )
    }
}
