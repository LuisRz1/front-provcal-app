package com.sanna.provcalapp.ui.sanitary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.data.models.HealthRevision
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.repository.SanitaryControlRepository
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Revision Form
 */
class RevisionFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SanitaryControlRepository(application)

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun submitRevision(
        policyId: String,
        isConforme: Boolean,
        comment: String,
        incidentTypeId: String?,
        companyId: String?
    ) {
        viewModelScope.launch {
            _loading.value = true

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val input = com.sanna.provcalapp.data.models.SubmitRevisionInput(
                policyId = policyId,
                date = currentDate,
                isConforme = isConforme,
                observation = comment.ifBlank { null },
                incidentTypeId = incidentTypeId,
                companyId = companyId
            )

            when (val result = repository.submitRevision(input)) {
                is Result.Success -> {
                    _saveSuccess.value = true
                }
                is Result.Error -> {
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
