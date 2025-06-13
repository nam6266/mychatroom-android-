package com.example.mychatroom

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mychatroom.navigate.DrawerScreen
import com.example.mychatroom.navigate.NavigationGraph
import com.example.mychatroom.navigate.Screen
import com.example.mychatroom.navigate.screensInDrawer
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.session.SettingManager
import com.example.mychatroom.ui.theme.MyChatRoomTheme
import com.example.mychatroom.viewModel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val authViewModel: AuthViewModel = viewModel()
            val context = LocalContext.current
            val sessionManager = remember { SessionManager(context) }
            val settingManager = remember { SettingManager(context) }
            val themeFlow = settingManager.getThemeMode()
            val theme by themeFlow.collectAsState(initial = true)
            MyChatRoomTheme(darkTheme = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(navController, authViewModel, sessionManager, settingManager)
                    // ChatRoomListScreen()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    settingManager: SettingManager
) {

    var startDestination by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerSeleted by remember { mutableStateOf("") }
    var backToLogin by remember { mutableStateOf(false) }
    var needLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val isLoggedIn = sessionManager.isLogin()
        startDestination = if (isLoggedIn) {
            Screen.HomeScreen.route
        } else {
            Screen.LoginScreen.route
        }
    }
    if (startDestination != null) {
        ModalNavigationDrawer(
            drawerState = drawerState,
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
                        TopAppBar(navController, pressOnDrawer = {
                            scope.launch {
                                if (sessionManager.isLogin()) drawerState.open() else needLogin =
                                    true
                            }
                        })
                    },
                    bottomBar = {
                        BottomAppBar(navController)
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavigationGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            sessionManager = sessionManager,
                            settingManager = settingManager,
                            startDestination = startDestination!!
                        )
                    }
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
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
fun BottomAppBar(navController: NavHostController) {
    var seletedTab by remember { mutableStateOf("Home") }

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = seletedTab == "Home",
            onClick = {
                navController.navigate(Screen.HomeScreen.route)
                seletedTab = "Home"
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Email, contentDescription = "Profile") },
            label = { Text("Chat room") },
            selected = seletedTab == "Profile",
            onClick = {
                navController.navigate(Screen.ChatRoomsScreen.route)
                seletedTab = "Profile"
            }
        )
    }
}