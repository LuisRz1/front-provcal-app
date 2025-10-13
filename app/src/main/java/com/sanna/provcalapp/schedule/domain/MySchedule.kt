package com.sanna.provcalapp.feature.schedule.domain.model

data class MySchedule(
    val shiftType: String,
    val startTime: String,
    val endTime: String,
    val workingDaysNames: List<String>,
    val lateToleranceMinutes: Int,
    val totalHoursPerDay: Double
)
