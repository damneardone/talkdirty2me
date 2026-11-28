package com.damneardone.talkdirty2me.net

import com.damneardone.talkdirty2me.tts.TtsAudio
import com.damneardone.talkdirty2me.tts.TtsFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Curated cloud-voice defaults (from the live /audio/speech/models catalog). */
object TtsCatalog {
    /** Seed-TTS 3.0: mp3 + speed 0.5–2 + delivery `instructions`, 500+ voices. */
    const val MODEL = "bytedance-seed/seed-tts-3-0"
    const val VOICE = "multi_female_skye_v3_uranus_bigtts"
}

/** One entry of OpenRouter's /audio/speech/models catalog. */
data class TtsModelInfo(
    val id: String,
    val name: String,
    val voices: List<String>,
    val formats: List<String>,
    val speedMin: Double? = null,
    val speedMax: Double? = null,
    val supportsInstructions: Boolean = false,
)

/** Cloud TTS client for the OpenAI-compatible /audio/speech endpoint (OpenRouter by default). */
class SpeechClient(private val client: OkHttpClient = OkHttpClient()) {

    /** GET {base}/audio/speech/models — the live voice-model catalog. */
    suspend fun fetchModels(base: String, key: String): List<TtsModelInfo> {
        val b = Request.Builder().url("$base/audio/speech/models")
        if (key.isNotEmpty()) b.header("Authorization", "Bearer $key")
        Http.addOpenRouterAttribution(b)
        val response = Http.executeAwait(client, b.get().build())
        try {
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) throw Td2mException("HTTP ${response.code} — ${SseParser.errorDetail(body)}")
            val models = parseModels(body)
            if (models.isEmpty()) throw Td2mException("the voice-model list came back empty — try again in a minute")
            return models
        } finally {
            response.close()
        }
    }

    /**
     * POST {base}/audio/speech — synthesize [input] to raw audio bytes.
     * [instructions] (delivery directions) is sent only for models that support them;
     * [speed] only when the model publishes a range (callers clamp to it).
     */
    suspend fun createSpeech(
        base: String,
        key: String,
        model: String,
        input: String,
        voice: String,
        responseFormat: String,
        speed: Double?,
        instructions: String?,
    ): TtsAudio {
        val payload = buildJsonObject {
            put("model", model)
            put("input", input)
            if (voice.isNotEmpty()) put("voice", voice)
            put("response_format", responseFormat)
            if (speed != null) put("speed", speed)
            if (!instructions.isNullOrEmpty()) put("instructions", instructions)
        }.toString()
        val request = Request.Builder()
            .url("$base/audio/speech")
            .header("Authorization", "Bearer $key")
            .also { Http.addOpenRouterAttribution(it) }
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        val response = Http.executeAwait(client, request)
        try {
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (response.code == 401) throw Td2mException("invalid key (401) — check the key at https://openrouter.ai/keys")
            if (response.code == 402) throw Td2mException("insufficient credits (402) — top up at https://openrouter.ai/credits")
            if (!response.isSuccessful) {
                throw Td2mException("HTTP ${response.code} — ${SseParser.errorDetail(String(bytes))}".take(160))
            }
            if (bytes.isEmpty()) throw Td2mException("the voice model returned empty audio")
            return TtsAudio(bytes, formatOf(response.header("Content-Type"), responseFormat))
        } finally {
            response.close()
        }
    }

    companion object {

        /** Pure: parses the /audio/speech/models catalog body (`{"data":[…]}` or a bare array). */
        fun parseModels(body: String): List<TtsModelInfo> {
            val data = try {
                val root = Json.parseToJsonElement(body)
                (root as? JsonObject)?.get("data") ?: root
            } catch (e: Exception) {
                throw Td2mException("unexpected voice-model list — type the model + voice ids manually")
            }
            val arr = data as? JsonArray
                ?: throw Td2mException("unexpected voice-model list — type the model + voice ids manually")
            return arr.mapNotNull { m ->
                val o = m as? JsonObject ?: return@mapNotNull null
                val id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val name = (o["name"] as? JsonPrimitive)?.contentOrNull ?: id
                val speech = (o["output_modalities"] as? JsonArray)
                    ?.filterIsInstance<JsonObject>()
                    ?.firstOrNull { (it["type"] as? JsonPrimitive)?.contentOrNull == "speech" }
                val sp = (speech?.get("supported_parameters") as? JsonObject) ?: JsonObject(emptyMap())
                TtsModelInfo(
                    id = id,
                    name = name,
                    voices = stringValues(sp, "voice"),
                    formats = stringValues(sp, "response_format"),
                    speedMin = (sp["speed"] as? JsonObject)?.get("min").let { it as? JsonPrimitive }?.doubleOrNull,
                    speedMax = (sp["speed"] as? JsonObject)?.get("max").let { it as? JsonPrimitive }?.doubleOrNull,
                    supportsInstructions = sp.containsKey("instructions"),
                )
            }
        }

        private fun stringValues(sp: JsonObject, key: String): List<String> =
            ((sp[key] as? JsonObject)?.get("values") as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: emptyList()

        /** Content-Type → playback format, falling back to what we asked for. */
        fun formatOf(contentType: String?, requested: String): TtsFormat = when {
            contentType == null -> TtsFormat.of(requested)
            contentType.contains("mpeg") -> TtsFormat.MP3
            contentType.contains("wav") -> TtsFormat.WAV
            contentType.contains("pcm") -> TtsFormat.PCM_16LE
            else -> TtsFormat.of(requested)
        }
    }
}