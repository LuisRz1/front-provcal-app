package com.sanna.provcalapp.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

object LocationProvider {

    data class Coords(val lat: Double, val lon: Double)

    // Usa EXACTAMENTE los del backend por defecto (Trujillo + radio 100m)
    private val fallback = Coords(-8.1116778, -79.0287578)

    @SuppressLint("MissingPermission")
    fun getLastBestLocation(context: Context): Coords {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p)
                if (loc != null && (best == null || loc.time > best!!.time)) best = loc
            } catch (_: SecurityException) { /* sin permisos -> fallback */ }
        }
        return if (best != null) Coords(best!!.latitude, best!!.longitude) else fallback
    }
}
