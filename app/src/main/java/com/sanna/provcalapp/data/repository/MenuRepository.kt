package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.*
import com.sanna.provcalapp.type.*

import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.remote.ApolloClientProvider

class MenuRepository(context: Context) {

    private val apolloClient: ApolloClient = ApolloClientProvider.getInstance(context)

    suspend fun getMonthlyMenu(year: Int, month: Int): Result<MonthlyMenuQuery.Menu> {
        return try {
            val resp = apolloClient.query(MonthlyMenuQuery(year, month)).execute()
            if (resp.hasErrors()) {
                Result.Error(resp.errors?.firstOrNull()?.message ?: "Error al obtener menú")
            } else {
                resp.data?.menu?.let { Result.Success(it) } ?: Result.Error("Sin datos de menú")
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    /**
     * Sube el menú mensual leyendo el Excel que viene en base64.
     *
     * El backend nuevo:
     *  - Siempre hace upsert del menú del mes.
     *  - Devuelve status "ok" en caso de éxito.
     *  - Ya no devuelve "conflict", así que no intentamos confirmar overwrite aquí.
     */
    suspend fun uploadMonthlyMenu(
        year: Int,
        month: Int,
        filename: String,
        fileBase64: String,
        overwriteIfConflict: Boolean
    ): Result<String> {
        return try {
            val input = UploadMonthlyMenuInput(
                year = year,
                month = month,
                filename = filename,
                fileBase64 = fileBase64
            )

            val resp = apolloClient.mutation(UploadMonthlyMenuMutation(input)).execute()

            if (resp.hasErrors()) {
                Result.Error(resp.errors?.firstOrNull()?.message ?: "Error subiendo menú")
            } else {
                val status = resp.data?.uploadMonthlyMenu?.status ?: "error"
                val message = resp.data?.uploadMonthlyMenu?.message ?: ""

                if (status == "ok") {
                    Result.Success(message.ifEmpty { "Menú cargado correctamente" })
                } else {
                    Result.Error(message.ifEmpty { "No se pudo subir el menú" })
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    suspend fun proposeMenuChange(items: List<MenuChangeItemInput>): Result<Int> {
        return try {
            val input = ProposeMenuChangeInput(items = items)
            val resp = apolloClient.mutation(ProposeMenuChangeMutation(input)).execute()
            if (resp.hasErrors()) {
                Result.Error(resp.errors?.firstOrNull()?.message ?: "Error proponiendo cambios")
            } else {
                Result.Success(resp.data?.proposeMenuChange?.size ?: 0)
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }
}
