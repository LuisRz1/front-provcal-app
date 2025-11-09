package com.sanna.provcalapp.data.models

data class AttendanceState(
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val totalHours: String? = null,
    val isCheckedIn: Boolean = false,
    val isOnBreak: Boolean = false,
    val isLate: Boolean = false,
    val lateMinutes: Int = 0
)

data class WorkSchedule(
    val shiftType: String,
    val startTime: String,
    val endTime: String,
    val workingDays: List<String>,
    val totalHoursPerDay: Double,
    val lateToleranceMinutes: Int,
    val breakDurationMinutes: Int
)