package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.*
import com.sanna.provcalapp.data.models.CheckInData
import com.sanna.provcalapp.data.models.CheckOutData
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.remote.ApolloClientProvider

class AttendanceRepository(context: Context) {

    private val apolloClient: ApolloClient = ApolloClientProvider.getInstance(context)

    suspend fun checkIn(latitude: Double, longitude: Double, accuracy: Double): Result<CheckInData> {
        return try {
            val response = apolloClient.mutation(
                CheckInMutation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy
                )
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error en check-in")
            } else {
                val data = response.data?.checkIn
                if (data?.success == true) {
                    Result.Success(
                        CheckInData(
                            attendanceId = data.attendanceId,
                            checkInTime = data.checkInTime,
                            isLate = data.isLate,
                            lateMinutes = data.lateMinutes,
                            isHoliday = data.isHoliday,
                            message = data.message ?: "Check-in exitoso"
                        )
                    )
                } else {
                    Result.Error(data?.message ?: "Error desconocido")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    suspend fun checkOut(latitude: Double, longitude: Double, accuracy: Double): Result<CheckOutData> {
        return try {
            val response = apolloClient.mutation(
                CheckOutMutation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy
                )
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error en check-out")
            } else {
                val data = response.data?.checkOut
                if (data?.success == true) {
                    Result.Success(
                        CheckOutData(
                            attendanceId = data.attendanceId,
                            checkOutTime = data.checkOutTime,
                            totalWorkHours = data.totalWorkHours,
                            noBreaksRegistered = data.noBreaksRegistered,
                            message = data.message ?: "Check-out exitoso"
                        )
                    )
                } else {
                    Result.Error(data?.message ?: "Error desconocido")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    suspend fun startBreak(latitude: Double, longitude: Double, accuracy: Double): Result<String> {
        return try {
            val response = apolloClient.mutation(
                StartBreakMutation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy
                )
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al iniciar descanso")
            } else {
                val data = response.data?.startBreak
                if (data?.success == true) {
                    Result.Success(data.message ?: "Descanso iniciado")
                } else {
                    Result.Error(data?.message ?: "Error desconocido")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    suspend fun endBreak(latitude: Double, longitude: Double, accuracy: Double): Result<String> {
        return try {
            val response = apolloClient.mutation(
                EndBreakMutation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy
                )
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error al finalizar descanso")
            } else {
                val data = response.data?.endBreak
                if (data?.success == true) {
                    Result.Success(data.message ?: "Descanso finalizado")
                } else {
                    Result.Error(data?.message ?: "Error desconocido")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }
}