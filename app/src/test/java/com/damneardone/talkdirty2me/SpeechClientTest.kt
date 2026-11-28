package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.net.SpeechClient
import com.damneardone.talkdirty2me.net.Td2mException
import com.damneardone.talkdirty2me.tts.TtsFormat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MockWebServer tests for the /audio/speech + /audio/speech/models calls. */
class SpeechClientTest {

    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createSpeechSendsOpenAiShapeAndReturnsBytes() = runBlocking {
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "audio/mpeg")
                .setBody("fake-mp3-bytes"),
        )
        server.start()
        val audio = SpeechClient().createSpeech(
            base = server.url("/v1").toString(),
            key = "sk-test",
            model = "bytedance-seed/seed-tts-3-0",
            input = "hello there",
            voice = "multi_female_skye_v3_uranus_bigtts",
            responseFormat = "mp3",
            speed = 1.2,
            instructions = "whisper it",
        )
        assertEquals("fake-mp3-bytes", String(audio.bytes))
        assertEquals(TtsFormat.MP3, audio.format)

        val req = server.takeRequest()
        assertEquals("/v1/audio/speech", req.path)
        assertEquals("Bearer sk-test", req.getHeader("Authorization"))
        assertEquals("https://github.com/damneardone/talkdirty2me", req.getHeader("HTTP-Referer"))
        assertEquals("TalkDirty2Me", req.getHeader("X-Title"))
        val body = Json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals("bytedance-seed/seed-tts-3-0", body["model"]!!.jsonPrimitive.content)
        assertEquals("hello there", body["input"]!!.jsonPrimitive.content)
        assertEquals("multi_female_skye_v3_uranus_bigtts", body["voice"]!!.jsonPrimitive.content)
        assertEquals("mp3", body["response_format"]!!.jsonPrimitive.content)
        assertEquals(1.2, body["speed"]!!.jsonPrimitive.content.toDouble(), 1e-9)
        assertEquals("whisper it", body["instructions"]!!.jsonPrimitive.content)
    }

    @Test
    fun optionalFieldsOmittedWhenNull() = runBlocking {
        server.enqueue(MockResponse().addHeader("Content-Type", "audio/wav").setBody("wav-bytes"))
        server.start()
        val audio = SpeechClient().createSpeech(
            base = server.url("/v1").toString(),
            key = "k",
            model = "m",
            input = "i",
            voice = "",
            responseFormat = "wav",
            speed = null,
            instructions = null,
        )
        assertEquals(TtsFormat.WAV, audio.format)
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertFalse(body.containsKey("speed"))
        assertFalse(body.containsKey("voice"))
        assertFalse(body.containsKey("instructions"))
    }

    @Test
    fun pcmContentTypeMapsToRawPcm() = runBlocking {
        server.enqueue(MockResponse().addHeader("Content-Type", "audio/pcm").setBody("pcm-bytes"))
        server.start()
        val audio = SpeechClient().createSpeech(
            server.url("/v1").toString(), "k", "m", "i", "v", "pcm", null, null,
        )
        assertEquals(TtsFormat.PCM_16LE, audio.format)
    }

    @Test
    fun creditsErrorIsFriendly() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(402)
                .setBody("""{"error":{"message":"insufficient credits"}}"""),
        )
        server.start()
        try {
            SpeechClient().createSpeech(
                server.url("/v1").toString(), "k", "m", "i", "v", "mp3", null, null,
            )
            error("expected Td2mException")
        } catch (e: Td2mException) {
            assertTrue(e.message!!, e.message!!.contains("credits"))
        }
    }

    @Test
    fun fetchModelsHitsCatalogEndpoint() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"data":[{"id":"a/b","name":"A B","output_modalities":[]}]}"""),
        )
        server.start()
        val models = SpeechClient().fetchModels(server.url("/v1").toString(), "sk-test")
        assertEquals(1, models.size)
        assertEquals("A B", models[0].name)
        assertEquals("/v1/audio/speech/models", server.takeRequest().path)
    }
}