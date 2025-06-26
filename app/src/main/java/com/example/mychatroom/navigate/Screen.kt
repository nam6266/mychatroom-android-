package com.example.mychatroom.navigate

import androidx.annotation.DrawableRes
import com.example.mychatroom.R

sealed class Screen(val route: String) {
    object LoginScreen : Screen("loginscreen")
    object SignupScreen : Screen("signupscreen")
    object HomeScreen : Screen("homescreen")
    object ChatRoomsScreen : Screen("chatroomscreen")
    object ChatScreen : Screen("chatscreen")
    object SettingScreen : Screen("settingscreen")
    object FriendScreen : Screen("friendscreen")
    object SavedScreen : Screen("savedscreen")
    object LocationScreen : Screen("locationscreen")
    object GameScreen : Screen("gamescreen")
}

sealed class DrawerScreen(val dTitle: String, val dRoute: String, @DrawableRes val icon: Int) {
    object Home : DrawerScreen(
        "Home",
        "homescreen",
        R.drawable.ic_home
    )

    object Message : DrawerScreen(
        "ChatRoomsScreen",
        "chatroomscreen",
        R.drawable.ic_message
    )

    object Friend : DrawerScreen(
        "Friend",
        "friendscreen",
        R.drawable.ic_friend
    )

    object Saved : DrawerScreen(
        "Saved",
        "savedscreen",
        R.drawable.ic_saved
    )

    object Location : DrawerScreen(
        "Location",
        "locationscreen",
        R.drawable.ic_location
    )

    object Game : DrawerScreen(
        "Game",
        "gamescreen",
        R.drawable.ic_game
    )

    object Logout : DrawerScreen(
        "Logout",
        "logout",
        R.drawable.ic_logout
    )
}

val screensInDrawer = listOf(
    DrawerScreen.Home,
    DrawerScreen.Friend,
    DrawerScreen.Saved,
    DrawerScreen.Location,
    DrawerScreen.Game,
    DrawerScreen.Logout
)

val bottomNavigator = listOf(
    DrawerScreen.Home,
    DrawerScreen.Message
)