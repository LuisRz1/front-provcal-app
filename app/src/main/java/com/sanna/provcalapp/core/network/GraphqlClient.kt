package com.sanna.provcalapp.core.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Graphql {
    const val ENDPOINT = "http://10.0.2.2:8000/graphql"

    var ACCESS_TOKEN: String? = "token_del_usuario"
} class GraphqlClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    fun currentUserId(): String? {
        val query = """query{ currentUser { id } }""".trimIndent()
        val media = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(mapOf("query" to query)).toRequestBody(media)

        val req = Request.Builder()
            .url(Graphql.ENDPOINT)
            .post(body)
            .apply { Graphql.ACCESS_TOKEN?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            val env = gson.fromJson(json, GqlEnvelopeCurrentUser::class.java)
            env.data?.currentUser?.id
        }
    }

    fun mySchedule(): MyScheduleDto? {
        val query = """
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

        val media = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(mapOf("query" to query)).toRequestBody(media)

        val req = Request.Builder()
            .url(Graphql.ENDPOINT)
            .post(body)
            .apply { Graphql.ACCESS_TOKEN?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            val env = gson.fromJson(json, GqlEnvelopeMySchedule::class.java)
            env.data?.mySchedule
        }
    }
}
