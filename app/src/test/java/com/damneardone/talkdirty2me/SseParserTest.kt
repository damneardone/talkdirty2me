package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.net.OpenAiCompatClient
import com.damneardone.talkdirty2me.net.SseParser
import com.damneardone.talkdirty2me.net.Td2mException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    private val client = OpenAiCompatClient()

    @Test
    fun dataLineParsing() {
        assertEquals("hello", SseParser.dataLine("data: hello"))
        assertEquals("hello", SseParser.dataLine("data:hello"))
        assertEquals("[DONE]", SseParser.dataLine("data: [DONE]"))
        assertNull(SseParser.dataLine(": keep-alive"))
        assertNull(SseParser.dataLine("event: message"))
        assertNull(SseParser.dataLine(""))
    }

    @Test
    fun pieceAndContentExtraction() {
        assertEquals("Hi", SseParser.pieceOf("""{"choices":[{"delta":{"content":"Hi"}}]}"""))
        assertEquals("", SseParser.pieceOf("""{"choices":[{"delta":{}}]}"""))
        assertEquals("", SseParser.pieceOf("""{"broken"""))
        assertEquals("Full reply", SseParser.contentOf("""{"choices":[{"message":{"content":"Full reply"}}]}"""))
        assertEquals("""{"broken""", SseParser.errorDetail("""{"broken"""))
        assertEquals("boom", SseParser.errorDetail("""{"error":{"message":"boom"}}"""))
    }

    @Test
    fun streamsAndAssemblesInOrder() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setBody(
                    ": ping\n\n" +
                        "data: {\"choices\":[{\"delta\":{\"content\":\"Hello \"}}]}\n\n" +
                        "data: {\"choices\":[{\"delta\":{\"content\":\"there\"}}]}\n\n" +
                        "data: {\"choices\":[{\"delta\":{}}]}\n\n" +
                        "data: [DONE]\n\n",
                )
                .addHeader("Content-Type", "text/event-stream"),
        )
        server.start()
        try {
            val seen = mutableListOf<String>()
            val full = runBlocking {
                client.streamChatCompletion(
                    base = server.url("/v1").toString().trimEnd('/'),
                    key = "sk-test",
                    model = "m",
                    instruction = "ignored",
                    isOpenRouter = false,
                ) { seen.add(it) }
            }
            assertEquals("Hello there", full)
            assertEquals(listOf("Hello ", "Hello there"), seen)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun errorMappingFriendlyStrings() {
        val cases = listOf(
            401 to "invalid API key (401)",
            402 to "payment required (402) — out of credits on your provider account",
            404 to "model not found (404) — \"m\" doesn't exist on this server",
            429 to "rate limited (429) — wait a bit and try again",
        )
        for ((code, expected) in cases) {
            val server = MockWebServer()
            server.enqueue(MockResponse().setResponseCode(code).setBody("nope"))
            server.start()
            try {
                val err = runBlocking {
                    try {
                        client.streamChatCompletion(
                            base = server.url("/v1").toString().trimEnd('/'),
                            key = "sk-test",
                            model = "m",
                            instruction = "ignored",
                            isOpenRouter = false,
                        ) { }
                        error("expected Td2mException for $code")
                    } catch (e: Td2mException) {
                        e
                    }
                }
                assertEquals(expected, err.message)
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun streamUnsupported400FallsBackToNonStreaming() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"error":{"message":"streaming is not supported here"}}"""),
        )
        server.enqueue(
            MockResponse().setBody("""{"choices":[{"message":{"content":"Non-stream reply"}}]}"""),
        )
        server.start()
        try {
            val seen = mutableListOf<String>()
            val full = runBlocking {
                client.streamChatCompletion(
                    base = server.url("/v1").toString().trimEnd('/'),
                    key = "sk-test",
                    model = "m",
                    instruction = "ignored",
                    isOpenRouter = false,
                ) { seen.add(it) }
            }
            assertEquals("Non-stream reply", full)
            assertEquals(listOf("Non-stream reply"), seen)
            // Fallback request must not ask for a stream.
            server.takeRequest() // first request: the streaming attempt (rejected 400)
            val retry = server.takeRequest() // the non-streaming fallback
            val bodyText = retry.body.peek().readUtf8()
            assertTrue("second request should be non-streaming: $bodyText", bodyText.contains("\"stream\":false"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun emptyStreamThrowsEmptyResponse() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody("data: [DONE]\n\n").addHeader("Content-Type", "text/event-stream"),
        )
        server.start()
        try {
            runBlocking {
                try {
                    client.streamChatCompletion(
                        base = server.url("/v1").toString().trimEnd('/'),
                        key = "sk-test",
                        model = "m",
                        instruction = "ignored",
                        isOpenRouter = false,
                    ) { }
                    error("expected Td2mException")
                } catch (e: Td2mException) {
                    assertEquals("empty response from model", e.message)
                }
            }
        } finally {
            server.shutdown()
        }
    }
}