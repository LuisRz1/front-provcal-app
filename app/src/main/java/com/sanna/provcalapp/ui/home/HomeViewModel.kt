package com.sanna.provcalapp.ui.home

import androidx.lifecycle.*
import com.sanna.provcalapp.core.network.CurrentUser
import com.sanna.provcalapp.core.network.GraphqlClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val gql = GraphqlClient()

    private val _user = MutableLiveData<CurrentUser?>()
    val user: LiveData<CurrentUser?> = _user

    fun loadUser() {
        viewModelScope.launch {
            val u = withContext(Dispatchers.IO) { gql.currentUser() }
            _user.value = u
        }
    }
}
