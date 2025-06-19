package com.example.mychatroom.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mychatroom.data.Role
import com.example.mychatroom.data.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class SessionManager(private val context: Context) {


    companion object {
        val USER_UID = stringPreferencesKey("user_uid")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_FIRST_NAME = stringPreferencesKey("user_first_name")
        val USER_LAST_NAME = stringPreferencesKey("user_last_name")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    suspend fun saveUserSession(
        user: User
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = user.email
            preferences[USER_FIRST_NAME] = user.firstName
            preferences[USER_LAST_NAME] = user.lastName
            preferences[USER_ROLE] = user.role.name
        }
    }

     suspend fun isLogin(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences.asMap().isNotEmpty()
    }

    //    fun getUserUid(): Flow<String?> = context.dataStore.data.map { it[USER_UID] }
    fun getUserEmail(): Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    fun getFirstName(): Flow<String?> = context.dataStore.data.map { it[USER_FIRST_NAME] }
    fun getLastName(): Flow<String?> = context.dataStore.data.map { it[USER_LAST_NAME] }
    fun getUserRole(): Flow<Role?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ROLE]?.let { roleString ->
                try {
                    Role.valueOf(roleString)
                } catch (e: IllegalArgumentException) {
                    Role.MEMBER
                }
            }
        }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}