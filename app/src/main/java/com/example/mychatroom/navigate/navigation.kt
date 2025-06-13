package com.example.mychatroom.navigate

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mychatroom.screen.ChatRoomListScreen
import com.example.mychatroom.screen.ChatScreen
import com.example.mychatroom.screen.GameScreen
import com.example.mychatroom.screen.HomeScreen
import com.example.mychatroom.screen.LoginScreen
import com.example.mychatroom.screen.SettingScreen
import com.example.mychatroom.screen.SignUpScreen
import com.example.mychatroom.session.SessionManager
import com.example.mychatroom.session.SettingManager
import com.example.mychatroom.viewModel.AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    settingManager: SettingManager,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(top = 32.dp)
    ) {
        composable(Screen.SignupScreen.route) {
            SignUpScreen(
                authViewModel,
                onNavigationToLogin = { navController.navigate(Screen.LoginScreen.route) })
        }

        composable(Screen.LoginScreen.route) {
            LoginScreen(
                authViewModel,
                LoginClick = { navController.navigate(Screen.HomeScreen.route) },
                SignUpClick = { navController.navigate(Screen.SignupScreen.route) },
                sessionManager
            )
        }

        composable(Screen.HomeScreen.route) {
            HomeScreen(
                onProfileClick = { navController.navigate(Screen.HomeScreen.route) },
                sessionManager
            )
        }

        composable(Screen.ChatRoomsScreen.route) {
            ChatRoomListScreen(
                sessionManager,
                onJoinChatClick = { room -> navController.navigate("${Screen.ChatScreen.route}/${room.id}/${room.name}") },
                onBackLoginCLick = { navController.navigate(Screen.LoginScreen.route) }
            )
        }

        composable(
            "${Screen.ChatScreen.route}/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) {
            val roomId = it.arguments?.getString("roomId") ?: ""
            val roomName = it.arguments?.getString("roomName") ?: ""
            ChatScreen(
                sessionManager,
                roomId = roomId,
                roomName = roomName,
                onClickBack = { navController.navigateUp() },
            )
        }

        composable(Screen.SettingScreen.route) {
            SettingScreen(settingManager, onBackClick = { navController.navigateUp() })
        }

        composable(Screen.FriendScreen.route) {
            SettingScreen(settingManager, onBackClick = { navController.navigateUp() })
        }

        composable(Screen.SavedScreen.route) {
            SettingScreen(settingManager, onBackClick = { navController.navigateUp() })
        }

        composable(Screen.LocationScreen.route) {
            SettingScreen(settingManager, onBackClick = { navController.navigateUp() })
        }

        composable(Screen.GameScreen.route) {
            GameScreen()
        }
    }
}