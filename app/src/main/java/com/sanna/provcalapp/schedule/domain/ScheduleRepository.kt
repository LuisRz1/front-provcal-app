package com.sanna.provcalapp.feature.schedule.domain

import com.sanna.provcalapp.feature.schedule.domain.model.MySchedule

interface ScheduleRepository {
    suspend fun getMySchedule(): MySchedule
}