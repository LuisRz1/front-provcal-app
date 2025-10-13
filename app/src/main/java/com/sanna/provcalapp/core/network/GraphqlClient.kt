package com.sanna.provcalapp.core.network

import android.content.ContentValues.TAG
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Graphql {
    const val ENDPOINT = "http://10.0.2.2:8000/graphql"

    var ACCESS_TOKEN: String? = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5NmIzNGEzMS1lZTc3LTQ1ZGItYTk3My02OWJkMGZhNDJkMmMiLCJlbWFpbCI6ImNvY2luYS5hbmFAcHJvdmNhbC5jb20iLCJyb2xlIjoiY29vayIsInR5cGUiOiJhY2Nlc3MiLCJleHAiOjE3NjAzNjA0NTgsImlhdCI6MTc2MDM1ODY1OH0.QXOCGtqentuEreImfw_Xebuwal80ByIXdPV82wx0hbM"
}

class GraphqlClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {
    private fun requestFor(query: String): Request {
        val media = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(mapOf("query" to query)).toRequestBody(media)
        return Request.Builder()
            .url(Graphql.ENDPOINT)
            .post(body)
            .apply { Graphql.ACCESS_TOKEN?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
    }

    private fun logErrorsIfAny(rawJson: String) {
        try {
            val base = gson.fromJson(rawJson, Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val errors = base["errors"] as? List<Map<String, Any?>>
            if (!errors.isNullOrEmpty()) {
                val msgs = errors.mapNotNull { it["message"]?.toString() }
                Log.e(TAG, "GraphQL errors: $msgs")
            }
        } catch (_: Throwable) { }
    }

    // ========== currentUser ==========
    fun currentUser(): CurrentUser? {
        fun call(query: String): Pair<CurrentUser?, String?> {
            val req = requestFor(query)
            return client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null to "HTTP ${resp.code}"
                val json = resp.body?.string() ?: return@use null to "empty"
                val base = try { gson.fromJson(json, Map::class.java) } catch (_: Throwable) { null }
                val errs = (base?.get("errors") as? List<*>)?.joinToString { (it as Map<*,*>)["message"].toString() }
                val env = gson.fromJson(json, CurrentUserEnvelope::class.java)
                (env.data?.currentUser) to errs
            }
        }

        // 1) currentUser
        val (cu, err1) = call("""query { currentUser { id fullName email role } }""")
        if (cu != null) return cu
        if (err1?.contains("Cannot query field 'currentUser'") == true) {
            // 2) me (fallback por si tu schema usa 'me')
            val (me, _) = call("""query { me { id fullName email role } }""")
            if (me != null) return me
        }
        // deja trazas en Logcat para entender el motivo
        Log.e("GraphqlClient", "currentUser() failed: $err1")
        return null
    }


    // ========== mySchedule (si lo usas) ==========
    fun mySchedule(): MyScheduleDto? {
        val q = """
            query {
              mySchedule {
                shiftType
                startTime
                endTime
                workingDaysNames
                lateToleranceMinutes
                totalHoursPerDay
              }
            }
        """.trimIndent()
        return client.newCall(requestFor(q)).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "HTTP ${resp.code} en mySchedule()")
                return@use null
            }
            val json = resp.body?.string() ?: return@use null
            logErrorsIfAny(json)
            val env = gson.fromJson(json, GqlEnvelopeMySchedule::class.java)
            env.data?.mySchedule
        }
    }

    // ========== asistencia de HOY (si la tienes en tu schema) ==========
    fun todayAttendance(): TodayAttendanceDto? {
        val q = """
            query {
              myAttendanceToday {
                checkInTime
                checkOutTime
                totalWorkedMinutes
              }
            }
        """.trimIndent()
        return client.newCall(requestFor(q)).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "HTTP ${resp.code} en todayAttendance()")
                return@use null
            }
            val json = resp.body?.string() ?: return@use null
            logErrorsIfAny(json)
            val env = gson.fromJson(json, GqlEnvelopeTodayAttendance::class.java)
            env.data?.myAttendanceToday
        }
    }

    // ====== checkIn: parámetros opcionales con fallback (Trujillo) ======
    fun checkIn(
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Double? = null
    ): CheckInPayload? {
        val lat = latitude ?: -8.1116778
        val lon = longitude ?: -79.0287578
        val acc = accuracy ?: 10.0

        val query = """
            mutation {
              checkIn(input: {
                latitude: $lat,
                longitude: $lon,
                accuracy: $acc
              }) {
                success
                message
                checkInTime
                isLate
                lateMinutes
                isHoliday
              }
            }
        """.trimIndent()

        val media = "application/json; charset=utf-8".toMediaType()
        val body  = gson.toJson(mapOf("query" to query)).toRequestBody(media)

        val req = Request.Builder()
            .url(Graphql.ENDPOINT)
            .post(body)
            .apply { Graphql.ACCESS_TOKEN?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            val env  = gson.fromJson(json, GqlEnvelopeCheckIn::class.java)
            env.data?.checkIn
        }
    }

    // ====== checkOut (envía lat/lon/accuracy y pide totalWorkHours) ======
    fun checkOut(
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Double? = null
    ): CheckOutPayload? {
        val lat  = latitude  ?: -8.1116778   // fallback Trujillo
        val lon  = longitude ?: -79.0287578
        val acc  = accuracy  ?: 10.0

        val query = """
        mutation {
          checkOut(input: {
            latitude: $lat,
            longitude: $lon,
            accuracy: $acc
          }) {
            success
            message
            checkOutTime
            totalWorkHours
            noBreaksRegistered
          }
        }
    """.trimIndent()

        val media = "application/json; charset=utf-8".toMediaType()
        val body  = gson.toJson(mapOf("query" to query)).toRequestBody(media)

        val req = Request.Builder()
            .url(Graphql.ENDPOINT)
            .post(body)
            .apply { Graphql.ACCESS_TOKEN?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            val env  = gson.fromJson(json, GqlEnvelopeCheckOut::class.java)
            env.data?.checkOut
        }
    }

}