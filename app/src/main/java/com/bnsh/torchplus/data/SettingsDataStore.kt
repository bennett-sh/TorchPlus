package com.bnsh.torchplus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException


class SettingsDataStore(private val context: Context) {
    private val TORCH_BRIGHTNESS = floatPreferencesKey(TORCH_BRIGHTNESS_NAME)

    companion object {
        private const val TORCH_BRIGHTNESS_NAME = "torch_brightness"
        private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(
            name = TORCH_BRIGHTNESS_NAME
        )
    }

    val getTorchBrightness: Flow<Float?> = context.dataStore.data
        .catch {
            if (it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[TORCH_BRIGHTNESS]
        }

    suspend fun setTorchBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[TORCH_BRIGHTNESS] = brightness
        }
    }
}