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

    init {
        loadPosts()
    }

    private val _post = MutableLiveData<List<Post>>()
    val post: LiveData<List<Post>> get() = _post

    private val imgListTemp = listOf(
        "https://i.postimg.cc/9F6SfNtV/348559168-143562455375458-6897804453317580134-n.jpg",
        "https://i.postimg.cc/tgSNssQP/mlem-meme.jpg"
    )
    private var num = 0;
    fun addPost(selectedImageUri: Uri?, message: String, posterFistName: String) {
        viewModelScope.launch {
            if (selectedImageUri != null) {
                when (val resultsImage = homeRepository.uploadImageToFirebase(selectedImageUri)) {
                    is Results.Error -> {
                        val result =
                            homeRepository.creatPost(message, posterFistName, imgListTemp[num])
                        if (result is Results.Success) {
                            loadPosts()
                        }
                        if (num > 1) num = 0 else num++
                    }

                    is Results.Success -> {
                        val result =
                            homeRepository.creatPost(message, posterFistName, resultsImage.data)
                        if (result is Results.Success) {
                            loadPosts()
                        }
                    }
                }
            }


        }
    }

    private fun loadPosts() {
        viewModelScope.launch() {
            when (val results = homeRepository.getPosts()) {
                is Results.Error -> Log.e("Error", " Load post failed")
                is Results.Success -> _post.value = results.data
            }
        }
    }
}