package com.example.mychatroom.navigate

import androidx.annotation.DrawableRes
import com.example.mychatroom.R

sealed class Screen(val route: String) {
    object LoginScreen : Screen("loginscreen")
    object HomeScreen : Screen("homescreen")
    object ChatRoomsScreen : Screen("chatroomscreen")
    object ChatScreen : Screen("chatscreen")
    object SettingScreen : Screen("settingscreen")
    object FriendScreen : Screen("friendscreen")
    object SavedScreen : Screen("savedscreen")
    object LocationScreen : Screen("locationscreen")
    object GameScreen : Screen("gamescreen")
    object AlienGame : Screen("alienscreen")
    object SpaceSurvivor : Screen("survivorscreen")
    object CameraScreen : Screen("camerascreen")
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
        "Games",
        "gamescreen",          // opens the game list (GameScreen)
        R.drawable.ic_game
    )

    object Logout : DrawerScreen(
        "Logout",
        "logout",
        R.drawable.ic_logout
    )

    object CameraScreen : DrawerScreen(
        "Camerascreen",
        "camerascreen",
        R.drawable.ic_takephoto
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

val fabNavigator = listOf(
    DrawerScreen.Home,
    DrawerScreen.Message,
    DrawerScreen.CameraScreen
)

val bottomNavigator = listOf(
    DrawerScreen.Home,
    DrawerScreen.Message
)

/** One game on the Games page: what to show, and which route opens it. */
data class GameInfo(
    val title: String,
    val description: String,
    @DrawableRes val cover: Int,
    val route: String,
)

/** Every game in the app. To add a game: register its route in navigation.kt, add a line here. */
val gameMenu = listOf(
    GameInfo(
        title = "Alien Shooter",
        description = "Dodge meteors, blast UFOs and grab upgrades.",
        cover = R.drawable.game_cover,
        route = Screen.AlienGame.route,
    ),
    GameInfo(
        title = "Space Survivor",
        description = "Fly anywhere and survive the swarm. (In progress)",
        cover = R.drawable.survivor_cover,
        route = Screen.SpaceSurvivor.route,
    ),
)