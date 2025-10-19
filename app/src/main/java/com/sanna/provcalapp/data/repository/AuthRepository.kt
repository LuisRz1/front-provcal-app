package com.sanna.provcalapp.data.repository

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.sanna.provcalapp.LoginMutation
import com.sanna.provcalapp.data.local.TokenManager
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.remote.ApolloClientProvider

class AuthRepository(private val context: Context) {

    private val apolloClient: ApolloClient = ApolloClientProvider.getInstance(context)
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            val response = apolloClient.mutation(
                LoginMutation(email = email, password = password)
            ).execute()

            if (response.hasErrors()) {
                Result.Error(response.errors?.firstOrNull()?.message ?: "Error en login")
            } else {
                val loginData = response.data?.login
                if (loginData != null) {
                    // Guardar tokens
                    tokenManager.saveTokens(
                        loginData.accessToken,
                        loginData.refreshToken
                    )

                    // Guardar info del usuario
                    tokenManager.saveUserInfo(
                        userId = loginData.user.id,
                        name = loginData.user.fullName,
                        role = loginData.user.role,
                        employeeId = loginData.user.employeeId
                    )

                    Result.Success(true)
                } else {
                    Result.Error("Respuesta vacía del servidor")
                }
            }
        } catch (e: ApolloException) {
            Result.Error("Error de conexión: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}", e)
        }
    }

    fun logout() {
        tokenManager.clearTokens()
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getUserName(): String? = tokenManager.getUserName()
    fun getEmployeeId(): String? = tokenManager.getEmployeeId()


}