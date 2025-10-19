package com.sanna.provcalapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.AttendanceState
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.repository.AttendanceRepository
import com.sanna.provcalapp.data.repository.AuthRepository
import com.sanna.provcalapp.utils.LocationProvider
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceRepository = AttendanceRepository(application)
    private val authRepository = AuthRepository(application)
    private val locationProvider = LocationProvider(application)
    private val attendanceStateManager = com.sanna.provcalapp.data.local.AttendanceStateManager(application)

    // Estado de la asistencia
    private val _attendanceState = MutableLiveData<AttendanceState>()
    val attendanceState: LiveData<AttendanceState> = _attendanceState

    // Loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Mensajes
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // Info del usuario
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _employeeId = MutableLiveData<String>()
    val employeeId: LiveData<String> = _employeeId

    init {
        loadUserInfo()
        // Load saved attendance state (supports night shifts across calendar days)
        val savedState = attendanceStateManager.loadState()
        if (savedState != null) {
            android.util.Log.d("HomeViewModel", "Restored attendance state: checkInTime=${savedState.checkInTime}, isCheckedIn=${savedState.isCheckedIn}")
            _attendanceState.value = savedState
        } else {
            // Initialize with default values if no saved state
            _attendanceState.value = AttendanceState()
        }
    }

    private fun loadUserInfo() {
        _userName.value = authRepository.getUserName() ?: "Usuario"
        _employeeId.value = authRepository.getEmployeeId() ?: "N/A"
    }

    fun checkIn() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtener ubicación
                val location = locationProvider.getCurrentLocation()

                // Hacer check-in
                when (val result = attendanceRepository.checkIn(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )) {
                    is Result.Success -> {
                        val checkInData = result.data
                        _message.value = checkInData.message

                        // Debug logging
                        android.util.Log.d("HomeViewModel", "CheckIn Success - AttendanceId: ${checkInData.attendanceId}")
                        android.util.Log.d("HomeViewModel", "CheckIn Success - Raw checkInTime: ${checkInData.checkInTime}")
                        val formattedTime = formatTime(checkInData.checkInTime)
                        android.util.Log.d("HomeViewModel", "CheckIn Success - Formatted time: $formattedTime")

                        // ✅ Actualizar estado con todos los datos
                        updateAttendanceState(
                            isCheckedIn = true,
                            checkInTime = formattedTime, // Formatear hora
                            isLate = checkInData.isLate,
                            lateMinutes = checkInData.lateMinutes,
                            attendanceId = checkInData.attendanceId
                        )
                    }
                    is Result.Error -> {
                        _message.value = result.message
                    }
                    else -> {}
                }
            } catch (e: SecurityException) {
                _message.value = "Se requieren permisos de ubicación"
            } catch (e: Exception) {
                _message.value = "Error al obtener ubicación: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper function for formatting the time
    private fun formatTime(isoDateTime: String?): String {
        android.util.Log.d("HomeViewModel", "formatTime() called with: $isoDateTime")

        if (isoDateTime == null) {
            android.util.Log.d("HomeViewModel", "formatTime() - isoDateTime is NULL, returning --:--")
            return "--:--"
        }

        return try {
            // Parse ISO 8601 with timezone offset (at: 2025-10-18T02:49:40.531139+00:00)
            val zonedDateTime = java.time.ZonedDateTime.parse(isoDateTime)

            // Convert to Peruvian time
            val peruZone = java.time.ZoneId.of("America/Lima")
            val peruDateTime = zonedDateTime.withZoneSameInstant(peruZone)

            // Format as HH:mm
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val formatted = peruDateTime.format(formatter)
            android.util.Log.d("HomeViewModel", "formatTime() - Successfully formatted: $formatted")
            formatted
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "formatTime() - Error parsing time: ${e.message}", e)
            "--:--"
        }
    }

    // Helper function to format total hours (from Double to HH:mm)
    private fun formatHours(hours: Double?): String {
        android.util.Log.d("HomeViewModel", "formatHours() called with: $hours")

        if (hours == null) {
            android.util.Log.d("HomeViewModel", "formatHours() - hours is NULL, returning --:--")
            return "--:--"
        }

        return try {
            val totalMinutes = (hours * 60).toInt()
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            val formatted = String.format("%02d:%02d", h, m)
            android.util.Log.d("HomeViewModel", "formatHours() - Input: $hours hours -> $totalMinutes total minutes -> $h hours and $m minutes -> Formatted: $formatted")
            formatted
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "formatHours() - Error formatting hours: ${e.message}", e)
            "--:--"
        }
    }

    fun checkOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val location = locationProvider.getCurrentLocation()

                when (val result = attendanceRepository.checkOut(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )) {
                    is Result.Success -> {
                        val checkOutData = result.data
                        _message.value = checkOutData.message

                        // Debug logging
                        android.util.Log.d("HomeViewModel", "CheckOut Success - Raw checkOutTime: ${checkOutData.checkOutTime}")
                        android.util.Log.d("HomeViewModel", "CheckOut Success - Raw totalWorkHours: ${checkOutData.totalWorkHours}")

                        val formattedCheckOutTime = formatTime(checkOutData.checkOutTime)
                        val formattedTotalHours = formatHours(checkOutData.totalWorkHours)
                        android.util.Log.d("HomeViewModel", "CheckOut Success - Formatted checkOutTime: $formattedCheckOutTime, totalHours: $formattedTotalHours")

                        // Update state with check-out time and total hours
                        updateAttendanceState(
                            isCheckedIn = false,
                            checkOutTime = formattedCheckOutTime,
                            totalHours = formattedTotalHours
                        )
                    }
                    is Result.Error -> {
                        _message.value = result.message
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startBreak() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("HomeViewModel", "Starting break - Current state: isCheckedIn=${_attendanceState.value?.isCheckedIn}, attendanceId=${attendanceStateManager.getAttendanceId()}")

                val location = locationProvider.getCurrentLocation()

                when (val result = attendanceRepository.startBreak(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )) {
                    is Result.Success -> {
                        android.util.Log.d("HomeViewModel", "Break started successfully: ${result.data}")
                        _message.value = result.data
                        updateAttendanceState(isOnBreak = true)
                    }
                    is Result.Error -> {
                        android.util.Log.e("HomeViewModel", "Error starting break: ${result.message}")
                        _message.value = "Error al iniciar descanso: ${result.message}"

                        // If the backend says we're not checked in, clear local state
                        if (result.message.contains("No has registrado tu entrada", ignoreCase = true)) {
                            android.util.Log.w("HomeViewModel", "Backend says not checked in - clearing local state")
                            clearAttendanceState()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception starting break: ${e.message}", e)
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun endBreak() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("HomeViewModel", "Ending break - Current state: isOnBreak=${_attendanceState.value?.isOnBreak}, attendanceId=${attendanceStateManager.getAttendanceId()}")

                val location = locationProvider.getCurrentLocation()

                when (val result = attendanceRepository.endBreak(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )) {
                    is Result.Success -> {
                        android.util.Log.d("HomeViewModel", "Break ended successfully: ${result.data}")
                        _message.value = result.data
                        updateAttendanceState(isOnBreak = false)
                    }
                    is Result.Error -> {
                        android.util.Log.e("HomeViewModel", "Error ending break: ${result.message}")
                        _message.value = "Error al finalizar descanso: ${result.message}"

                        // If the backend says we're not checked in, clear local state
                        if (result.message.contains("No has registrado tu entrada", ignoreCase = true)) {
                            android.util.Log.w("HomeViewModel", "Backend says not checked in - clearing local state")
                            clearAttendanceState()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception ending break: ${e.message}", e)
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateAttendanceState(
        isCheckedIn: Boolean? = null,
        isOnBreak: Boolean? = null,
        checkInTime: String? = null,
        checkOutTime: String? = null,
        totalHours: String? = null,
        isLate: Boolean? = null,
        lateMinutes: Int? = null,
        attendanceId: String? = null
    ) {
        val currentState = _attendanceState.value ?: AttendanceState()
        val newState = currentState.copy(
            isCheckedIn = isCheckedIn ?: currentState.isCheckedIn,
            isOnBreak = isOnBreak ?: currentState.isOnBreak,
            checkInTime = checkInTime ?: currentState.checkInTime,
            checkOutTime = checkOutTime ?: currentState.checkOutTime,
            totalHours = totalHours ?: currentState.totalHours,
            isLate = isLate ?: currentState.isLate,
            lateMinutes = lateMinutes ?: currentState.lateMinutes
        )
        android.util.Log.d("HomeViewModel", "updateAttendanceState() - New state: checkInTime=${newState.checkInTime}, isCheckedIn=${newState.isCheckedIn}")
        _attendanceState.value = newState

        // Save state to persist across app restarts (supports night shifts)
        attendanceStateManager.saveState(newState, attendanceId)
    }

    /**
     * Clears the saved attendance state.
     * Should be called when user logs out.
     */
    fun clearAttendanceState() {
        attendanceStateManager.clearState()
        _attendanceState.value = AttendanceState()
    }

    /**
     * Logs out the user by clearing tokens and attendance state
     */
    fun logout() {
        android.util.Log.d("HomeViewModel", "Logging out - clearing all state")
        // Clear attendance state
        clearAttendanceState()
        // Clear auth tokens
        authRepository.logout()
    }
}