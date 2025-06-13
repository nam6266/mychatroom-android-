package com.example.mychatroom.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _data = MutableLiveData(1)
    val data: LiveData<Int> get() = _data

    fun initGame() {
        viewModelScope.launch {
            while (true) {
                delay(16L)
                _data.value = _data.value?.plus(1)
            }

        }
    }
}