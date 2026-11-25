package com.damneardone.talkdirty2me.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/** GET {base}/models — live model list fetch + connection test (port of the web `refreshModels`/`testConnection`). */
class ModelsClient(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun fetchModels(base: String, key: String, isOpenRouter: Boolean): List<String> {
        val b = Request.Builder().url("$base/models")
        if (key.isNotEmpty()) b.header("Authorization", "Bearer $key")
        if (isOpenRouter) Http.addOpenRouterAttribution(b)
        val response = Http.executeAwait(client, b.get().build())
        try {
            val body = response.body?.string() ?: ""
            if (response.code == 401) throw Td2mException("invalid key (401) — check the key at https://openrouter.ai/keys")
            if (response.code == 404) throw Td2mException("no /models endpoint here (404) — type the model id manually")
            if (!response.isSuccessful) throw Td2mException("HTTP ${response.code} — ${body.take(120)}")
            val models = parseModels(body, isOpenRouter)
            if (models.isEmpty()) throw Td2mException("provider returned an empty model list")
            return models
        } finally {
            response.close()
        }
    }

    /** Returns the success message ("✅ Connected to …"). Throws Td2mException with a friendly message otherwise. */
    suspend fun testConnection(base: String, key: String, isOpenRouter: Boolean): String {
        if (key.isEmpty()) throw Td2mException("enter an API key first — create one at https://openrouter.ai/keys")
        val b = Request.Builder().url("$base/models").header("Authorization", "Bearer $key")
        if (isOpenRouter) Http.addOpenRouterAttribution(b)
        val response = Http.executeAwait(client, b.get().build())
        try {
            val body = response.body?.string() ?: ""
            if (response.code == 401) throw Td2mException("invalid key (401) — check the key")
            if (response.code == 404) throw Td2mException("no /models endpoint here (404) — check the server URL, or just try generating anyway")
            if (!response.isSuccessful) throw Td2mException("HTTP ${response.code} — ${body.take(120)}")
            var count = ""
            try {
                val arr = Json.parseToJsonElement(body).jsonObject["data"] as? JsonArray
                if (arr != null) count = " — ${arr.size} models available"
            } catch (e: Exception) {
            }
            return "✅ Connected to $base$count."
        } finally {
            response.close()
        }
    }

    companion object {
        /** Pure: filters a provider /models response body down to chat-capable model ids. */
        fun parseModels(body: String, isOpenRouter: Boolean): List<String> {
            val data = try {
                Json.parseToJsonElement(body).jsonObject["data"]
            } catch (e: Exception) {
                throw Td2mException("unexpected /models response — type the model id manually")
            } ?: throw Td2mException("unexpected /models response — type the model id manually")
            val arr = data as? JsonArray ?: throw Td2mException("unexpected /models response — type the model id manually")
            val mapped = arr.mapNotNull { m ->
                val o = m as? JsonObject ?: return@mapNotNull null
                if (isOpenRouter && !isChatModel(o)) return@mapNotNull null
                o["id"]?.jsonPrimitive?.contentOrNull ?: o["name"]?.jsonPrimitive?.contentOrNull
            }
            return mapped
                .filter { !Regex(":batch$").containsMatchIn(it) }
                .distinct()
                .sorted()
        }

        /** Text-modality check (OpenRouter): output_modalities, else endpoints, else keep. */
        private fun isChatModel(o: JsonObject): Boolean {
            val modalities = (o["architecture"] as? JsonObject)?.get("output_modalities") as? JsonArray
            if (modalities != null) return modalities.any { it.jsonPrimitive.contentOrNull == "text" }
            val endpoints = o["endpoints"] as? JsonArray
            if (endpoints != null) return endpoints.any {
                val e = it.jsonPrimitive.contentOrNull
                e == "/chat/completions" || e == "/responses"
            }
            return true
        }
    }
}