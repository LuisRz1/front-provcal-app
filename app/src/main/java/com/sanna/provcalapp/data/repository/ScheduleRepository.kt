package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.MyScheduleQuery
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.models.WorkSchedule
import com.sanna.provcalapp.data.remote.ApolloClientProvider

class ScheduleRepository(context: Context) {

    private val apolloClient: ApolloClient = ApolloClientProvider.getInstance(context)

    suspend fun getMySchedule(): Result<WorkSchedule> {
        return try {
            val response = apolloClient.query(MyScheduleQuery()).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al obtener horario")
            } else {
                val schedule = response.data?.mySchedule
                if (schedule != null) {
                    Result.Success(
                        WorkSchedule(
                            shiftType = schedule.shiftType,
                            startTime = schedule.startTime,
                            endTime = schedule.endTime,
                            workingDays = schedule.workingDaysNames,
                            totalHoursPerDay = schedule.totalHoursPerDay,
                            lateToleranceMinutes = schedule.lateToleranceMinutes,
                            breakDurationMinutes = schedule.breakDurationMinutes
                        )
                    )
                } else {
                    Result.Error("No tienes horario asignado")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }
}