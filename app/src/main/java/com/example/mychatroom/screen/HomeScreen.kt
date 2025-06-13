package com.example.mychatroom.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mychatroom.data.Post
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.viewModel.HomeVIewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    sessionManager: SessionManager,
    homeVIewModel: HomeVIewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    val name by sessionManager.getFirstName().collectAsState(initial = "")
    var showDialog by remember { mutableStateOf(false) }
    val posts by homeVIewModel.post.observeAsState(initial = emptyList())
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.DarkGray, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable { showDialog = true },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Hi ${name}, what are you thinking today",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(posts) { post ->
                        PostItem(post)
                    }
                }
            }
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Post", fontSize = 18.sp)
                            IconButton(onClick = { showDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(
                                    Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )

                            selectedImageUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        ImagePicker(onImagePicked = { uri ->
                            selectedImageUri = uri
                        })

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                name?.let { homeVIewModel.addPost(selectedImageUri, text, it) }
                                text = ""
                                showDialog = false
                            }
                        ) {
                            Text("Post")
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ImagePicker(onImagePicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it) }
    }

    Button(onClick = { launcher.launch("image/*") }) {
        Text("Pick Image")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PreviewPost() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PostItem(
                Post(
                    posterFistName = "Nam",
                    postMessage = "hailo from the admin, welcom to the fun comunity",
                    timestamp = 1745155857393
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostItem(post: Post) {
    var postLiked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val isDarkTheme = isSystemInDarkTheme()
            val iconColor = if (isDarkTheme) Color.White else Color.Black
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User",
                    tint = iconColor,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = post.posterFistName,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        fontSize = 16.sp
                    )
                    Text(
                        text = formatTimestamp(post.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post message
            Text(
                text = post.postMessage,
                fontSize = 16.sp,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Post image
            AsyncImage(
                model = post.postImgUrl,
                contentDescription = "Post Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        postLiked = !postLiked
                    }
                ) {
                    Icon(
                        imageVector = if (postLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (postLiked) Color.Blue else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (postLiked) "Liked" else "Like",
                        color = if (postLiked) Color.Blue else Color.Gray,
                        fontSize = 14.sp
                    )
                }

            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}


@RequiresApi(Build.VERSION_CODES.O)
private fun formatTimestamp(timestamp: Long): String {
    val messageDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val now = LocalDateTime.now()

    return when {
        isSameDay(messageDateTime, now) -> "today ${formatTime(messageDateTime)}"
        isSameDay(messageDateTime.plusDays(1), now) -> "yesterday ${formatTime(messageDateTime)}"
        else -> formatDate(messageDateTime)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun isSameDay(dateTime1: LocalDateTime, dateTime2: LocalDateTime): Boolean {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return dateTime1.format(formatter) == dateTime2.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return formatter.format(dateTime)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return formatter.format(dateTime)
}