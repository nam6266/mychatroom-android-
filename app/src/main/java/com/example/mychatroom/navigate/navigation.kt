package com.example.mychatroom.navigate

import com.example.mychatroom.screen.SpaceSurvivor
import androidx.navigation.NavGraph.Companion.findStartDestination
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mychatroom.screen.AlienGame
import com.example.mychatroom.screen.CameraCaptureScreen
import com.example.mychatroom.screen.ChatRoomListScreen
import com.example.mychatroom.screen.ChatScreen
import com.example.mychatroom.screen.GameScreen
import com.example.mychatroom.screen.HomeScreen
import com.example.mychatroom.screen.LoginScreen
import com.example.mychatroom.screen.SettingScreen
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
    ) {
        composable(Screen.LoginScreen.route) {
            LoginScreen(
                authViewModel,
                LoginClick = { navController.navigate(Screen.HomeScreen.route) },
                sessionManager
            )
        }

        composable(Screen.HomeScreen.route) {
            HomeScreen(
                onProfileClick = { navController.navigateTopLevel(Screen.HomeScreen.route) },
                sessionManager
            )
        }

        composable(Screen.ChatRoomsScreen.route) {
            ChatRoomListScreen(
                sessionManager,
                // encode the name so spaces, '/' or '?' in a room name don't break the route
                onJoinChatClick = { room -> navController.navigate("${Screen.ChatScreen.route}/${room.id}/${Uri.encode(room.name)}") },
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
            GameScreen(navController = navController)   // the list of games
        }

        composable(Screen.AlienGame.route) {
            AlienGame()
        }

        composable(Screen.SpaceSurvivor.route) {
            SpaceSurvivor()
        }

        composable(Screen.CameraScreen.route) {
            CameraCaptureScreen()
        }

    }
}

/**
 * Navigate to a top-level screen (drawer, bottom bar, FAB, top bar) without
 * stacking duplicates: pressing "Home" five times no longer needs five Backs.
 */
fun NavHostController.navigateTopLevel(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}