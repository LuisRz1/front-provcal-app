package com.sanna.provcalapp.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.models.WorkSchedule
import com.sanna.provcalapp.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository(application)

    private val _schedule = MutableLiveData<WorkSchedule>()
    val schedule: LiveData<WorkSchedule> = _schedule

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = scheduleRepository.getMySchedule()) {
                is Result.Success -> {
                    _schedule.value = result.data
                    _error.value = null
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }
}