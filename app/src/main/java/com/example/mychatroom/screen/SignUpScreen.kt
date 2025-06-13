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
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.mychatroom.viewModel.AuthViewModel

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigationToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    var showSignUpSucces by remember { mutableStateOf(false) }
    var showSignUpFalse by remember { mutableStateOf(false) }
    val result by authViewModel.authResultSignUp.observeAsState()
    var errorMess: Exception? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        authViewModel.clearLoginResult()
    }
    LaunchedEffect(result) {
        when (result) {
            is Results.Success -> {
                showSignUpSucces = true
            }

            is Results.Error -> {
                errorMess = (result as Results.Error).exception
                showSignUpFalse = true
            }

            else -> {}
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            visualTransformation = PasswordVisualTransformation()
        )


        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name", color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Button(onClick = {
            authViewModel.signUp(email, password, lastName, firstName)
            //  after signup reset all to null
            email = ""
            password = ""
            lastName = ""
            firstName = ""
        }) {
            Text("Sign Up", color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Already have an account? Sign in.",
            modifier = Modifier.clickable { onNavigationToLogin() },
            color = MaterialTheme.colorScheme.onBackground
        )

        if (showSignUpFalse) {
            AlertDialog(
                onDismissRequest = { showSignUpFalse = false },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showSignUpFalse = false },
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
                                color = MaterialTheme.colorScheme.onBackground
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


        if (showSignUpSucces) {
            AlertDialog(
                onDismissRequest = { showSignUpSucces = false },
                confirmButton = {}, // You can leave these empty if you're only using the custom button
                dismissButton = {},
                title = null,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Account created successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Would you like to go to the login screen now?",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Button(
                            onClick = onNavigationToLogin,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                        ) {
                            Text(
                                "Go to Login",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

