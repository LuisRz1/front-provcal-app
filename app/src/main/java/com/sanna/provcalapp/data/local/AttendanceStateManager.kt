package com.sanna.provcalapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.sanna.provcalapp.data.models.AttendanceState

/**
 * Manages local persistence of attendance state.
 *
 * Handles both regular and night shifts:
 * - Regular shifts: State is valid within the same calendar day
 * - Night shifts: State persists across calendar days using attendanceId
 *
 * State is cleared when:
 * - User checks out (completes the attendance session)
 * - User explicitly logs out
 * - More than 24 hours have passed since check-in (safety measure)
 */
class AttendanceStateManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "attendance_state",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_ATTENDANCE_ID = "attendance_id"
        private const val KEY_CHECK_IN_TIMESTAMP = "check_in_timestamp"
        private const val KEY_CHECK_IN_TIME = "check_in_time"
        private const val KEY_CHECK_OUT_TIME = "check_out_time"
        private const val KEY_TOTAL_HOURS = "total_hours"
        private const val KEY_IS_CHECKED_IN = "is_checked_in"
        private const val KEY_IS_ON_BREAK = "is_on_break"
        private const val KEY_IS_LATE = "is_late"
        private const val KEY_LATE_MINUTES = "late_minutes"

        // Maximum time to keep a "checked in" state (24 hours in milliseconds)
        private const val MAX_SESSION_DURATION_MS = 24 * 60 * 60 * 1000L
    }

    /**
     * Saves the current attendance state with attendance ID and timestamp
     */
    fun saveState(state: AttendanceState, attendanceId: String? = null) {
        prefs.edit().apply {
            // Save attendance ID if provided (from check-in response)
            if (attendanceId != null) {
                putString(KEY_ATTENDANCE_ID, attendanceId)
            }

            // Save check-in timestamp only when first checking in
            if (state.isCheckedIn && !prefs.getBoolean(KEY_IS_CHECKED_IN, false)) {
                putLong(KEY_CHECK_IN_TIMESTAMP, System.currentTimeMillis())
            }

            // Save all state fields
            putString(KEY_CHECK_IN_TIME, state.checkInTime)
            putString(KEY_CHECK_OUT_TIME, state.checkOutTime)
            putString(KEY_TOTAL_HOURS, state.totalHours)
            putBoolean(KEY_IS_CHECKED_IN, state.isCheckedIn)
            putBoolean(KEY_IS_ON_BREAK, state.isOnBreak)
            putBoolean(KEY_IS_LATE, state.isLate)
            putInt(KEY_LATE_MINUTES, state.lateMinutes)

            // If user checked out, clear the session
            if (!state.isCheckedIn) {
                remove(KEY_ATTENDANCE_ID)
                remove(KEY_CHECK_IN_TIMESTAMP)
            }

            apply()
        }
    }

    /**
     * Loads the attendance state if it's still valid.
     * Returns null if:
     * - No state exists
     * - More than 24 hours have passed since check-in (expired session)
     */
    fun loadState(): AttendanceState? {
        val isCheckedIn = prefs.getBoolean(KEY_IS_CHECKED_IN, false)

        // If not checked in, no state to restore
        if (!isCheckedIn) {
            return null
        }

        // Check if session has expired (more than 24 hours since check-in)
        val checkInTimestamp = prefs.getLong(KEY_CHECK_IN_TIMESTAMP, 0L)
        if (checkInTimestamp > 0) {
            val elapsedTime = System.currentTimeMillis() - checkInTimestamp
            if (elapsedTime > MAX_SESSION_DURATION_MS) {
                // Session expired, clear state
                android.util.Log.w("AttendanceStateManager", "Attendance session expired (>24h), clearing state")
                clearState()
                return null
            }
        }

        // Return saved state (valid for current attendance session)
        return AttendanceState(
            checkInTime = prefs.getString(KEY_CHECK_IN_TIME, null),
            checkOutTime = prefs.getString(KEY_CHECK_OUT_TIME, null),
            totalHours = prefs.getString(KEY_TOTAL_HOURS, null),
            isCheckedIn = isCheckedIn,
            isOnBreak = prefs.getBoolean(KEY_IS_ON_BREAK, false),
            isLate = prefs.getBoolean(KEY_IS_LATE, false),
            lateMinutes = prefs.getInt(KEY_LATE_MINUTES, 0)
        )
    }

    /**
     * Gets the saved attendance ID for the current session
     */
    fun getAttendanceId(): String? {
        return prefs.getString(KEY_ATTENDANCE_ID, null)
    }

    /**
     * Clears all saved attendance state
     */
    fun clearState() {
        prefs.edit().clear().apply()
    }
}
