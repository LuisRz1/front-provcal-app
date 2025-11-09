// En data/models/CheckInData.kt
package com.sanna.provcalapp.data.models

data class CheckInData(
    val attendanceId: String?,
    val checkInTime: String?,
    val isLate: Boolean,
    val lateMinutes: Int,
    val isHoliday: Boolean,
    val message: String
)