package com.sanna.provcalapp.attendance.data

import com.sanna.provcalapp.core.network.GraphqlClient
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class AttendanceRepository(
    private val gql: GraphqlClient = GraphqlClient()
) {
    private val lima = ZoneId.of("America/Lima")
    private val fmt12 = DateTimeFormatter.ofPattern("hh:mm a", Locale("es", "PE"))

    fun doCheckIn(): String? {
        val payload = gql.checkIn() ?: return null
        val iso = payload.checkInTime ?: return null
        val zdt = ZonedDateTime.parse(iso).withZoneSameInstant(lima)
        return fmt12.format(zdt)
    }

    /** Devuelve (horaSalida, totalHHmm) o null si falla */
    fun doCheckOut(): Pair<String, String>? {
        val payload = gql.checkOut() ?: return null
        val iso = payload.checkOutTime ?: return null
        val out = ZonedDateTime.parse(iso).withZoneSameInstant(lima)
        val outStr = fmt12.format(out)

        val totalHours = payload.totalWorkHours
        val totalHHmm = if (totalHours != null) {
            val totalMinutes = (totalHours * 60.0).roundToInt()
            String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60)
        } else {
            ""
        }
        return outStr to totalHHmm
    }
}
