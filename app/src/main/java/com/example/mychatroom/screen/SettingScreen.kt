package com.example.mychatroom.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mychatroom.session.SettingManager
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(settingManager: SettingManager, onBackClick: () -> Unit) {
    val isDarkMode by settingManager.getThemeMode().collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (isDarkMode) "Dark Mode" else "Light Mode")

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isDarkMode,
                onCheckedChange = {
                    scope.launch { settingManager.saveThemeMode(it) }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Back", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }

}