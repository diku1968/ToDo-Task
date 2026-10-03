package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val defaultPriority: String = "NONE",
    val defaultCategoryId: Long = -1L,
    val autoCompleteParent: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibrate: Boolean = true,
    val showStreak: Boolean = true,
    val showAds: Boolean = true,
    val installedAt: Long = System.currentTimeMillis()
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val DEFAULT_CATEGORY_ID = longPreferencesKey("default_category_id")
        val AUTO_COMPLETE_PARENT = booleanPreferencesKey("auto_complete_parent")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATE = booleanPreferencesKey("notification_vibrate")
        val SHOW_STREAK = booleanPreferencesKey("show_streak")
        val SHOW_ADS = booleanPreferencesKey("show_ads")
        val INSTALLED_AT = longPreferencesKey("installed_at")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                defaultPriority = preferences[PreferencesKeys.DEFAULT_PRIORITY] ?: "NONE",
                defaultCategoryId = preferences[PreferencesKeys.DEFAULT_CATEGORY_ID] ?: -1L,
                autoCompleteParent = preferences[PreferencesKeys.AUTO_COMPLETE_PARENT] ?: true,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                notificationSound = preferences[PreferencesKeys.NOTIFICATION_SOUND] ?: true,
                notificationVibrate = preferences[PreferencesKeys.NOTIFICATION_VIBRATE] ?: true,
                showStreak = preferences[PreferencesKeys.SHOW_STREAK] ?: true,
                showAds = preferences[PreferencesKeys.SHOW_ADS] ?: true,
                installedAt = preferences[PreferencesKeys.INSTALLED_AT] ?: System.currentTimeMillis()
            )
        }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun setDefaultPriority(priority: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PRIORITY] = priority
        }
    }

    suspend fun setDefaultCategoryId(categoryId: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CATEGORY_ID] = categoryId
        }
    }

    suspend fun setAutoCompleteParent(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_COMPLETE_PARENT] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUND] = enabled
        }
    }

    suspend fun setNotificationVibrate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_VIBRATE] = enabled
        }
    }

    suspend fun setShowStreak(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_STREAK] = show
        }
    }

    suspend fun setShowAds(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ADS] = show
        }
    }
}
