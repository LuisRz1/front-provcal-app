package com.sanna.provcalapp.ui.sanitary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.HealthPolicy
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.repository.SanitaryControlRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Sanitary Control Menu
 * Manages loading and displaying health policies
 */
class SanitaryControlViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SanitaryControlRepository(application)

    private val _policies = MutableLiveData<List<HealthPolicy>>()
    val policies: LiveData<List<HealthPolicy>> = _policies

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        loadPolicies()
    }

    fun loadPolicies() {
        _loading.value = true
        viewModelScope.launch {
            when (val result = repository.getPolicies()) {
                is Result.Success -> {
                    _policies.value = result.data
                    _message.value = null
                }
                is Result.Error -> {
                    _message.value = result.message
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
            _loading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
