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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository: ChatRepository = ChatRepository(Injection.instance())
    private val userRepository: UserRepository = UserRepository(
        FirebaseAuth.getInstance(),
        Injection.instance()
    )

    private var roomId: String? = null

    // Only one Firestore listener at a time; replaced when the room changes.
    private var messagesJob: Job? = null

    private val _message = MutableLiveData<List<Message>>(emptyList())
    val message: LiveData<List<Message>> get() = _message

    private val _currentUser = MutableLiveData<User>()

    // One-off error text for the screen to show (then call clearError()).
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    init {
        loadCurrentUser()
    }

    fun setRoomId(roomId: String) {
        if (this.roomId == roomId && messagesJob?.isActive == true) return
        this.roomId = roomId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getChatMessage(roomId).collect { _message.value = it }
        }
    }

    fun sendMessage(text: String) {
        val user = _currentUser.value
        val room = roomId
        if (user == null || room == null) {
            _error.value = "Still loading your profile, please try again in a moment."
            if (user == null) loadCurrentUser()
            return
        }
        val message = Message(
            senderFirstName = user.firstName,
            senderId = user.email,
            text = text
        )
        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(room, message)) {
                is Results.Success -> Unit
                is Results.Error -> _error.value = "Message not sent: ${result.exception.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Results.Success -> _currentUser.value = result.data
                is Results.Error -> _error.value = "Couldn't load your profile: ${result.exception.message}"
            }
        }
    }
}
