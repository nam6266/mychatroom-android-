package com.example.mychatroom.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mychatroom.Injection
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.Room
import com.example.mychatroom.repository.RoomRepository
import kotlinx.coroutines.launch

class RoomViewModel : ViewModel() {
    private val roomRepository: RoomRepository = RoomRepository(Injection.instance())
    private val _rooms = MutableLiveData<List<Room>>()
    val rooms: LiveData<List<Room>> get() = _rooms

    init {
        loadRooms()
    }

    fun createRoom(name: String) {
        viewModelScope.launch {
            val result = roomRepository.createRoom(name)
            if (result is Results.Success) {
                loadRooms() // ✅ Refresh the list after adding
            } else {
                // optionally handle error
            }
        }
    }

    private fun loadRooms() {
        viewModelScope.launch {
            when (val results = roomRepository.getRoom()) {
                is Results.Success -> _rooms.value = results.data
                is Results.Error -> {

                }
            }
        }
    }
}