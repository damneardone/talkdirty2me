package com.damneardone.talkdirty2me

import com.damneardone.talkdirty2me.net.SpeechClient
import com.damneardone.talkdirty2me.net.Td2mException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-parser tests for the /audio/speech/models catalog (shape from the live endpoint). */
class TtsModelsParseTest {

    private val fixture = """
        {"data":[
          {"id":"bytedance-seed/seed-tts-3-0","name":"ByteDance Seed: Seed-TTS 3.0",
           "output_modalities":[{"type":"speech","supported_parameters":{
             "voice":{"type":"enum","values":["multi_female_skye_v3_uranus_bigtts","multi_male_valentino_v3_uranus_bigtts"]},
             "response_format":{"type":"enum","values":["mp3","pcm","wav"]},
             "speed":{"type":"range","min":0.5,"max":2},
             "instructions":{"type":"unknown"}}}]},
          {"id":"elevenlabs/eleven-v3","name":"ElevenLabs: Eleven v3",
           "output_modalities":[{"type":"text"},{"type":"speech","supported_parameters":{
             "voice":{"type":"enum","values":["sarah","george"]},
             "response_format":{"type":"enum","values":["mp3","pcm"]},
             "speed":{"type":"range","min":0.7,"max":1.2}}}]},
          {"id":"fish-audio/drama-3","name":"Fish Audio: Drama 3",
           "output_modalities":[{"type":"speech","supported_parameters":{
             "voice":{"type":"unknown"},
             "response_format":{"type":"enum","values":["mp3","pcm"]},
             "speed":{"type":"range","min":0.5,"max":2},
             "instructions":{"type":"unknown"}}}]},
          {"id":"no-params/model","output_modalities":["text"]},
          {"id":null,"name":"broken"}
        ]}
    """.trimIndent()

    @Test
    fun parsesVoicesFormatsSpeedInstructions() {
        val models = SpeechClient.parseModels(fixture)
        assertEquals(4, models.size)

        val seed = models[0]
        assertEquals("bytedance-seed/seed-tts-3-0", seed.id)
        assertEquals(listOf("multi_female_skye_v3_uranus_bigtts", "multi_male_valentino_v3_uranus_bigtts"), seed.voices)
        assertEquals(listOf("mp3", "pcm", "wav"), seed.formats)
        assertEquals(0.5, seed.speedMin!!, 1e-9)
        assertEquals(2.0, seed.speedMax!!, 1e-9)
        assertTrue(seed.supportsInstructions)

        val eleven = models[1]
        assertEquals(listOf("sarah", "george"), eleven.voices)
        assertEquals(0.7, eleven.speedMin!!, 1e-9)
        assertFalse(eleven.supportsInstructions)

        val fish = models[2]
        assertTrue(fish.voices.isEmpty()) // voice enum "unknown" -> no published list
        assertTrue(fish.supportsInstructions)

        assertEquals("no-params/model", models[3].id)
        assertTrue(models[3].voices.isEmpty())
        assertTrue(models[3].formats.isEmpty())
    }

    @Test
    fun bareArrayTopLevelAlsoParses() {
        val models = SpeechClient.parseModels("""[{"id":"a/b","name":"A B","output_modalities":[]}]""")
        assertEquals(1, models.size)
        assertEquals("a/b", models[0].id)
        assertEquals("A B", models[0].name)
    }

    @Test
    fun garbageThrowsFriendly() {
        try {
            SpeechClient.parseModels("""{"unexpected":true}""")
            error("expected Td2mException")
        } catch (e: Td2mException) {
            assertTrue(e.message!!, e.message!!.contains("voice-model"))
        }
    }
}