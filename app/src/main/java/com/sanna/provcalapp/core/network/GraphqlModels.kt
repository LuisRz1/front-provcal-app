package com.sanna.provcalapp.core.network

// ========= currentUser =========
data class CurrentUserEnvelope(val data: CurrentUserData?, val errors: List<GqlError>? = null)
data class CurrentUserData(val currentUser: CurrentUser?)
data class CurrentUser(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String
)

// ========= mySchedule (opcional) =========
data class MyScheduleData(val mySchedule: MyScheduleDto?)
data class MyScheduleDto(
    val shiftType: String,
    val startTime: String,
    val endTime: String,
    val workingDaysNames: List<String>,
    val lateToleranceMinutes: Int,
    val totalHoursPerDay: Double
)
data class GqlEnvelopeMySchedule(val data: MyScheduleData?, val errors: List<GqlError>? = null)

// ========= checkIn / checkOut =========
data class CheckInData(val checkIn: CheckInPayload?)
data class CheckOutData(val checkOut: CheckOutPayload?)

data class CheckInPayload(
    val success: Boolean,
    val message: String?,
    val checkInTime: String?          // ISO-8601
)

data class CheckOutPayload(
    val success: Boolean,
    val message: String?,
    val checkOutTime: String?,   // ISO-8601
    val totalWorkHours: Double?, // <-- OJO: horas en float, viene del backend
    val noBreaksRegistered: Boolean? = null
)

data class GqlEnvelopeCheckIn(
    val data: CheckInData? = null,
    val errors: List<GqlError>? = null
)

data class GqlEnvelopeCheckOut(
    val data: CheckOutData? = null,
    val errors: List<GqlError>? = null
)

// ========= asistencia HOY =========
data class TodayAttendanceDto(
    val checkInTime: String?,
    val checkOutTime: String?,
    val totalWorkedMinutes: Int?
)
data class TodayAttendanceData(val myAttendanceToday: TodayAttendanceDto?)
data class GqlEnvelopeTodayAttendance(
    val data: TodayAttendanceData?,
    val errors: List<GqlError>? = null
)

// ========= errores GraphQL =========
data class GqlError(val message: String? = null)
