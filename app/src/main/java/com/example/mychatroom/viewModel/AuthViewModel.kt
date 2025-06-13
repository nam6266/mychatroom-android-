package com.example.mychatroom.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mychatroom.Injection
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.User
import com.example.mychatroom.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val userRepository: UserRepository = UserRepository(
        FirebaseAuth.getInstance(),
        Injection.instance()
    )
    private val _authResultLogin = MutableLiveData<Results<Pair<Boolean, User>>?>()
    val authResultLogin: LiveData<Results<Pair<Boolean, User>>?> get() = _authResultLogin

    private val _authResultSignUp = MutableLiveData<Results<Pair<Boolean, User>>?>()
    val authResultSignUp: LiveData<Results<Pair<Boolean, User>>?> get() = _authResultSignUp

    fun signUp(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authResultSignUp.value = userRepository.signUp(email, password, firstName, lastName)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authResultLogin.value = userRepository.login(email, password)
        }
    }

    fun clearLoginResult() {
        _authResultLogin.value = null
        _authResultSignUp.value = null
    }
}