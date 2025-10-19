package com.sanna.provcalapp.data.models

data class CheckOutData(
    val attendanceId: String?,
    val checkOutTime: String?,
    val totalWorkHours: Double?,
    val noBreaksRegistered: Boolean?,
    val message: String
)
