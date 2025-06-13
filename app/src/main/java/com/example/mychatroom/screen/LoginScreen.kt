package com.example.mychatroom.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.User
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.viewModel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    LoginClick: () -> Unit,
    SignUpClick: () -> Unit,
    sessionManager: SessionManager
) {


    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    val result by authViewModel.authResultLogin.observeAsState()
    var errorMess: Exception? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        authViewModel.clearLoginResult()
    }

    LaunchedEffect(result) {
        when (result) {
            is Results.Success -> {
                val user = (result as Results.Success<Pair<Boolean, User>>).data.second
                sessionManager.saveUserSession(user)
                LoginClick()
            }

            is Results.Error -> {
                errorMess = (result as Results.Error).exception
                showDialog = true
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("email", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("password", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = {
                authViewModel.login(
                    email.trimEnd(Char::isWhitespace),
                    password.trimEnd(Char::isWhitespace)
                )
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Login", color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Dont have an account? Sign up.",
            modifier = Modifier.clickable { SignUpClick() },
            color = MaterialTheme.colorScheme.onBackground
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onBackground)
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
                            text = errorMess?.message ?: "An unexpected error occurred.",
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