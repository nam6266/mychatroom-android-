package com.example.mychatroom.screen

import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mychatroom.R
import com.example.mychatroom.data.Results
import com.example.mychatroom.navigate.DrawerScreen
import com.example.mychatroom.navigate.NavigationGraph
import com.example.mychatroom.navigate.Screen
import com.example.mychatroom.navigate.bottomNavigator
import com.example.mychatroom.navigate.fabNavigator
import com.example.mychatroom.navigate.screensInDrawer
import com.example.mychatroom.servies.ChatService
import com.example.mychatroom.servies.NotificationPermissionManager
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.session.SettingManager
import com.example.mychatroom.times
import com.example.mychatroom.transform
import com.example.mychatroom.ui.theme.DEFAULT_PADDING
import com.example.mychatroom.ui.theme.MyChatRoomTheme
import com.example.mychatroom.viewModel.AuthViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.S)
private fun getRenderEffect(): RenderEffect {
    val blurEffect = RenderEffect
        .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)

    val alphaMatrix = RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 50f, -5000f
                )
            )
        )
    )

    return RenderEffect
        .createChainEffect(alphaMatrix, blurEffect)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Main() {
    val context = LocalContext.current
    val permissionManager = remember { NotificationPermissionManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {

        permissionManager.requestPermissionIfRequired(permissionLauncher)
        val serviceIntent = Intent(context, ChatService::class.java).apply {
        }
        context.startService(serviceIntent)
    }
    val isMenuExtended = remember { mutableStateOf(false) }

    val fabAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = LinearEasing
        )
    )

    val clickAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearEasing
        )
    )

    val renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getRenderEffect().asComposeRenderEffect()
    } else {
        null
    }
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val sessionManager = remember { SessionManager(context) }
    val settingManager = remember { SettingManager(context) }
    val themeFlow = settingManager.getThemeMode()
    val theme by themeFlow.collectAsState(initial = true)
    MyChatRoomTheme(darkTheme = theme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                navController,
                authViewModel,
                sessionManager,
                settingManager,
                renderEffect = renderEffect,
                fabAnimationProgress = fabAnimationProgress,
                clickAnimationProgress = clickAnimationProgress,
                toggleAnimation = {isMenuExtended.value = !isMenuExtended.value }
            )
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    settingManager: SettingManager,
    renderEffect: androidx.compose.ui.graphics.RenderEffect?,
    fabAnimationProgress: Float = 0f,
    clickAnimationProgress: Float = 0f,
    toggleAnimation: () -> Unit = { },
    ) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerSeleted by remember { mutableStateOf("") }
    var backToLogin by remember { mutableStateOf(false) }
    var needLogin by remember { mutableStateOf(false) }
    val authResult by authViewModel.authResultLogin.observeAsState()

    val isLoggedIn = (authResult as? Results.Success)?.data?.first == true
    val startDestination = if (isLoggedIn) Screen.HomeScreen.route else Screen.LoginScreen.route
    val loading by authViewModel.isLoading.observeAsState(initial = true)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        authViewModel.checkLoginSession()
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    screensInDrawer.forEach { item ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painterResource(id = item.icon),
                                    contentDescription = item.dTitle
                                )
                            },
                            label = { Text(item.dTitle) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.dRoute == DrawerScreen.Logout.dRoute) {
                                    backToLogin = true
                                } else {
                                    navController.navigate(item.dRoute)
                                }
                                drawerSeleted = item.dTitle
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            content = {
                Scaffold(
                    topBar = {
                        if (isLoggedIn) {
                            TopAppBar(navController, pressOnDrawer = {
                                scope.launch {
                                    drawerState.open()
                                }
                            })
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavigationGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            sessionManager = sessionManager,
                            settingManager = settingManager,
                            startDestination = startDestination
                        )
                    }
                }
                if (isLoggedIn && currentRoute == Screen.HomeScreen.route) {
                    BottomAppBar(
                        navController,
                        renderEffect,
                        fabAnimationProgress,
                        clickAnimationProgress,
                        toggleAnimation
                    )
                }
            }
        )
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
                                    navController.navigate(Screen.LoginScreen.route)
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
//                        style = MaterialTheme.typography.bodyLarge.copy(
//                            color = MaterialTheme.colors.onSurface
//                        ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        if (needLogin) {
            AlertDialog(
                onDismissRequest = { needLogin = false },
                confirmButton = {

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
                            text = "Please Login",
//                        style = MaterialTheme.typography.bodyLarge.copy(
//                            color = MaterialTheme.colorScheme.onSurface
//                        ),
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
fun Circle(color: Color, animationProgress: Float) {
    val animationValue = sin(PI * animationProgress).toFloat()

    Box(
        modifier = Modifier
            .padding(DEFAULT_PADDING.dp)
            .size(56.dp)
            .scale(2 - animationValue)
            .border(
                width = 2.dp,
                color = color.copy(alpha = color.alpha * animationValue),
                shape = CircleShape
            )
    )
}

@Composable
fun CustomBottomNavigation() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(80.dp)
            .paint(
                painter = painterResource(R.drawable.bottom_navigator),
                contentScale = ContentScale.FillHeight
            )
            .padding(horizontal = 40.dp)
    ) {
        listOf(Icons.Filled.Person, Icons.Filled.Settings).map { image ->
            IconButton(onClick = { }) {
                Icon(imageVector = image, contentDescription = null, tint = Color.White)
            }
        }
    }
}




@Composable
fun FabGroup(
    navController: NavHostController? = null,
    animationProgress: Float = 0f,
    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
    toggleAnimation: () -> Unit = { }
) {
    // temproal fix for dynamic padding, will update later
    val paddings = fabNavigator.mapIndexed { index, _ ->
        val baseBottom = 100

        val size = fabNavigator.size
        val middle = size / 2

        val distance = kotlin.math.abs(index - middle)


        val currentBottom = baseBottom - (distance * (baseBottom / middle)) + 40
        val currentSide = distance * (200 / middle)

        when {
            size % 2 == 1 && index == middle -> {
                // Odd size → true middle element
                PaddingValues(bottom = baseBottom.dp)
            }
            index >= middle -> {
                // Left side
                PaddingValues(bottom = currentBottom.dp, start = currentSide.dp)
            }
            else -> {
                // Right side
                PaddingValues(bottom = currentBottom.dp, end = currentSide.dp)

            }
        }

    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.renderEffect = renderEffect }
            .padding(bottom = DEFAULT_PADDING.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

        fabNavigator.forEachIndexed { index, item ->
            AnimatedFab(
                icon = item.icon,
                modifier = Modifier.padding(paddings[index] * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
                ),
                opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress),
                onClick = { navController?.navigate(item.dRoute)}
            )
        }

//        AnimatedFab(
//            icon = Icons.Default.Settings,
//            modifier = Modifier
//                .padding(paddings[index] * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
//                ),
//            opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress)
//        )
//
//        AnimatedFab(
//            icon = Icons.Default.Build,
//            modifier = Modifier
//                .padding(
//                PaddingValues(
//                    bottom = 100.dp,
//                ) * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)
//            ),
//            opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress)
//        )
//
//        AnimatedFab(
//            painter = painterResource(R.drawable.ic_takephoto),
//            modifier = Modifier.padding(
//                PaddingValues(
//                    bottom = 72.dp,
//                    start = 210.dp
//                ) * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)
//            ),
//            opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress),
//            onClick = {
//                navController?.navigate(Screen.CameraScreen.route)
//            }
//        )



        AnimatedFab(
            painter = Icons.Default.Add,
            modifier = Modifier
                .rotate(
                    225 * FastOutSlowInEasing
                        .transform(0.35f, 0.65f, animationProgress)
                ),
            onClick = toggleAnimation,
            // onClick = { Log.d("namlog", "padding : $paddings") },
            backgroundColor = Color.Transparent
        )
    }
}

@Composable
fun AnimatedFab(
    modifier: Modifier,
    icon: Int? = null,
    painter: ImageVector? = null,
    opacity: Float = 1f,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    onClick: () -> Unit = {}
) {
    FloatingActionButton(
        onClick = onClick,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        shape = CircleShape,
        modifier = modifier.scale(1.25f)
    ) {
        when {
            icon != null -> {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = opacity)
                )
            }

            painter != null -> {
                Icon(
                    imageVector  = painter,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = opacity)
                )
            }
        }
    }
}

@Composable
fun TopAppBar(navController: NavHostController, pressOnDrawer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = pressOnDrawer) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Fun Community",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable { navController.navigate(Screen.HomeScreen.route) }
                )
            }

            IconButton(onClick = { navController.navigate(Screen.SettingScreen.route) }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun BottomAppBar(
    navController: NavHostController,
    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
    fabAnimationProgress: Float = 0f,
    clickAnimationProgress: Float = 0f,
    toggleAnimation: () -> Unit
) {
    var seletedTab by remember { mutableStateOf("Home") }

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(80.dp)
                .paint(
                    painter = painterResource(R.drawable.bottom_navigator),
                    contentScale = ContentScale.FillHeight
                )
                .padding(horizontal = 40.dp)
        ) {
            bottomNavigator.forEach { item ->
                IconButton(onClick = {
                    navController.navigate(item.dRoute)
                    seletedTab = item.dTitle
                }) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
        Circle(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            animationProgress = 0.5f
        )

        FabGroup(renderEffect = renderEffect, animationProgress = fabAnimationProgress)
        FabGroup(
            navController = navController,
            renderEffect = null,
            animationProgress = fabAnimationProgress,
            toggleAnimation = toggleAnimation
        )
        Circle(
            color = Color.White,
            animationProgress = clickAnimationProgress
        )
    }
}