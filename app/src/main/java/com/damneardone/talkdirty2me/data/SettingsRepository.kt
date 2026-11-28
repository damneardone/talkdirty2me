package com.damneardone.talkdirty2me.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.damneardone.talkdirty2me.net.TtsCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// App-private DataStore. The web version kept these in localStorage under the same
// key names; the API key lives only here (app-private storage, no backups — see
// allowBackup=false in the manifest). Model lists and favorites are stored as plain
// string sets — no JSON, no serialization plugin needed.
private val Context.td2mDataStore by preferencesDataStore(name = "td2m_settings")

object SettingsKeys {
    val PROVIDER = stringPreferencesKey("td2m.provider")
    val BASE = stringPreferencesKey("td2m.base")
    val KEY = stringPreferencesKey("td2m.key")
    val MODEL = stringPreferencesKey("td2m.model")
    val AGE_OK = booleanPreferencesKey("td2m.ageok")
    val FAV_ONLY = booleanPreferencesKey("td2m.favOnly")
    val OR_FAVS = stringSetPreferencesKey("td2m.orFavs.v1")
    val OR_HIDDEN = stringSetPreferencesKey("td2m.orHidden.v1")
    val MODELS_OPENROUTER = stringSetPreferencesKey("td2m.modelsCache.openrouter")
    val MODELS_OPENAI = stringSetPreferencesKey("td2m.modelsCache.openai")

    // Cloud voice (TTS) settings — same key as chat, but its own model + voice + speed.
    val TTS_MODEL = stringPreferencesKey("td2m.ttsModel")
    val TTS_VOICE = stringPreferencesKey("td2m.ttsVoice")
    val TTS_SPEED = floatPreferencesKey("td2m.ttsSpeed")
    val READ_ALOUD = booleanPreferencesKey("td2m.readAloud")
    val BANS = stringPreferencesKey("td2m.bans")
    fun modelsKey(provider: String) = when (provider) {
        "openai" -> MODELS_OPENAI
        else -> MODELS_OPENROUTER
    }
}

data class ProviderSettings(
    val provider: String = "openrouter",
    val base: String = "",
    val apiKey: String = "",
    val model: String = "",
    val favOnly: Boolean = true,
    val favs: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val ageOk: Boolean = false,
    val ttsModel: String = TtsCatalog.MODEL,
    val ttsVoice: String = TtsCatalog.VOICE,
    val ttsSpeed: Float = 1.0f,
    val readAloud: Boolean = false,
    val bans: String = "",
    val cachedModels: Map<String, Set<String>> = emptyMap(),
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<ProviderSettings> = context.td2mDataStore.data.map { p ->
        val cachedModels = mapOf(
            "openrouter" to (p[SettingsKeys.MODELS_OPENROUTER] ?: emptySet()),
            "openai" to (p[SettingsKeys.MODELS_OPENAI] ?: emptySet()),
        )
        ProviderSettings(
            provider = p[SettingsKeys.PROVIDER] ?: "openrouter",
            base = p[SettingsKeys.BASE] ?: "",
            apiKey = p[SettingsKeys.KEY] ?: "",
            model = p[SettingsKeys.MODEL] ?: "",
            favOnly = p[SettingsKeys.FAV_ONLY] ?: true,
            favs = (p[SettingsKeys.OR_FAVS] ?: emptySet()).toList(),
            hidden = (p[SettingsKeys.OR_HIDDEN] ?: emptySet()).toList(),
            ageOk = p[SettingsKeys.AGE_OK] ?: false,
            ttsModel = p[SettingsKeys.TTS_MODEL] ?: TtsCatalog.MODEL,
            ttsVoice = p[SettingsKeys.TTS_VOICE] ?: TtsCatalog.VOICE,
            ttsSpeed = p[SettingsKeys.TTS_SPEED] ?: 1.0f,
            readAloud = p[SettingsKeys.READ_ALOUD] ?: false,
            bans = p[SettingsKeys.BANS] ?: "",
            cachedModels = cachedModels.filterValues { it.isNotEmpty() },
        )
    }

    /** Mirrors the web saveProvider: key is only written when non-empty. */
    suspend fun saveProvider(provider: String, base: String, key: String, model: String) {
        context.td2mDataStore.edit { p ->
            p[SettingsKeys.PROVIDER] = provider
            p[SettingsKeys.BASE] = base
            if (key.isNotEmpty()) p[SettingsKeys.KEY] = key
            p[SettingsKeys.MODEL] = model
        }
    }

    suspend fun forgetKey() {
        context.td2mDataStore.edit { it.remove(SettingsKeys.KEY) }
    }

    suspend fun setFavOnly(on: Boolean) {
        context.td2mDataStore.edit { it[SettingsKeys.FAV_ONLY] = on }
    }

    suspend fun saveFavs(favs: List<String>, hidden: List<String>) {
        context.td2mDataStore.edit {
            it[SettingsKeys.OR_FAVS] = favs.toHashSet()
            it[SettingsKeys.OR_HIDDEN] = hidden.toHashSet()
        }
    }

    suspend fun setAgeOk(ok: Boolean) {
        context.td2mDataStore.edit { p ->
            if (ok) p[SettingsKeys.AGE_OK] = true else p.remove(SettingsKeys.AGE_OK)
        }
    }

    suspend fun saveCachedModels(provider: String, models: List<String>) {
        context.td2mDataStore.edit {
            it[SettingsKeys.modelsKey(provider)] = models.toHashSet()
        }
    }

    suspend fun saveTts(model: String, voice: String, speed: Float) {
        context.td2mDataStore.edit {
            it[SettingsKeys.TTS_MODEL] = model
            it[SettingsKeys.TTS_VOICE] = voice
            it[SettingsKeys.TTS_SPEED] = speed
        }
    }

    suspend fun setReadAloud(on: Boolean) {
        context.td2mDataStore.edit { it[SettingsKeys.READ_ALOUD] = on }
    }

    suspend fun saveBans(value: String) {
        context.td2mDataStore.edit { it[SettingsKeys.BANS] = value }
    }
}