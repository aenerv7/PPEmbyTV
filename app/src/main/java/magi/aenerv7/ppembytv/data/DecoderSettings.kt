package magi.aenerv7.ppembytv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.decoderDataStore: DataStore<Preferences> by preferencesDataStore(name = "decoder_settings")

class DecoderSettings(private val context: Context) {

    val decoderConfigFlow: Flow<DecoderConfig> = context.decoderDataStore.data.map { prefs ->
        DecoderConfig(
            mode = prefs[DECODER_MODE_KEY] ?: DECODER_MODE_AUTO,
            audioMode = prefs[AUDIO_DECODER_MODE_KEY] ?: AUDIO_DECODER_AUTO,
            audioPassthroughPriorityEnabled = prefs[AUDIO_PASSTHROUGH_PRIORITY_ENABLED_KEY] ?: false,
            dv7CompatibilityEnabled = prefs[DV7_COMPATIBILITY_ENABLED_KEY] ?: false,
        )
    }

    suspend fun saveDecoderMode(mode: String) {
        context.decoderDataStore.edit { prefs ->
            prefs[DECODER_MODE_KEY] = mode
        }
    }

    suspend fun saveAudioDecoderMode(mode: String) {
        context.decoderDataStore.edit { prefs ->
            prefs[AUDIO_DECODER_MODE_KEY] = mode
        }
    }

    suspend fun saveAudioPassthroughPriorityEnabled(enabled: Boolean) {
        context.decoderDataStore.edit { prefs ->
            prefs[AUDIO_PASSTHROUGH_PRIORITY_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveDv7CompatibilityEnabled(enabled: Boolean) {
        context.decoderDataStore.edit { prefs ->
            prefs[DV7_COMPATIBILITY_ENABLED_KEY] = enabled
        }
    }

    data class DecoderConfig(
        val mode: String = DECODER_MODE_AUTO,
        val audioMode: String = AUDIO_DECODER_AUTO,
        val audioPassthroughPriorityEnabled: Boolean = false,
        val dv7CompatibilityEnabled: Boolean = false,
    )

    companion object {
        const val AUDIO_DECODER_AUTO = "auto"
        const val AUDIO_DECODER_FORCE_FFMPEG = "force_ffmpeg"
        const val DECODER_MODE_AUTO = "auto"
        const val DECODER_MODE_HARDWARE = "hardware"
        const val DECODER_MODE_SOFTWARE = "software"

        private val DECODER_MODE_KEY = stringPreferencesKey("decoder_mode")
        private val AUDIO_DECODER_MODE_KEY = stringPreferencesKey("audio_decoder_mode")
        private val AUDIO_PASSTHROUGH_PRIORITY_ENABLED_KEY = booleanPreferencesKey("audio_passthrough_priority_enabled")
        private val DV7_COMPATIBILITY_ENABLED_KEY = booleanPreferencesKey("dv7_compatibility_enabled")
    }
}
