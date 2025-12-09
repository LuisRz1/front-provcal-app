package com.sanna.provcalapp.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sanna.provcalapp.MonthlyMenuQuery
import com.sanna.provcalapp.data.models.Result
import com.sanna.provcalapp.data.repository.MenuRepository
import com.sanna.provcalapp.type.MenuChangeItemInput
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MenuRepository(application)

    private val _menu = MutableLiveData<MonthlyMenuQuery.Menu?>()
    val menu: LiveData<MonthlyMenuQuery.Menu?> = _menu

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadMenu(year: Int, month: Int) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.getMonthlyMenu(year, month)) {
                is Result.Success -> {
                    _menu.value = r.data
                    _message.value = null
                }
                is Result.Error -> {
                    _message.value = r.message
                }
                is Result.Loading -> { }
            }
            _loading.value = false
        }
    }

    fun uploadMenu(
        year: Int,
        month: Int,
        filename: String,
        base64: String,
        overwrite: Boolean,
        onResult: (conflict: Boolean) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.uploadMonthlyMenu(year, month, filename, base64, overwrite)) {
                is Result.Success -> {
                    _message.value = r.data
                    loadMenu(year, month)
                    onResult(false)
                }
                is Result.Error -> {
                    if (r.message.startsWith("conflict:")) {
                        onResult(true)
                    } else {
                        _message.value = r.message
                        onResult(false)
                    }
                }
                is Result.Loading -> { }
            }
            _loading.value = false
        }
    }

    fun proposeChanges(items: List<MenuChangeItemInput>, onDone: (Int) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.proposeMenuChange(items)) {
                is Result.Success -> {
                    _message.value = "Cambios guardados exitosamente"
                    onDone(r.data)
                }
                is Result.Error -> {
                    _message.value = r.message
                }
                is Result.Loading -> { }
            }
            _loading.value = false
        }
    }
}