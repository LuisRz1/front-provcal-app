package com.sanna.provcalapp.ui.sanitary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.models.RevisionHistoryItem
import com.sanna.provcalapp.data.repository.SanitaryControlRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Policy Management Screen
 * Handles history loading, filtering, and next revision date
 */
class PolicyManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SanitaryControlRepository(application)

    private val _history = MutableLiveData<List<RevisionHistoryItem>>()
    val history: LiveData<List<RevisionHistoryItem>> = _history

    private val _nextRevisionDate = MutableLiveData<String?>()
    val nextRevisionDate: LiveData<String?> = _nextRevisionDate

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private var currentPolicyId: String? = null
    private var currentFilterMonths: Int? = null
    private var nextRevisionDateObject: java.util.Date? = null

    fun loadPolicyData(policyId: String) {
        currentPolicyId = policyId
        loadHistory(null)
        loadNextRevisionDate(policyId)
    }

    fun loadHistory(filterMonths: Int?) {
        val policyId = currentPolicyId ?: return
        currentFilterMonths = filterMonths

        _loading.value = true
        viewModelScope.launch {
            when (val result = repository.getRevisionHistory(policyId, filterMonths)) {
                is Result.Success -> {
                    _history.value = result.data
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

    private fun loadNextRevisionDate(policyId: String) {
        viewModelScope.launch {
            when (val result = repository.getNextRevisionDate(policyId)) {
                is Result.Success -> {
                    _nextRevisionDate.value = result.data
                    // Parse the date string to Date object for comparison
                    result.data?.let { dateString ->
                        try {
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "PE"))
                            nextRevisionDateObject = dateFormat.parse(dateString)
                        } catch (e: Exception) {
                            nextRevisionDateObject = null
                        }
                    }
                }
                is Result.Error -> {
                    // Silently fail for next revision date
                    _nextRevisionDate.value = null
                    nextRevisionDateObject = null
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun isBeforeScheduledDate(): Boolean {
        val scheduledDate = nextRevisionDateObject ?: return false
        val today = java.util.Date()
        return today.before(scheduledDate)
    }

    fun scheduleRevision() {
        val policyId = currentPolicyId ?: return
        val date = _nextRevisionDate.value ?: return

        viewModelScope.launch {
            when (val result = repository.scheduleRevision(policyId, date)) {
                is Result.Success -> {
                    _message.value = "Revisión confirmada para $date"
                    // Reload data
                    loadHistory(currentFilterMonths)
                }
                is Result.Error -> {
                    _message.value = result.message
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
