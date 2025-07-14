package com.example.mychatroom.viewModel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState.Initial)
    val uiState: StateFlow<CameraUiState> get() = _uiState

    private val reader = MultiFormatReader().apply {
        val map = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        setHints(map)
    }

    private lateinit var user: User

    init {

    }

    private fun initCamera() {
        viewModelScope.launch {

        }
    }
}

sealed interface CameraUiState {
    data object Initial : CameraUiState
    data class Ready(
        val user: User,
        val lastPicture: File?,
        val throwable: Throwable? = null,
        val qrCodeText: String? = null,
    ) : CameraUiState
}

@Immutable
data class User(
    val usePinchToZoom: Boolean,
    val useTapToFocus: Boolean,
    val useCamFront: Boolean,
)