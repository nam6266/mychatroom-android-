package com.example.mychatroom.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mychatroom.data.Role
import com.example.mychatroom.data.Room
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.viewModel.RoomViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatRoomListScreen(
    sessionManager: SessionManager,
    roomViewModel: RoomViewModel = viewModel(),
    onJoinChatClick: (Room) -> Unit,
    onBackLoginCLick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val rooms by roomViewModel.rooms.observeAsState(emptyList())
    var backToLogin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val roleFlow = sessionManager.getUserRole()
    val role by roleFlow.collectAsState(initial = Role.MEMBER)
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
//            IconButton(onClick = { backToLogin = true }) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "Back",
//                    tint = MaterialTheme.colorScheme.onBackground
//                )
//            }
            Text(
                text = "Chat Rooms",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(rooms) { room ->
                RoomItem(room, onJoinChatClick = onJoinChatClick)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (role == Role.ADMIN) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                onClick = { showDialog = true }
            ) {
                Text("Create room", color = MaterialTheme.colorScheme.onBackground)
            }
        }

//        if (showdialog) {
//            BasicAlertDialog(onDismissRequest = { showdialog = false }) {
//                Surface(
//                    modifier = Modifier.wrapContentWidth().wrapContentHeight(),
//                    shape = MaterialTheme.shapes.large,
//                    tonalElevation = AlertDialogDefaults.TonalElevation
//                ) {
//                    Text("Create a new room")
//                    Spacer(Modifier.height(8.dp))
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        OutlinedTextField(
//                            value = name,
//                            onValueChange = { name = it },
//                            label = { Text("name") },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(8.dp)
//                        )
//
//                        Row(modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceEvenly) {
//                            Button(onClick = {}) {
//                                Text("Create")
//                            }
//
//                            Button(onClick = {}) {
//                                Text("Cancel")
//                            }
//                        }
//                    }
//                }
//            }
//        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {},
                dismissButton = {},
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Create a new room",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = {
                                Text(
                                    "Name",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    roomViewModel.createRoom(name)
                                    showDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create", color = MaterialTheme.colorScheme.onBackground)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { showDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                },
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            )
        }

        if (backToLogin) {
            AlertDialog(
                onDismissRequest = { backToLogin = false },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    onBackLoginCLick()

                                    sessionManager.clearSession()
                                    backToLogin = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Log Out", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Return to Login Screen",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
fun RoomItem(room: Room, onJoinChatClick: (Room) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = room.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        OutlinedButton(
            onClick = { onJoinChatClick(room) }
        ) {
            Text("Join", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}