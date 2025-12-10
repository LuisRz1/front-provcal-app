package com.sanna.provcalapp.ui.sanitary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.Company
import com.sanna.provcalapp.data.models.IncidentType
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.models.SubmitRevisionInput
import com.sanna.provcalapp.data.repository.SanitaryControlRepository
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Incident Report Form
 */
class IncidentReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SanitaryControlRepository(application)

    private val _incidentTypes = MutableLiveData<List<IncidentType>>()
    val incidentTypes: LiveData<List<IncidentType>> = _incidentTypes

    private val _companies = MutableLiveData<List<Company>>()
    val companies: LiveData<List<Company>> = _companies

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private var currentPolicyId: String? = null

    fun loadIncidentTypes(policyId: String = currentPolicyId ?: "") {
        currentPolicyId = policyId
        viewModelScope.launch {
            when (val result = repository.getIncidentTypes(policyId)) {
                is Result.Success -> {
                    _incidentTypes.value = result.data
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun loadCompanies() {
        viewModelScope.launch {
            when (val result = repository.getCompanies()) {
                is Result.Success -> {
                    _companies.value = result.data
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun getIncidentTypeId(name: String): String? {
        return _incidentTypes.value?.find { it.name == name }?.id
    }

    fun getCompanyId(socialReason: String): String? {
        return _companies.value?.find { it.socialReason == socialReason }?.id
    }

    fun submitIncidentReport(
        policyId: String,
        isConforme: Boolean,
        observation: String,
        incidentTypeId: String?,
        companyId: String?
    ) {
        viewModelScope.launch {
            _loading.value = true

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // Debug logging
            android.util.Log.d("IncidentReportVM", "=== SUBMITTING REVISION ===")
            android.util.Log.d("IncidentReportVM", "Policy ID: $policyId")
            android.util.Log.d("IncidentReportVM", "Incident Type ID: $incidentTypeId")
            android.util.Log.d("IncidentReportVM", "Company ID: $companyId")
            android.util.Log.d("IncidentReportVM", "Is Conforme: $isConforme")

            // Log loaded incident types for comparison
            _incidentTypes.value?.forEach { type ->
                android.util.Log.d("IncidentReportVM", "Available incident type: ${type.id} -> ${type.name} (policy: ${type.policyId})")
            }

            val input = SubmitRevisionInput(
                policyId = policyId,
                date = currentDate,
                isConforme = isConforme,
                observation = observation.ifBlank { null },
                incidentTypeId = incidentTypeId,
                companyId = companyId
            )

            when (val result = repository.submitRevision(input)) {
                is Result.Success -> {
                    _saveSuccess.value = true
                }
                is Result.Error -> {
                    android.util.Log.e("IncidentReportVM", "Submission error: ${result.message}")
                    _errorMessage.value = result.message
                }
                is Result.Loading -> {
                    // Already handling loading
                }
            }

            _loading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
