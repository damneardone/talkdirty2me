package com.damneardone.talkdirty2me

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.damneardone.talkdirty2me.data.ProviderSettings
import com.damneardone.talkdirty2me.data.SettingsRepository
import com.damneardone.talkdirty2me.net.ModelCatalog
import com.damneardone.talkdirty2me.net.ModelsClient
import com.damneardone.talkdirty2me.net.OpenAiCompatClient
import com.damneardone.talkdirty2me.net.SpeechClient
import com.damneardone.talkdirty2me.net.Td2mException
import com.damneardone.talkdirty2me.net.TtsModelInfo
import com.damneardone.talkdirty2me.prompt.Choices
import com.damneardone.talkdirty2me.prompt.TalkDirtyPrompts
import com.damneardone.talkdirty2me.prompt.TtsPrompts
import com.damneardone.talkdirty2me.tts.TtsPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GenViewModel(private val repo: SettingsRepository, appContext: Context) : ViewModel() {

    private val chat = OpenAiCompatClient()
    private val modelsClient = ModelsClient()
    private val speechClient = SpeechClient()
    private val ttsPlayer = TtsPlayer(appContext)

    private val _ttsModels = MutableStateFlow<List<TtsModelInfo>>(emptyList())
    val ttsModels = _ttsModels.asStateFlow()

    private val _ttsStatus = MutableStateFlow("")
    val ttsStatus = _ttsStatus.asStateFlow()

    val speaking: StateFlow<Boolean> = ttsPlayer.playing

    private val _ageOk = MutableStateFlow(false)
    val ageOk = _ageOk.asStateFlow()

    private val _settings = MutableStateFlow(ProviderSettings())
    val settings = _settings.asStateFlow()

    // Control defaults mirror the web version's HTML defaults (nothing persisted there).
    val userGender = MutableStateFlow(Choices.USER_GENDERS.first().value)
    val voice = MutableStateFlow(Choices.VOICES.first().value)
    val vibeChoice = MutableStateFlow(Choices.VIBES.first().value)
    val spice = MutableStateFlow(2)
    val lengthChoice = MutableStateFlow("2")
    val extra = MutableStateFlow("")

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating = _generating.asStateFlow()

    private val _connStatus = MutableStateFlow("")
    val connStatus = _connStatus.asStateFlow()

    private val _modelsStatus = MutableStateFlow("")
    val modelsStatus = _modelsStatus.asStateFlow()

    private var genJob: Job? = null
    private var lastTwistIdx = -1

    init {
        viewModelScope.launch {
            repo.settings.collect { s ->
                _settings.value = s
                _ageOk.value = s.ageOk
            }
        }
    }

    fun confirmAge(rememberMe: Boolean) {
        viewModelScope.launch { repo.setAgeOk(rememberMe) }
    }

    /** Re-lock: hide everything, stop any in-flight generation + speech, forget the age memory. */
    fun lockSite() {
        genJob?.cancel()
        ttsPlayer.stop()
        _message.value = ""
        _status.value = ""
        viewModelScope.launch { repo.setAgeOk(false) }
    }

    fun saveTts(model: String, voice: String, speed: Float) {
        viewModelScope.launch { repo.saveTts(model, voice, speed) }
    }

    fun setReadAloud(on: Boolean) {
        viewModelScope.launch { repo.setReadAloud(on) }
    }

    fun saveBans(value: String) {
        viewModelScope.launch { repo.saveBans(value) }
    }

    private var modelsLoaded = false
    private var ttsModelsLoaded = false

    /** First panel open each session pulls the live model lists — no manual refresh button. */
    fun ensureModels() {
        if (!modelsLoaded) {
            modelsLoaded = true
            refreshModels()
        }
    }

    fun ensureTtsModels() {
        if (!ttsModelsLoaded) {
            ttsModelsLoaded = true
            refreshTtsModels()
        }
    }

    fun providerName(id: String): String = when (id) {
        "openai" -> "OpenAI-compatible"
        "openrouter" -> "OpenRouter"
        else -> id
    }

    private fun resolvedBase(s: ProviderSettings): String {
        val raw = when (s.provider) {
            "openrouter" -> s.base.ifEmpty { OpenAiCompatClient.OPENROUTER_DEFAULT }
            else -> s.base.ifEmpty { OpenAiCompatClient.OPENAI_DEFAULT }
        }
        return raw.trim().trimEnd('/')
    }

    /** Model list shown in the dropdown — port of the web `renderModelList`. */
    fun modelSuggestions(s: ProviderSettings = _settings.value): List<String> {
        val cached = s.cachedModels[s.provider].orEmpty().toList()
        return when {
            s.provider == "openrouter" && s.favOnly -> ModelCatalog.curated(s.favs, s.hidden)
            !cached.isNullOrEmpty() -> cached
            s.provider == "openrouter" -> ModelCatalog.OPENROUTER_MODELS
            else -> ModelCatalog.OPENAI_MODELS
        }
    }

    fun saveProvider(provider: String, base: String, key: String, model: String) {
        viewModelScope.launch { repo.saveProvider(provider, base, key, model) }
    }

    fun setFavOnly(on: Boolean) {
        viewModelScope.launch { repo.setFavOnly(on) }
    }

    fun toggleFavorite(current: String) {
        val s = _settings.value
        val (favs, hidden) = ModelCatalog.toggleFav(current, s.favs, s.hidden)
        viewModelScope.launch { repo.saveFavs(favs, hidden) }
    }

    fun isFavorite(model: String, s: ProviderSettings = _settings.value): Boolean =
        ModelCatalog.isFav(model, s.favs, s.hidden)

    fun forgetKey() {
        viewModelScope.launch { repo.forgetKey() }
    }

    fun refreshModels() {
        val s = _settings.value
        viewModelScope.launch {
            _modelsStatus.value = "Fetching live model list…"
            try {
                val models = modelsClient.fetchModels(resolvedBase(s), s.apiKey, s.provider == "openrouter")
                repo.saveCachedModels(s.provider, models)
                val favN = ModelCatalog.curated(s.favs, s.hidden).size
                _connStatus.value = if (s.provider == "openrouter" && s.favOnly) {
                    "✅ ${models.size} text models cached — showing your $favN ★ favorites."
                } else {
                    "✅ ${models.size} models loaded — pick one from the dropdown."
                }
                _modelsStatus.value = ""
            } catch (e: Exception) {
                _modelsStatus.value = "❌ ${e.message ?: e.toString()}"
            }
        }
    }

    fun testConnection() {
        val s = _settings.value
        viewModelScope.launch {
            _connStatus.value = "Testing…"
            try {
                _connStatus.value = modelsClient.testConnection(resolvedBase(s), s.apiKey, s.provider == "openrouter")
            } catch (e: Exception) {
                _connStatus.value = "❌ ${e.message ?: e.toString()}"
            }
        }
    }

    fun stopGen() {
        genJob?.cancel()
    }

    fun generate() {
        if (!_ageOk.value) {
            _status.value = "🔞 Please confirm you are 18+ first."
            return
        }
        genJob?.cancel()

        val rng = Random
        val s = _settings.value
        val model = s.model.trim()
        val spiceLevel = spice.value
        val sentences = lengthChoice.value.let { v ->
            if (v == "random") 1 + rng.nextInt(4) else v.toInt()
        }
        val vibe0 = vibeChoice.value
        val vibe = if (vibe0 == "random") {
            Choices.VIBES.filter { it.value != "random" }.random(rng).value
        } else {
            vibe0
        }
        val (idx, twist) = TalkDirtyPrompts.pickTwist(rng, lastTwistIdx)
        lastTwistIdx = idx
        // 🚫 user's banned phrases — injected into the prompt as a hard rule.
        val bans = s.bans.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        val instruction = TalkDirtyPrompts.buildInstruction(
            userGender = userGender.value,
            voice = voice.value,
            vibe = vibe,
            spice = spiceLevel,
            sentences = sentences,
            extra = extra.value,
            twist = twist,
            bans = bans,
        )
        val whisperWord = when {
            spiceLevel >= 5 -> "unspeakable"
            spiceLevel == 4 -> "filthy"
            spiceLevel == 3 -> "spicy"
            spiceLevel == 2 -> "flirty"
            else -> "sweet"
        }
        val viaNote = " via ${model.ifEmpty { "your model" }}"
        _generating.value = true
        _status.value = "Whispering something $whisperWord in your ear$viaNote"
        _message.value = ""

        genJob = viewModelScope.launch {
            try {
                val text = chat.streamChatCompletion(
                    base = resolvedBase(s),
                    key = s.apiKey,
                    model = model,
                    instruction = instruction,
                    isOpenRouter = s.provider == "openrouter",
                ) { full -> _message.value = full.trimStart() }
                val cleaned = text.trim().replace(Regex("^[\"“”]+|[\"“”]+$"), "")
                if (cleaned.isEmpty()) throw Td2mException("empty response")
                _message.value = cleaned
                val slipped = bans.firstOrNull { cleaned.contains(it, ignoreCase = true) }
                _status.value = if (slipped != null) {
                    "⚠ banned phrase slipped in (\"$slipped\") — ↻ more like this for a reroll"
                } else {
                    ""
                }
                if (s.readAloud) speak(cleaned)
            } catch (e: CancellationException) {
                _status.value = "Stopped."
                throw e
            } catch (e: Exception) {
                _status.value = "❌ ${e.message ?: e.toString()}"
            } finally {
                _generating.value = false
            }
        }
    }

    fun surpriseMe() {
        if (!_ageOk.value) {
            _status.value = "🔞 Please confirm you are 18+ first."
            return
        }
        val rng = Random
        voice.value = Choices.VOICES[rng.nextInt(Choices.VOICES.size)].value
        vibeChoice.value = Choices.VIBES.filter { it.value != "random" }.random(rng).value
        spice.value = 1 + rng.nextInt(5)
        lengthChoice.value = (1 + rng.nextInt(4)).toString()
        generate()
    }

    fun refreshTtsModels() {
        val s = _settings.value
        viewModelScope.launch {
            _ttsStatus.value = "Fetching voice models…"
            try {
                _ttsModels.value = speechClient.fetchModels(resolvedBase(s), s.apiKey)
                _ttsStatus.value = "✅ ${_ttsModels.value.size} voice models loaded."
            } catch (e: Exception) {
                _ttsStatus.value = "❌ ${e.message ?: e.toString()}"
            }
        }
    }

    fun ttsModelInfo(s: ProviderSettings = _settings.value): TtsModelInfo? =
        _ttsModels.value.firstOrNull { it.id == s.ttsModel }

    /** Synthesizes [text] with the cloud voice and plays it out loud. */
    fun speak(text: String = _message.value) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val s = _settings.value
        if (s.apiKey.isEmpty()) {
            _ttsStatus.value = "❌ add your API key in the 🔌 provider box first"
            return
        }
        val info = ttsModelInfo(s)
        val format = when {
            info?.formats?.contains("mp3") == true -> "mp3"
            info?.formats?.contains("wav") == true -> "wav"
            info?.formats?.isNotEmpty() == true -> info.formats.first()
            else -> "mp3"
        }
        // 🎭 delivery directions — only for models that support `instructions`,
        // steered automatically by the current vibe + spice picks.
        val instructions = info?.takeIf { it.supportsInstructions }
            ?.let { TtsPrompts.delivery(vibeChoice.value, spice.value) }
        val speed = info?.let { i ->
            val min = i.speedMin
            val max = i.speedMax
            if (min != null && max != null) s.ttsSpeed.toDouble().coerceIn(min, max) else null
        }
        viewModelScope.launch {
            try {
                _ttsStatus.value = "🔊 Voicing it…"
                val audio = speechClient.createSpeech(
                    base = resolvedBase(s),
                    key = s.apiKey,
                    model = s.ttsModel,
                    input = trimmed,
                    voice = s.ttsVoice,
                    responseFormat = format,
                    speed = speed,
                    instructions = instructions,
                )
                ttsPlayer.play(audio)
                _ttsStatus.value = ""
            } catch (e: CancellationException) {
                _ttsStatus.value = ""
                throw e
            } catch (e: Exception) {
                _ttsStatus.value = "❌ ${e.message ?: e.toString()}"
            }
        }
    }

    fun stopSpeaking() {
        ttsPlayer.stop()
        _ttsStatus.value = ""
    }
}