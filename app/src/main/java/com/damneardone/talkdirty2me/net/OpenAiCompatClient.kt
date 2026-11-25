package com.damneardone.talkdirty2me.net

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random
import kotlin.concurrent.thread

class Td2mException(message: String) : Exception(message)

/** Pure SSE data-line handling, split out so it can be unit-tested. */
object SseParser {

    /** Returns the data payload of an SSE line, or null when the line is not a data event. */
    fun dataLine(line: String): String? {
        val t = line.trim()
        if (!t.startsWith("data:")) return null
        return t.removePrefix("data:").trim()
    }

    /** Extracts the streamed content piece from a chat-completion SSE data payload. */
    fun pieceOf(data: String): String = try {
        val j = Json.parseToJsonElement(data)
        j.jsonObject["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
            ?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
    } catch (e: Exception) {
        "" // partial JSON or malformed line — skip, more bytes coming
    }

    /** Extracts the full content from a non-streaming chat completion response body. */
    fun contentOf(body: String): String = try {
        Json.parseToJsonElement(body).jsonObject["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
    } catch (e: Exception) {
        ""
    }

    /** Best-effort error message from an error response body (port of the web detail logic). */
    fun errorDetail(body: String?): String {
        val s = body ?: return ""
        return try {
            val el = Json.parseToJsonElement(s)
            el.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull ?: s.take(200)
        } catch (e: Exception) {
            s.take(200)
        }
    }
}

/** OpenAI-compatible /chat/completions streaming client (OkHttp port of the web `streamChatCompletion`). */
class OpenAiCompatClient(private val client: OkHttpClient = OkHttpClient()) {

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val OPENROUTER_DEFAULT = "https://openrouter.ai/api/v1"
        const val OPENAI_DEFAULT = "https://api.openai.com/v1"
    }

    private fun payload(
        model: String,
        instruction: String,
        stream: Boolean,
        isOpenRouter: Boolean,
        rng: Random,
    ): String = buildJsonObject {
        put("model", model)
        put(
            "messages",
            kotlinx.serialization.json.buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", instruction)
                    }
                )
            },
        )
        // Jittered sampling + random seed: same prompt never decodes the same way twice.
        put("temperature", 0.85 + rng.nextDouble() * 0.35)
        put("top_p", 0.9 + rng.nextDouble() * 0.1)
        put("presence_penalty", 0.3 + rng.nextDouble() * 0.5)
        put("frequency_penalty", 0.2 + rng.nextDouble() * 0.3)
        put("seed", (rng.nextDouble() * 2147483647.0).toInt())
        put("max_tokens", 600)
        put("stream", stream)
        // Keep reasoning short so models don't burn the reply budget thinking.
        if (isOpenRouter) put("reasoning", buildJsonObject { put("effort", "low") })
    }.toString()

    private fun request(
        url: String,
        key: String,
        isOpenRouter: Boolean,
        body: String,
    ): Request {
        val b = Request.Builder().url(url)
        if (key.isNotEmpty()) b.header("Authorization", "Bearer $key")
        if (isOpenRouter) Http.addOpenRouterAttribution(b)
        return b.post(body.toRequestBody(JSON_MEDIA)).build()
    }

    /**
     * Streams a chat completion; [onText] receives the accumulated full text on every chunk
     * (raw, untrimmed — callers trim like the web version did). Returns the final full text.
     * Cancellation closes the response (equivalent of the web AbortController/stop button).
     */
    suspend fun streamChatCompletion(
        base: String,
        key: String,
        model: String,
        instruction: String,
        isOpenRouter: Boolean,
        rng: Random = Random.Default,
        onText: (String) -> Unit,
    ): String {
        val url = "$base/chat/completions"
        val streamReq = request(url, key, isOpenRouter, payload(model, instruction, stream = true, isOpenRouter, rng))
        val response = Http.executeAwait(client, streamReq)
        try {
            if (!response.isSuccessful) {
                val detail = SseParser.errorDetail(response.body?.string())
                val code = response.code
                when (code) {
                    401 -> throw Td2mException("invalid API key (401)")
                    402 -> throw Td2mException("payment required (402) — out of credits on your provider account")
                    404 -> throw Td2mException("model not found (404) — \"$model\" doesn't exist on this server")
                    429 -> throw Td2mException("rate limited (429) — wait a bit and try again")
                }
                if (code == 400 && Regex("stream", RegexOption.IGNORE_CASE).containsMatchIn(detail)) {
                    // Some servers don't support streaming — retry non-streaming once.
                    val resp2 = Http.executeAwait(client, request(url, key, isOpenRouter, payload(model, instruction, stream = false, isOpenRouter, rng)))
                    try {
                        if (!resp2.isSuccessful) throw Td2mException("HTTP ${resp2.code} — ${detail.ifEmpty { "request failed" }}")
                        val text = SseParser.contentOf(resp2.body?.string() ?: "")
                        if (text.isBlank()) throw Td2mException("empty response from model")
                        onText(text)
                        return text
                    } finally {
                        resp2.close()
                    }
                }
                throw Td2mException("HTTP $code — ${detail.ifEmpty { "request failed" }}")
            }
            val full = readSse(response, onText)
            if (full.isBlank()) throw Td2mException("empty response from model")
            return full
        } finally {
            response.close()
        }
    }

    /** Reads the SSE body line-by-line, accumulating delta content. Port of the web loop. */
    private suspend fun readSse(response: Response, onText: (String) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            // Closing the response unblocks a blocking read — this is how "stop" works mid-stream.
            cont.invokeOnCancellation { runCatching { response.close() } }
            val body = response.body ?: return@suspendCancellableCoroutine cont.resumeWithException(Td2mException("this client returned no stream body"))
            val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            val full = StringBuilder()
            thread(name = "td2m-sse") {
                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        val data = SseParser.dataLine(line) ?: continue
                        if (data == "[DONE]") break
                        val piece = SseParser.pieceOf(data)
                        if (piece.isNotEmpty()) {
                            full.append(piece)
                            onText(full.toString())
                        }
                    }
                    if (cont.isActive) cont.resume(full.toString())
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWithException(Td2mException(e.message ?: "stream error"))
                }
            }
        }
}