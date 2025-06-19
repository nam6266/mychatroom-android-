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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val userRepository: UserRepository = UserRepository(
        FirebaseAuth.getInstance(),
        Injection.instance()
    )
    private val _authResultLogin = MutableLiveData<Results<Pair<Boolean, User>>?>()
    val authResultLogin: LiveData<Results<Pair<Boolean, User>>?> get() = _authResultLogin

    private val _authResultSignUp = MutableLiveData<Results<Pair<Boolean, User>>?>()
    val authResultSignUp: LiveData<Results<Pair<Boolean, User>>?> get() = _authResultSignUp

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> get() = _isLoading

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
        FirebaseAuth.getInstance().signOut()
        _authResultLogin.value = null
        _authResultSignUp.value = null
    }

    fun checkLoginSession() {
        _isLoading.value = true
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            viewModelScope.launch {
                try {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                    val user = snapshot.toObject(User::class.java)
                    user?.let {
                        _authResultLogin.value = Results.Success(Pair(true, it))
                    } ?: run {
                        _authResultLogin.value = Results.Error(Exception("User not found"))
                    }
                } catch (e: Exception) {
                    _authResultLogin.value = Results.Error(e)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _isLoading.value = false
        }
    }

}