package magi.aenerv7.ppembytv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IntroOutroSettings(private val context: Context) {

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            autoPriority = prefs[KEY_AUTO_PRIORITY] ?: true,
            skipIntro = prefs[KEY_SKIP_INTRO] ?: false,
            skipOutro = prefs[KEY_SKIP_OUTRO] ?: false,
        )
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_PRIORITY] = settings.autoPriority
            prefs[KEY_SKIP_INTRO] = settings.skipIntro
            prefs[KEY_SKIP_OUTRO] = settings.skipOutro
        }
    }

    data class Settings(
        val autoPriority: Boolean = true,
        val skipIntro: Boolean = false,
        val skipOutro: Boolean = false,
    )

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "intro_outro_settings")

        private val KEY_AUTO_PRIORITY = booleanPreferencesKey("auto_priority")
        private val KEY_SKIP_INTRO = booleanPreferencesKey("skip_intro")
        private val KEY_SKIP_OUTRO = booleanPreferencesKey("skip_outro")
    }
}
