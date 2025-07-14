package com.example.mychatroom.screen

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.mychatroom.data.Results
import com.example.mychatroom.data.User
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.viewModel.AuthViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    LoginClick: () -> Unit,
    SignUpClick: () -> Unit,
    sessionManager: SessionManager
) {

    val context = LocalContext.current
    // Login Focus
    val passwordFocusRequester = FocusRequester()

    // Sign up Focus
    val passwordSignUpFocusRequester = FocusRequester()
    val firstNameFocusRequester = FocusRequester()
    val lastNameFocusRequester = FocusRequester()


    val focusManager = LocalFocusManager.current
    val exoPlayer = remember { context.buildExoPlayer(getVideoUri(context)) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    val loginResult by authViewModel.authResultLogin.observeAsState()
    val signUpResult by authViewModel.authResultSignUp.observeAsState()
    var errorMess: Exception? by remember { mutableStateOf(null) }
    var showSignUpSucces by remember { mutableStateOf(false) }
    var showSignUpFalse by remember { mutableStateOf(false) }
    var showLoginFalse by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false)}

    val animationScope = rememberCoroutineScope()
    var screenWidthPx by remember { mutableStateOf(0f) }
    val transitionProgress = remember { Animatable(0f) }
    var isLogin by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        authViewModel.clearLoginResult()
    }

    LaunchedEffect(loginResult) {
        when (loginResult) {
            is Results.Success -> {
                val user = (loginResult as Results.Success<Pair<Boolean, User>>).data.second
                sessionManager.saveUserSession(user)
                LoginClick()
            }

            is Results.Error -> {
                errorMess = (loginResult as Results.Error).exception
                showLoginFalse = true
            }

            else -> {}
        }
    }

    LaunchedEffect(signUpResult) {
        when (signUpResult) {
            is Results.Success -> {
                showSignUpSucces = true
            }

            is Results.Error -> {
                errorMess = (signUpResult as Results.Error).exception
                showSignUpFalse = true
            }

            else -> {}
        }
    }

    AndroidView(
        factory = { it.buildPlayerView(exoPlayer) },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    ProvideWindowInsets {
        Column(
            Modifier
                .navigationBarsWithImePadding()

                .fillMaxSize()
                .onSizeChanged {
                    intSize -> screenWidthPx = intSize.width.toFloat()
                    Log.d("myapp","screenWidthPx : $screenWidthPx")
                               },
            verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box() {
                // login section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .graphicsLayer {
                            this.translationX = -transitionProgress.value * screenWidthPx
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextInput(
                        value = email,
                        onValueChange = { email = it },
                        InputType.Name,
                        keyboardActions = KeyboardActions(onNext = {
                            passwordFocusRequester.requestFocus()
                        })
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextInput(
                        value = password,
                        onValueChange = { password = it },
                        InputType.Password,
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        focusRequester = passwordFocusRequester
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            authViewModel.login(
                                email.trimEnd(Char::isWhitespace),
                                password.trimEnd(Char::isWhitespace)
                            )
                        }, modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("SIGN IN", Modifier.padding(vertical = 8.dp))
                    }
                }

                // signup section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .graphicsLayer {
                            translationX = (1f - transitionProgress.value) * screenWidthPx
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextInput(
                        value = email,
                        onValueChange = { email = it },
                        inputType = InputType.Name,
                        keyboardActions = KeyboardActions(onNext = {
                            passwordSignUpFocusRequester.requestFocus()
                        })
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextInput(
                        value = password,
                        onValueChange = { password = it },
                        inputType =  InputType.Password,
                        keyboardActions = KeyboardActions(onDone = {
                            firstNameFocusRequester.requestFocus()
                        }),
                        focusRequester = passwordSignUpFocusRequester
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextInput(
                        value = firstName,
                        onValueChange = { firstName = it },
                        inputType = InputType.Name,
                        keyboardActions = KeyboardActions(onDone = {
                            lastNameFocusRequester.requestFocus()
                        }),
                        focusRequester = firstNameFocusRequester
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextInput(
                        value = lastName,
                        onValueChange = { lastName = it },
                        inputType = InputType.Name,
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        focusRequester = lastNameFocusRequester
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            authViewModel.signUp(email, password, lastName, firstName)
                            //  after signup reset all to null
                            email = ""
                            password = ""
                            lastName = ""
                            firstName = ""
                        }, modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("SIGN UP", Modifier.padding(vertical = 8.dp))
                    }
                }
            }
            Divider(
                color = Color.White.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 48.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Forgot your password?", color = Color.White)
                TextButton(onClick = {
                    showForgotPassword = true
                }) {
                    Text("CLICK HERE")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLogin) {
                    Text("Don't have an account?", color = Color.White)
                }else {
                    Text("Allready had an account?", color = Color.White)
                }
                TextButton(onClick = {
                    password = "" // Clear the password field
                    animationScope.launch {
                        val targetValue = if (isLogin) 1f else 0f
                        transitionProgress.animateTo(
                            targetValue = targetValue,
                            animationSpec = tween(durationMillis = 500)
                        )
                        isLogin = !isLogin
                    }
                }) {
                    if (isLogin) {
                        Text("SIGN UP")
                    }else {
                        Text("LOGIN")
                    }
                }
            }
        }
    }

    if (showLoginFalse) {
        AlertDialog(
            onDismissRequest = { showLoginFalse = false },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showLoginFalse = false },
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
                        onClick = {
                            password = "" // Clear the password field
                            animationScope.launch {
                                val targetValue = if (isLogin) 1f else 0f
                                transitionProgress.animateTo(
                                    targetValue = targetValue,
                                    animationSpec = tween(durationMillis = 500)
                                )
                                isLogin = !isLogin
                                showSignUpSucces = false
                            }
                        },
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
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showForgotPassword = false },
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
                        text = "I will do this when I no longer lazy",
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
}

sealed class InputType(
    val label: String,
    val icon: ImageVector,
    val keyboardOptions: KeyboardOptions,
    val visualTransformation: VisualTransformation
) {
    object Name : InputType(
        label = "Username",
        icon = Icons.Default.Person,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        visualTransformation = VisualTransformation.None
    )

    object Password : InputType(
        label = "Password",
        icon = Icons.Default.Lock,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password
        ),
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    inputType: InputType,
    focusRequester: FocusRequester? = null,
    keyboardActions: KeyboardActions
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester ?: FocusRequester()),
        leadingIcon = { Icon(imageVector = inputType.icon, null) },
        label = { Text(text = inputType.label) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Black,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(32.dp),
        keyboardOptions = inputType.keyboardOptions,
        visualTransformation = inputType.visualTransformation,
        keyboardActions = keyboardActions
    )
}

private fun Context.buildExoPlayer(uri: Uri) =
    ExoPlayer.Builder(this).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        repeatMode = Player.REPEAT_MODE_ALL
        playWhenReady = true
        prepare()
    }

@SuppressLint("DiscouragedApi")
private fun getVideoUri(context: Context): Uri {
    val rawId = context.resources.getIdentifier("clouds", "raw", context.packageName)
    val videoUri = "android.resource://${context.packageName}/$rawId"
    return videoUri.toUri()
}

private fun Context.buildPlayerView(exoPlayer: ExoPlayer) =
    StyledPlayerView(this).apply {
        player = exoPlayer
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        useController = false
        resizeMode = RESIZE_MODE_ZOOM
    }
