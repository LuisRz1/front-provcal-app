package com.sanna.provcalapp.data.remote

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.sanna.provcalapp.BuildConfig
import com.sanna.provcalapp.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object ApolloClientProvider {

    @Volatile
    private var instance: ApolloClient? = null

    fun getInstance(context: Context): ApolloClient {
        return instance ?: synchronized(this) {
            instance ?: buildApolloClient(context).also { instance = it }
        }
    }

    private fun buildApolloClient(context: Context): ApolloClient {
        val tokenManager = TokenManager(context)

        // Logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Auth interceptor
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()

            // Agregar token si existe
            tokenManager.getAccessToken()?.let { token ->
                request.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(request.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return ApolloClient.Builder()
            .serverUrl(BuildConfig.BASE_URL)
            .okHttpClient(okHttpClient)
            .build()
    }
}