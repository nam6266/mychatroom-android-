package com.example.mychatroom.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mychatroom.Injection
import com.example.mychatroom.data.Post
import com.example.mychatroom.data.Results
import com.example.mychatroom.repository.HomeRepository
import kotlinx.coroutines.launch

class HomeVIewModel : ViewModel() {

    private val homeRepository: HomeRepository =
        HomeRepository(Injection.instance(), Injection.firebaseStorageInstance())

    private val _post = MutableLiveData<List<Post>>(emptyList())
    val post: LiveData<List<Post>> get() = _post

    private val _isPosting = MutableLiveData(false)
    val isPosting: LiveData<Boolean> get() = _isPosting

    // Placeholder images used when the Firebase Storage upload fails.
    private val imgListTemp = listOf(
        "https://i.postimg.cc/9F6SfNtV/348559168-143562455375458-6897804453317580134-n.jpg",
        "https://i.postimg.cc/tgSNssQP/mlem-meme.jpg"
    )
    private var num = 0

    init {
        loadPosts()
    }

    fun addPost(selectedImageUri: Uri?, message: String, posterFistName: String) {
        if (message.isBlank() && selectedImageUri == null) return
        viewModelScope.launch {
            _isPosting.value = true
            val imageUrl: String? = if (selectedImageUri == null) {
                null // text-only post
            } else {
                when (val upload = homeRepository.uploadImageToFirebase(selectedImageUri)) {
                    is Results.Success -> upload.data
                    is Results.Error -> {
                        Log.e("HomeVIewModel", "Image upload failed", upload.exception)
                        imgListTemp[num++ % imgListTemp.size]
                    }
                }
            }
            when (val result = homeRepository.creatPost(message.trim(), posterFistName, imageUrl)) {
                is Results.Success -> loadPosts()
                is Results.Error -> Log.e("HomeVIewModel", "Create post failed", result.exception)
            }
            _isPosting.value = false
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            when (val results = homeRepository.getPosts()) {
                is Results.Error -> Log.e("HomeVIewModel", "Load posts failed", results.exception)
                is Results.Success -> _post.value = results.data
            }
        }
    }
}
