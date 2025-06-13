package com.example.mychatroom.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mychatroom.Injection
import com.example.mychatroom.data.Message
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.User
import com.example.mychatroom.repository.ChatRepository
import com.example.mychatroom.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository: ChatRepository = ChatRepository(Injection.instance())
    private val userRepository: UserRepository = UserRepository(
        FirebaseAuth.getInstance(),
        Injection.instance()
    )

    init {
        loadCurrentUser()
    }

    private val _roomId = MutableLiveData<String>()

    private val _message = MutableLiveData<List<Message>>()
    val message: LiveData<List<Message>> get() = _message

    private val _currentUser = MutableLiveData<User>()

    fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getChatMessage(_roomId.value.toString())
                .collect { _message.value = it }
        }
    }

    fun sendMessage(text: String) {
        if (_currentUser.value != null) {
            val message = Message(
                senderFirstName = _currentUser.value!!.firstName,
                senderId = _currentUser.value!!.email,
                text = text
            )
            viewModelScope.launch {
                when (chatRepository.sendMessage(_roomId.value.toString(), message)) {
                    is Results.Success -> Unit
                    is Results.Error -> TODO()
                }
            }
        }
    }

    fun setRoomId(roomId: String) {
        _roomId.value = roomId
        loadMessages()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Results.Success -> _currentUser.value = result.data
                is Results.Error -> {
                    TODO()
                }
            }
        }
    }
}