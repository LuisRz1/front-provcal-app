package com.sanna.provcalapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Utility class to monitor network connectivity status
 */
class NetworkMonitor(private val context: Context) {

    /**
     * Check if network is currently available
     * @return true if connected to network with internet capability, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected == true
        }
    }

    /**
     * Get user-friendly network status message
     * @return Descriptive message about network status
     */
    fun getNetworkStatusMessage(): String {
        return if (isNetworkAvailable()) {
            "Conectado a internet"
        } else {
            "Sin conexión a internet. Por favor, verifica tu conexión."
        }
    }
}
