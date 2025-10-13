package com.sanna.provcalapp.core.network

// currentUser
data class CurrentUserData(val currentUser: CurrentUser?)
data class CurrentUser(val id: String)
data class GqlEnvelopeCurrentUser(val data: CurrentUserData?)

// mySchedule
data class MyScheduleData(val mySchedule: MyScheduleDto?)
data class MyScheduleDto(
    val shiftType: String,
    val startTime: String,
    val endTime: String,
    val workingDaysNames: List<String>,
    val lateToleranceMinutes: Int,
    val totalHoursPerDay: Double
)
data class GqlEnvelopeMySchedule(val data: MyScheduleData?)
